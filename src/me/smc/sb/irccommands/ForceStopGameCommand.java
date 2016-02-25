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
	public void onCommand(MessageEvent<PircBotX> e, PrivateMessageEvent<PircBotX> pe, String discord, String[] args){
		if(!Utils.checkArguments(e, pe, discord, args, 1)) return;
		if(Utils.stringToInt(args[0]) == -1){Utils.info(e, pe, discord, "Invalid mp #!"); return;}
		
		String user = Utils.toUser(e, pe);
		
		for(Tournament tournament : Tournament.tournaments)
			for(Match match : new ArrayList<Match>(tournament.getMatches()))
				if(match.getGame() != null && match.getGame().getMpNum() == Utils.stringToInt(args[0])){
					if(match.isMatchAdmin(user == null ? discord : user)){
						match.getGame().stop();
						
						Utils.info(e, pe, discord, "Game #" + Utils.stringToInt(args[0]) + " was force stopped!");
						return;	
					}
				}
		
		Utils.info(e, pe, discord, "An error occured while force stopping the requested game!");
	}
	
}
