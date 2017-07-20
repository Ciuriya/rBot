package me.smc.sb.irccommands;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.PrivateMessageEvent;

import me.smc.sb.perm.Permissions;
import me.smc.sb.tourney.Map;
import me.smc.sb.tourney.MapPool;
import me.smc.sb.tourney.Tournament;
import me.smc.sb.utils.Utils;

public class ListMapsInPoolCommand extends IRCCommand{
	
	public ListMapsInPoolCommand(){
		super("Lists all maps in the map pool.",
			  "<tournament name> <map pool num>",
			  Permissions.TOURNEY_ADMIN,
			  "poolmaplist");
	}
	
	@Override
	public String onCommand(MessageEvent e, PrivateMessageEvent pe, String discord, String[] args){
		String argCheck = Utils.checkArguments(args, 2);
		if(argCheck.length() > 0) return argCheck;
		
		String tournamentName = "";
		
		for(int i = 0; i < args.length - 1; i++) tournamentName += args[i] + " ";
		Tournament t = Tournament.getTournament(tournamentName.substring(0, tournamentName.length() - 1));
		
		if(t == null) return "Invalid tournament!";
		if(Utils.stringToInt(args[args.length - 1]) == -1) return "Map pool number needs to be a number!";
		
		MapPool pool = MapPool.getPool(t, Utils.stringToInt(args[args.length - 1]));
		
		if(pool == null) return "Map pool does not exist!";
		
		String user = Utils.toUser(e, pe);
		
		if(t.isAdmin(user)){
			String msg = "Maps in map pool #" + pool.getPoolNum() + " of " + t.get("name");
			msg = addNewLine(discord, msg, 2);
			
			java.util.Map<Integer, List<Map>> maps = new HashMap<>();
			
			if(!pool.getMaps().isEmpty())
				for(Map map : pool.getMaps()){
					List<Map> list = new ArrayList<>();
					
					if(maps.containsKey(map.getCategory()))
						list = maps.get(map.getCategory());
					
					list.add(map);
					maps.put(map.getCategory(), list);
				}
				
			for(int i = 0; i < 6; i++){
				List<Map> list = maps.get(i);
				msg += getCategoryString(i);
				msg = addNewLine(discord, msg, 2);
				
				if(list != null && !list.isEmpty())
					for(Map map : list){
						msg += map.getURL();
						msg = addNewLine(discord, msg, 1);
					}
				
				if(i != 5) msg = addNewLine(discord, msg, 1);
				else msg = msg.substring(0, msg.lastIndexOf("\n"));
			}
			
			if(discord == null){
				String built = "";
				for(String part : msg.split("=")){
					if(part.isEmpty()) continue;
					if(e == null && pe == null) built += part.substring(0, part.length() - 1) + "\n";
					else Utils.info(e, pe, discord, part.substring(0, part.length() - 1));
				}
				
				if(built.length() > 0) return built.substring(0, built.length() - 1);
			}else return msg;
		}
		
		return "";
	}
	
	private String addNewLine(String discord, String message, int amount){
		String msg = message;
		
		for(int i = 0; i < amount; i++)
			if(discord != null) msg += "\n";
		if(discord == null) msg += " =";
		
		return msg;
	}
	
	private String getCategoryString(int category){
		switch(category){
			case 0: return "No Mod";
			case 1: return "Free Mod";
			case 2: return "Hidden";
			case 3: return "Hard Rock";
			case 4: return "Double Time";
			case 5: return "Tiebreaker";
			default: return "Unknown";
		}
	}
	
}
