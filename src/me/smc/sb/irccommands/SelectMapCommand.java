package me.smc.sb.irccommands;

import java.util.ArrayList;
import java.util.List;

import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.PrivateMessageEvent;

import me.smc.sb.pickstrategies.ModPickStrategy;
import me.smc.sb.tourney.PlayingTeam;
import me.smc.sb.utils.Utils;

public class SelectMapCommand extends IRCCommand{

	public static List<PlayingTeam> pickingTeams;
	
	public SelectMapCommand(){
		super("Selects a map for a tournament game.",
			  "<map url OR map #> ",
			  null,
			  "select", "pick");
		pickingTeams = new ArrayList<>();
	}

	@Override
	public String onCommand(MessageEvent e, PrivateMessageEvent pe, String discord, String[] args){
		String argCheck = Utils.checkArguments(args, 1);
		if(argCheck.length() > 0) return argCheck;
		
		if(e == null || discord != null || pe != null) return "You cannot select a map in here!";
		
		String userName = e.getUser().getNick();
		
		if(!pickingTeams.isEmpty())
			for(PlayingTeam team : pickingTeams)
				if(team.getTeam().has(userName)){
					String url = Utils.takeOffExtrasInBeatmapURL(args[0]);
					
					if(!(team.getGame().getSelectionManager().getPickStrategy() instanceof ModPickStrategy)){
						if(Utils.stringToInt(args[0]) == -1){
							if(!url.matches("^https?:\\/\\/osu.ppy.sh\\/b\\/[0-9]{1,8}")){
								Utils.info(e, pe, discord, "Invalid URL, example format: https://osu.ppy.sh/b/123456");
								Utils.info(e, pe, discord, "Your URL likely uses a /s/ just click on the difficulty name and grab that link.");
								return "";
							}
						}
					}
					
					String mod = "";
					
					if(args.length > 1 && team.getGame().getSelectionManager().getWarmupsLeft() > 0){
						String arg = args[1].replaceAll("\\+", "");
						
						if(arg.equalsIgnoreCase("DT") || arg.equalsIgnoreCase("HT"))
							mod = arg;
					}
					
					team.getGame().getSelectionManager().handleMapSelect(url, true, mod);
					return "";
				}
		
		return "Could not select map!";
	}
	
}
