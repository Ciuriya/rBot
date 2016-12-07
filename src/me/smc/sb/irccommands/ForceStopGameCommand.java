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
		
		String tournamentName = "";
		Tournament t = null;
		
		if(args.length > 1){
			for(int i = 0; i < args.length - 2; i++)
				tournamentName += args[i] + " ";
			
			t = Tournament.getTournament(tournamentName.substring(0, tournamentName.length() - 1));	
		}
		
		if(t == null && Utils.stringToInt(args[0]) == -1) return "Invalid mp #!";
		if(t != null && Utils.stringToInt(args[args.length - 1]) == -1) return "Invalid match number!";
		
		String user = Utils.toUser(e, pe);
		
		if(t != null){
			int matchNumber = Utils.stringToInt(args[args.length - 1]);
			
			Match match = t.getMatch(matchNumber);
			
			if(match == null) return "Match does not exist!";
			
			if(match.isMatchAdmin(user)){
				if(match.getGame() == null) return "Game is already stopped!";
				
				match.getGame().stop();
				return "Game #" + Utils.stringToInt(args[0]) + " was force stopped!";
			}
			
			return "";
		}
		
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
