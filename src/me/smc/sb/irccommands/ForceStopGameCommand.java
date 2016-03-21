package me.smc.sb.irccommands;

import java.util.ArrayList;

import org.pircbotx.PircBotX;
import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.PrivateMessageEvent;

import me.smc.sb.multi.Match;
import me.smc.sb.multi.Tournament;
import me.smc.sb.utils.Utils;

public class ForceStopGameCommand extends IRCCommand{

	public ForceStopGameCommand(){
		super("Force stops a running game.",
			  "<mp #> ",
			  null,
			  "fstop");
	}

	@Override
	public String onCommand(MessageEvent<PircBotX> e, PrivateMessageEvent<PircBotX> pe, String discord, String[] args){
		String argCheck = Utils.checkArguments(args, 1);
		if(argCheck.length() > 0) return argCheck;
		
		if(Utils.stringToInt(args[0]) == -1) return "Invalid mp #!";
		
		String user = Utils.toUser(e, pe);
		
		for(Tournament tournament : Tournament.tournaments)
			for(Match match : new ArrayList<Match>(tournament.getMatches()))
				if(match.getGame() != null && match.getGame().getMpNum() == Utils.stringToInt(args[0])){
					if(match.isMatchAdmin(user)){
						match.getGame().stop();
						
						return "Game #" + Utils.stringToInt(args[0]) + " was force stopped!";
					}
				}
		
		return "An error occured while force stopping the requested game!";
	}
	
}
