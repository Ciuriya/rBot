package me.smc.sb.irccommands;

import org.pircbotx.PircBotX;
import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.PrivateMessageEvent;

import me.smc.sb.multi.Map;
import me.smc.sb.multi.Match;
import me.smc.sb.multi.Tournament;
import me.smc.sb.perm.Permissions;
import me.smc.sb.utils.Utils;

public class ForceSelectCommand extends IRCCommand {

	public ForceSelectCommand(){
		super("Force selects a map in the tournament match.",
			  "<tournament name> <match number> <revert last point?> (map link or #, if null, current)",
			  Permissions.TOURNEY_ADMIN,
			  "fselect", "fsel");
	}

	@Override
	public String onCommand(MessageEvent<PircBotX> e, PrivateMessageEvent<PircBotX> pe, String discord, String[] args){
		String argCheck = Utils.checkArguments(args, 3);
		if(argCheck.length() > 0) return argCheck;
		
		String mapLink = "";
		int mapID = 0;
		int mapArgMod = 0;
		
		if(args[args.length - 1].startsWith("http")){
			mapLink = args[args.length - 1];
			mapArgMod = 1;
		}else if(Utils.stringToInt(args[args.length - 1]) != -1){
			mapID = Utils.stringToInt(args[args.length - 1]);
			mapArgMod = 1;
		}
		
		String tournamentName = "";
		
		for(int i = 0; i < args.length - 2 - mapArgMod; i++)
			tournamentName += args[i] + " ";
		
		Tournament t = Tournament.getTournament(tournamentName.substring(0, tournamentName.length() - 1));
		
		if(t == null) return "Invalid tournament!";
		if(Utils.stringToInt(args[args.length - 2 - mapArgMod]) == -1) 
			return "Match number needs to be a number!";
		
		Match match = t.getMatch(Utils.stringToInt(args[args.length - 2 - mapArgMod]));
		
		boolean revertScore = false;
		
		switch(args[args.length - 1 - mapArgMod].toLowerCase()){
			case "true":
			case "yes":
			case "y": revertScore = true; break;
			case "false":
			case "no":
			case "n": revertScore = false; break;
			default: return "Revert last point argument must be a boolean (true or false)";
		}
		
		if(mapLink.length() > 0 && !mapLink.matches("^https?:\\/\\/osu.ppy.sh\\/b\\/[0-9]{1,8}"))
			return "Invalid URL, example format: https://osu.ppy.sh/b/123456";
		
		Map newMap = null;
		
		int mapNum = 1;
		for(Map poolMap : match.getMapPool().getMaps()){
			if((mapLink.length() > 0 && poolMap.getURL().equalsIgnoreCase(mapLink)) ||
			   (mapID != 0 && mapNum == mapID)){
				newMap = poolMap;
				break;
			}
			
			mapNum++;
		}
		
		if(newMap == null && mapLink.length() > 0) newMap = new Map(mapLink, 1, null);
		
		String user = Utils.toUser(e, pe);
		
		if(match.getGame() == null) return "The game is not started!";
		
		if(match.isMatchAdmin(user)){
			boolean success = match.getGame().forceSelect(revertScore, newMap);
			return success ? "Force select successful!" : "";
		}
		
		return "";
	}
	
}