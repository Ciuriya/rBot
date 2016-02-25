package me.smc.sb.irccommands;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.pircbotx.PircBotX;
import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.PrivateMessageEvent;

import me.smc.sb.multi.Map;
import me.smc.sb.multi.MapPool;
import me.smc.sb.multi.Tournament;
import me.smc.sb.perm.Permissions;
import me.smc.sb.utils.Utils;

public class ListMapsInPoolCommand extends IRCCommand{
	
	public ListMapsInPoolCommand(){
		super("Lists all maps in the map pool.",
			  "<tournament name> <map pool num>",
			  Permissions.IRC_BOT_ADMIN,
			  "poolmaplist");
	}
	
	@Override
	public void onCommand(MessageEvent<PircBotX> e, PrivateMessageEvent<PircBotX> pe, String discord, String[] args){
		if(!Utils.checkArguments(e, pe, discord, args, 2)) return;
		
		String tournamentName = "";
		
		for(int i = 0; i < args.length - 1; i++) tournamentName += args[i] + " ";
		Tournament t = Tournament.getTournament(tournamentName.substring(0, tournamentName.length() - 1));
		
		if(t == null){Utils.info(e, pe, discord, "Invalid tournament!"); return;}
		if(Utils.stringToInt(args[args.length - 1]) == -1){Utils.info(e, pe, discord, "Map pool number needs to be a number!"); return;}
		
		MapPool pool = t.getPool(Utils.stringToInt(args[args.length - 1]));
		
		if(pool == null){Utils.info(e, pe, discord, "Map pool does not exist!"); return;}
		
		String msg = "Maps in map pool #" + pool.getPoolNum() + " of " + t.getName();
		msg = addNewLine(discord, msg, 2);
		
		java.util.Map<Integer, List<Map>> maps = new HashMap<>();
		
		if(!pool.getMaps().isEmpty())
			for(Map map : pool.getMaps()){
				List<Map> list = new ArrayList<>();
				
				System.out.println("Map: " + map.getBeatmapID());
				
				if(maps.containsKey(map.getCategory()))
					list = maps.get(map.getCategory());
				
				list.add(map);
				maps.put(map.getCategory(), list);
			}
			
		for(int i = 0; i < 6; i++){
			List<Map> list = maps.get(i);
			msg += getCategoryString(i);
			msg = addNewLine(discord, msg, 2);
			
			System.out.println("size " + (list == null ? "null" : list.size()));
			
			if(list != null && !list.isEmpty())
				for(Map map : list){
					msg += map.getURL();
					msg = addNewLine(discord, msg, 1);
				}
			
			if(i != 5) msg = addNewLine(discord, msg, 1);
			else msg = msg.substring(0, msg.lastIndexOf("\n"));
		}
		
		if(discord == null)
			for(String part : msg.split("=")){
				if(part.isEmpty()) continue;
				Utils.info(e, pe, discord, part.substring(0, part.length() - 1));
			}
		else Utils.info(e, pe, discord, msg);
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
			case 3: return "Hardrock";
			case 4: return "Doubletime";
			case 5: return "Tiebreaker";
			default: return "Unknown";
		}
	}
	
}
