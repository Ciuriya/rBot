package me.smc.sb.pickstrategies;

import java.util.List;
import java.util.stream.Collectors;

import org.json.JSONObject;

import me.smc.sb.tourney.Game;
import me.smc.sb.tourney.Map;
import me.smc.sb.tourney.SelectionManager;
import me.smc.sb.utils.Utils;

public class ModPickStrategy implements PickStrategy{

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
												   Utils.toDuration(game.match.getTournament().getInt("warmupLength")) + ".", false);
				
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
		
		if(manager.getMap() != null) return;
		
		int category = 0;
		
		switch(map.toLowerCase()){
			case "nm": case "nomod": category = 0; break;
			case "fm": case "freemod": category = 1; break;
			case "hd": case "hidden": category = 2; break;
			case "hr": case "hardrock": category = 3; break;
			case "dt": case "doubletime": category = 4; break;
			default: 
				game.getBanchoHandle().sendMessage("This isn't a valid category! Valid ones are NM, FM, HD, HR and DT!", false); 
				return;
		}
		
		final int fCategory = category;
		boolean valid = false;
		List<Map> maps = game.match.getMapPool().getMaps().stream().filter(m -> m.getCategory() == fCategory).collect(Collectors.toList());
		
		while(!valid){
			if(maps.size() == 0){
				game.getBanchoHandle().sendMessage("You have already picked all maps in this mod!", false);
				return;
			}
			
			int num = Utils.fetchRandom(0, maps.size() - 1);
			selected = maps.get(num);
			
			if(!manager.wasPicked(selected)) valid = true;
			else maps.remove(selected);
		}
		
		if(selected != null && manager.getMap() == null && select){
			manager.setMap(selected);
			Utils.updateTwitch(game, selected);
			game.getReadyManager().startReadyWait();
			
			return;
		}
		
		if(selected == null) game.getBanchoHandle().sendMessage("Invalid selection!", false);
	}
}
