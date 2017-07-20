package me.smc.sb.irccommands;

import java.util.ArrayList;
import java.util.List;

import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.PrivateMessageEvent;

import me.smc.sb.multi.ModPickStrategy;
import me.smc.sb.multi.Player;
import me.smc.sb.multi.Team;
import me.smc.sb.tourney.PlayingTeam;
import me.smc.sb.utils.Utils;

public class BanMapCommand extends IRCCommand{

	public static List<PlayingTeam> banningTeams;
	
	public BanMapCommand(){
		super("Bans a map from the tournament game.",
			  "<map url OR map #> ",
			  null,
			  "ban");
		banningTeams = new ArrayList<>();
	}

	@Override
	public String onCommand(MessageEvent e, PrivateMessageEvent pe, String discord, String[] args){		
		String argCheck = Utils.checkArguments(args, 1);
		if(argCheck.length() > 0) return argCheck;
		
		if(e == null || discord != null || pe != null) return "You cannot ban a map in here!";
		
		String userName = e.getUser().getNick();
		
		if(!banningTeams.isEmpty())
			for(PlayingTeam team : banningTeams)
				if(team.getGame().getLobbyManager().verify(userName) &&
				   team.getTeam().has(userName)){
					String url = Utils.takeOffExtrasInBeatmapURL(args[0]);
					
					if(team.getGame().getSelectionManager().getPickStrategy() instanceof ModPickStrategy)
						return "";
					
					if(Utils.stringToInt(args[0]) == -1){
						if(!url.matches("^https?:\\/\\/osu.ppy.sh\\/b\\/[0-9]{1,8}")){
							Utils.info(e, pe, discord, "Invalid URL, example format: https://osu.ppy.sh/b/123456");
							Utils.info(e, pe, discord, "Your URL likely uses a /s/ just click on the difficulty name and grab that link.");
							return "";
						}
					}
					
					team.getGame().getSelectionManager().handleMapSelect(url, false, "");
					return "";
				}
		
		return "Could not ban map!";
	}
	
}
