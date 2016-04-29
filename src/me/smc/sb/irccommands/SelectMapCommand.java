package me.smc.sb.irccommands;

import java.util.ArrayList;
import java.util.List;

import org.pircbotx.PircBotX;
import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.PrivateMessageEvent;

import me.smc.sb.multi.Game;
import me.smc.sb.multi.Player;
import me.smc.sb.utils.Utils;

public class SelectMapCommand extends IRCCommand{

	public static List<Game> gamesInSelection;
	
	public SelectMapCommand(){
		super("Selects a map for a tournament game.",
			  "<map url OR map #> ",
			  null,
			  "select", "pick");
		gamesInSelection = new ArrayList<>();
	}

	@Override
	public String onCommand(MessageEvent<PircBotX> e, PrivateMessageEvent<PircBotX> pe, String discord, String[] args){
		String argCheck = Utils.checkArguments(args, 1);
		if(argCheck.length() > 0) return argCheck;
		
		if(e == null || discord != null || pe != null) return "You cannot select a map in here!";
		
		String userName = e.getUser().getNick();
		
		if(!gamesInSelection.isEmpty())
			for(Game game : gamesInSelection)
				for(Player pl : game.getSelectingTeam().getPlayers())
					if(pl.getName().replaceAll(" ", "_").equalsIgnoreCase(userName.replaceAll(" ", "_"))){
						String url = Utils.takeOffExtrasInBeatmapURL(args[0]);
						
						if(Utils.stringToInt(args[0]) == -1){
							if(!url.matches("^https?:\\/\\/osu.ppy.sh\\/b\\/[0-9]{1,8}")){
								Utils.info(e, pe, discord, "Invalid URL, example format: https://osu.ppy.sh/b/123456");
								Utils.info(e, pe, discord, "Your URL likely uses a /s/ just click on the difficulty name and grab that link.");
								return "";
							}
						}
						
						game.handleMapSelect(url, true);
						return "";
					}
		
		return "Could not select map!";
	}
	
}
