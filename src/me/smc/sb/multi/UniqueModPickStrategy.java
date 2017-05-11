package me.smc.sb.multi;

import java.util.logging.Level;

import org.json.JSONObject;

import me.smc.sb.irccommands.BanMapCommand;
import me.smc.sb.utils.Log;
import me.smc.sb.utils.RemotePatyServerUtils;
import me.smc.sb.utils.Utils;

public class UniqueModPickStrategy implements PickStrategy{

	@Override
	public void handleMapSelect(Game game, String map, boolean select, String mod){
		Map selected = null;
		
		if(game.warmupsLeft > 0 && select){
			JSONObject jsMap = Map.getMapInfo(new Map(map, 1, null).getBeatmapID(), game.match.getTournament().getMode(), true);
			if(jsMap == null){game.sendMessage("Could not find the selected map!"); return;}
			
			int length = jsMap.getInt("total_length");
			game.mapLength = length;
			
			double modMultiplier = 1;
			
			if(mod.length() > 0){
				if(mod.equalsIgnoreCase("DT") || mod.equalsIgnoreCase("NC"))
					modMultiplier = 1.5;
				else if(mod.equalsIgnoreCase("HT"))
					modMultiplier = 0.75;
			}
				
			if(length / modMultiplier > 270){
				game.sendMessage("The warmup selected is too long! The maximum length is 4m30s.");
				return;
			}
			
			game.map = new Map(map, 1, null);
			game.mapSelected = true;
			game.mapSelectedTime = System.currentTimeMillis();
			game.mapMod = mod;
			
			if(mod.length() > 0)
				game.sendMessage("!mp mods " + mod.toUpperCase() + " Freemod");
			
			game.updateTwitch("Warmup: " + jsMap.getString("artist") + " - " + 
				    	  	  jsMap.getString("title") + " [" + jsMap.getString("version") + "] " + 
				    	  	  (mod.length() > 0 ? "+" + mod + " " : "") + "was picked by " + 
				    	  	  game.selectingTeam.getTeamName() + "!");
			
			game.prepareReadyCheck();
			return;
		}
		
		int num = 1;
		for(Map m : game.match.getMapPool().getMaps()){
			boolean chosen = false;
			if(num == Utils.stringToInt(map)) chosen = true;
			else if(m.getURL().equalsIgnoreCase(map)) chosen = true;
			
			if(chosen){
				if(select && game.warmupsLeft == 0){
					if(game.bans.contains(m)){game.sendMessage("This map is banned! Please choose something else."); return;}
					if(!game.checkMap(m)){game.sendMessage("This map was already picked once! Please choose something else."); return;}
				}
				
				if(m.getCategory() == 5){game.sendMessage("You cannot " + (select ? "select" : "ban") + " the tiebreaker!"); return;}
				
				selected = m;
				break;
			}
			
			num++;
		}

		if(!select && selected != null && !game.bans.contains(selected)){
			BanMapCommand.banningTeams.remove(game.banningTeam);
			game.bans.add(selected);
			
			final Map fSelected = selected;
			
			final Team banningTeam = game.banningTeam;
			
			new Thread(new Runnable(){
				public void run(){
					JSONObject jsMap = Map.getMapInfo(fSelected.getBeatmapID(), game.match.getTournament().getMode(), true);
					
					String name = game.getMod(fSelected).replace("None", "Nomod") + " pick: " + jsMap.getString("artist") + " - " + 
						    	  jsMap.getString("title") + " [" + jsMap.getString("version") + "]";
					
					game.sendMessage(name + " was banned!");
					
					game.bansWithNames.add("{" + game.getMod(fSelected).replace("None", "Nomod") + "} " + jsMap.getString("artist") + " - " + 
					    	  		  jsMap.getString("title") + " [" + jsMap.getString("version") + "] (" + banningTeam.getTeamName() + ")");
					
					game.updateTwitch(name + " was banned by " + banningTeam.getTeamName() + "!");
					
					if(game.match.getTournament().isUsingMapStats()){
						int mapId = game.match.getMapPool().getMapId(fSelected);
						
						if(mapId != 0){
							int tourneyId = 0;
							
							try{
								tourneyId = RemotePatyServerUtils.fetchTournamentId(game.match.getTournament().getName());
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
			
			game.mapSelection(3);
			return;
		}
		
		if(selected != null && select){
			final Map fSelected = selected;
			
			if(game.mapsPicked.stream().anyMatch(p -> p.getTeam().getTeamName().equals(game.selectingTeam.getTeamName()) && 
													  p.getMap().getCategory() > 0 && p.getMap().getCategory() == fSelected.getCategory())) {
				game.sendMessage("You cannot select a map within a mod pool twice!");
				
				return;
			}
		}
		
		if(selected != null && !game.mapSelected && select){			
			game.mapSelected = true;
			game.map = selected;
			game.mapSelectedTime = System.currentTimeMillis();
			
			Utils.updateTwitch(game, selected);
			
			game.prepareReadyCheck();
			return;
		}else if(game.mapSelected && select && selected != null){
			game.map = selected;
			
			Utils.updateTwitch(game, selected);
			
			return;
		}
		
		if(selected == null || !select) game.sendMessage("Invalid " + (select ? "selection!" : "ban!"));
	}
}