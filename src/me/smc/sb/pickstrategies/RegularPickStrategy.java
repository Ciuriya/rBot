package me.smc.sb.pickstrategies;

import java.util.logging.Level;

import org.json.JSONObject;

import me.smc.sb.irccommands.BanMapCommand;
import me.smc.sb.tourney.Game;
import me.smc.sb.tourney.Map;
import me.smc.sb.tourney.PlayingTeam;
import me.smc.sb.tourney.SelectionManager;
import me.smc.sb.utils.Log;
import me.smc.sb.utils.RemotePatyServerUtils;
import me.smc.sb.utils.Utils;

public class RegularPickStrategy implements PickStrategy{

	@Override
	public void handleMapSelect(Game game, String map, boolean select, String mod){
		Map selected = null;
		SelectionManager manager = game.getSelectionManager();
		
		if(manager.getWarmupsLeft() > 0 && select){
			JSONObject jsMap = Map.getMapInfo(new Map(map, 1, null).getBeatmapID(), game.match.getTournament().getInt("mode"), true);
			
			if(jsMap == null){
				game.getBanchoHandle().sendMessage("Could not find the selected map!", false); 
				
				return;
			}
			
			int length = jsMap.getInt("total_length");
				
			if(manager.isWarmupTooLong(mod, length)){
				game.getBanchoHandle().sendMessage("The warmup selected is too long! The maximum length is " + 
												   Utils.toDuration(game.match.getTournament().getInt("warmupLength") * 1000) + ".", false);
				
				return;
			}
			
			manager.setMap(new Map(map, 1, null));
			
			if(mod.length() > 0)
				game.getBanchoHandle().sendMessage("!mp mods " + mod.toUpperCase() + " Freemod", false);
			
			game.getGameFeed().updateTwitch("Warmup: " + jsMap.getString("artist") + " - " + 
				    	  	  				jsMap.getString("title") + " [" + jsMap.getString("version") + "] " + 
				    	  	  				(mod.length() > 0 ? "+" + mod + " " : "") + "was picked by " + 
				    	  	  				game.getNextTeam().getTeam().getTeamName() + "!");
			
			game.getReadyManager().startReadyWait();
			
			return;
		}
		
		int num = 1;
		for(Map m : game.match.getMapPool().getMaps()){
			boolean chosen = false;
			
			if(num == Utils.stringToInt(map)) chosen = true;
			else if(m.getURL().equalsIgnoreCase(map)) chosen = true;
			
			if(chosen){
				if(select && manager.getWarmupsLeft() == 0){
					if(manager.getBans().contains(m)){
						game.getBanchoHandle().sendMessage("This map was removed! Please choose something else.", false); 
						
						return;
					}
					
					if(manager.wasPicked(m)){
						game.getBanchoHandle().sendMessage("This map was already picked once! Please choose something else.", false); 
						
						return;
					}
				}
				
				if(m.getCategory() == 5){
					game.getBanchoHandle().sendMessage("You cannot " + (select ? "select" : "remove") + " the tiebreaker!", false); 
					
					return;
				}
				
				selected = m;
				
				break;
			}
			
			num++;
		}
		
		if(!select && selected != null && !manager.getBans().contains(selected)){
			BanMapCommand.banningTeams.remove(game.getNextTeam());
			game.getNextTeam().addBan(selected);
			
			final Map fSelected = selected;
			final PlayingTeam banningTeam = game.getNextTeam();
			
			JSONObject jsMap = Map.getMapInfo(fSelected.getBeatmapID(), game.match.getTournament().getInt("mode"), true);
			String name = manager.getMod(fSelected).replace("None", "Nomod") + " pick: " + jsMap.getString("artist") + " - " + 
				    	  jsMap.getString("title") + " [" + jsMap.getString("version") + "]";
			game.getBanchoHandle().sendMessage(name + " was removed!", false);
			game.getGameFeed().addBan("{" + manager.getMod(fSelected).replace("None", "Nomod") + "} " + jsMap.getString("artist") + " - " + 
			    	  		  		  jsMap.getString("title") + " [" + jsMap.getString("version") + "] (" + banningTeam.getTeam().getTeamName() + ")");
			game.getGameFeed().updateTwitch(name + " was removed by " + banningTeam.getTeam().getTeamName() + "!");
			
			new Thread(new Runnable(){
				public void run(){
					if(game.match.getTournament().getBool("usingMapStats")){
						int mapId = game.match.getMapPool().getMapId(fSelected);
						
						if(mapId != 0){
							int tourneyId = 0;
							
							try{
								tourneyId = RemotePatyServerUtils.fetchTournamentId(game.match.getTournament().get("name"));
							}catch(Exception e){
								Log.logger.log(Level.SEVERE, "Could not fetch tourney id", e);
							}
							
							if(tourneyId != 0){
								RemotePatyServerUtils.incrementMapValue(mapId, game.match.getMapPool().getPoolNum(), tourneyId, "bancount", 1);
							}
						}
					}
				}
			}).start();
			
			manager.selectBans();
			
			return;
		}
		
		if(selected != null && manager.getMap() == null && select){
			manager.setMap(selected);
			Utils.updateTwitch(game, selected);
			game.getReadyManager().startReadyWait();
			
			return;
		}else if(manager.getMap() != null && select && selected != null){
			manager.setMap(selected);
			Utils.updateTwitch(game, selected);
			
			return;
		}
		
		if(selected == null || !select) 
			game.getBanchoHandle().sendMessage("Invalid " + (select ? "selection!" : "removal!"), false);
	}
}
