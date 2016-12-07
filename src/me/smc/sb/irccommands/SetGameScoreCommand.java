package me.smc.sb.irccommands;

import java.util.ArrayList;

import org.pircbotx.PircBotX;
import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.PrivateMessageEvent;

import me.smc.sb.multi.Match;
import me.smc.sb.multi.Tournament;
import me.smc.sb.utils.Utils;

public class SetGameScoreCommand extends IRCCommand{

	public SetGameScoreCommand(){
		super("Sets the scores of both teams in the game.",
			  "<mp number> <team 1 score> <team 2 score>",
			  null,
			  "setscores");
	}

	@Override
	public String onCommand(MessageEvent<PircBotX> e, PrivateMessageEvent<PircBotX> pe, String discord, String[] args){
		String argCheck = Utils.checkArguments(args, 3);
		if(argCheck.length() > 0) return argCheck;
		
		String tournamentName = "";
		Tournament t = null;
		
		if(args.length > 3){
			for(int i = 0; i < args.length - 4; i++)
				tournamentName += args[i] + " ";
			
			t = Tournament.getTournament(tournamentName.substring(0, tournamentName.length() - 1));	
		}
		
		if(t == null && Utils.stringToInt(args[0]) == -1) return "Invalid mp #!";
		if(t != null && Utils.stringToInt(args[args.length - 1]) == -1) return "Invalid match number!";
		
		String user = Utils.toUser(e, pe);
		
		if(t != null){
			int matchNumber = Utils.stringToInt(args[args.length - 3]);
			
			Match match = t.getMatch(matchNumber);
			
			if(match == null) return "Match does not exist!";
			
			if(match.isMatchAdmin(user)){
				if(match.getGame() == null) return "Game is stopped!";
				
				match.getGame().setScores(Utils.stringToInt(args[args.length - 2]), Utils.stringToInt(args[args.length - 1]));
				return "Scores switched: " + Utils.stringToInt(args[args.length - 2]) + " - " + Utils.stringToInt(args[args.length - 1]);
			}
			
			return "";
		}
		
		for(Tournament tournament : Tournament.tournaments)
			for(Match match : new ArrayList<Match>(tournament.getMatches()))
				if(match.getGame() != null && match.getGame().getMpNum() == Utils.stringToInt(args[0])){
					if(match.isMatchAdmin(user)){
						match.getGame().setScores(Utils.stringToInt(args[args.length - 2]), Utils.stringToInt(args[args.length - 1]));
						
						return "Scores switched: " + Utils.stringToInt(args[args.length - 2]) + " - " + Utils.stringToInt(args[args.length - 1]);
					}
				}
		
		return "Could not switch scores!";
	}
	
}
