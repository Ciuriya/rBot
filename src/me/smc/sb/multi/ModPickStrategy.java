package me.smc.sb.multi;

import java.util.List;
import java.util.stream.Collectors;

import org.json.JSONObject;

import me.smc.sb.utils.Utils;

public class ModPickStrategy implements PickStrategy{

	@Override
	public void handleMapSelect(Game game, String map, boolean select, String mod){
		Map selected = null;
		
		if(game.warmupsLeft > 0 && select){
			JSONObject jsMap = Map.getMapInfo(new Map(map, 1).getBeatmapID(), true);
			if(jsMap == null){game.sendMessage("Could not find the selected map!"); return;}
			
			int length = jsMap.getInt("total_length");
			double modMultiplier = 1;
			
			if(mod.length() > 0){
				if(mod.equalsIgnoreCase("DT"))
					modMultiplier = 1.5;
				else if(mod.equalsIgnoreCase("HT"))
					modMultiplier = 0.75;
			}
				
			if(length / modMultiplier > 270){
				game.sendMessage("The warmup selected is too long! The maximum length is 4m30s.");
				return;
			}
			
			game.map = new Map(map, 1);
			game.mapSelected = true;
			
			if(mod.length() > 0)
				game.sendMessage("!mp mods " + mod.toUpperCase() + " Freemod");
			
			game.updateTwitch("Warmup: " + jsMap.getString("artist") + " - " + 
				    	  	  jsMap.getString("title") + " [" + jsMap.getString("version") + "] " + 
				    	  	  (mod.length() > 0 ? "+" + mod + " " : "") + "was picked by " + 
				    	  	  game.selectingTeam.getTeamName() + "!");
			
			game.prepareReadyCheck();
			return;
		}
		
		if(game.mapSelected) return;
		
		int category = 0;
		
		switch(map.toLowerCase()){
			case "nm": case "nomod": category = 0; break;
			case "fm": case "freemod": category = 1; break;
			case "hd": case "hidden": category = 2; break;
			case "hr": case "hardrock": category = 3; break;
			case "dt": case "doubletime": category = 4; break;
			default: 
				game.sendMessage("This isn't a valid category! Valid ones are NM, FM, HD, HR and DT!"); 
				return;
		}
		
		final int fCategory = category;
		
		boolean valid = false;
		List<Map> maps = game.match.getMapPool().getMaps().stream().filter(m -> m.getCategory() == fCategory).collect(Collectors.toList());
		
		while(!valid){
			if(maps.size() == 0){
				game.sendMessage("You have already picked all maps in this mod!");
				return;
			}
			
			int num = Utils.fetchRandom(0, maps.size() - 1);
			selected = maps.get(num);
			
			if(!game.mapsPicked.contains(selected))
				valid = true;
			else maps.remove(selected);
		}
		
		if(selected != null && !game.mapSelected && select){
			game.mapSelected = true;
			game.map = selected;
			
			JSONObject jsMap = Map.getMapInfo(selected.getBeatmapID(), true);
			
			game.updateTwitch(game.getMod(selected).replace("None", "Nomod") + " pick: " + jsMap.getString("artist") + " - " + 
		    	  	 	 jsMap.getString("title") + " [" + jsMap.getString("version") + "] was picked by " + 
		    	  	 	game.selectingTeam.getTeamName() + "!");
			
			game.prepareReadyCheck();
			return;
		}else if(game.mapSelected && select && selected != null){
			game.map = selected;
			
			JSONObject jsMap = Map.getMapInfo(selected.getBeatmapID(), true);
			
			game.updateTwitch(game.getMod(selected).replace("None", "Nomod") + " pick: " + jsMap.getString("artist") + " - " + 
		    	  	 	 jsMap.getString("title") + " [" + jsMap.getString("version") + "] was picked by " + 
		    	  	 	game.selectingTeam.getTeamName() + "!");
			
			return;
		}
		
		if(selected == null) game.sendMessage("Invalid selection!");
	}

}
