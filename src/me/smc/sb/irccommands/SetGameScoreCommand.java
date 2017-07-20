package me.smc.sb.irccommands;

import java.util.ArrayList;

import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.PrivateMessageEvent;

import me.smc.sb.perm.Permissions;
import me.smc.sb.tourney.Match;
import me.smc.sb.tourney.Tournament;
import me.smc.sb.utils.Utils;

public class SetGameScoreCommand extends IRCCommand{

	public SetGameScoreCommand(){
		super("Sets the scores of both teams in the game.",
			  "<mp number> <team 1 score> <team 2 score>",
			  Permissions.TOURNEY_ADMIN,
			  "setscores");
	}

	@Override
	public String onCommand(MessageEvent e, PrivateMessageEvent pe, String discord, String[] args){
		String argCheck = Utils.checkArguments(args, 3);
		if(argCheck.length() > 0) return argCheck;
		
		String tournamentName = "";
		Tournament t = null;
		
		if(args.length > 3){
			for(int i = 0; i < args.length - 3; i++)
				tournamentName += args[i] + " ";
			
			t = Tournament.getTournament(tournamentName.substring(0, tournamentName.length() - 1));	
		}
		
		if(t == null && Utils.stringToInt(args[0]) == -1) return "Invalid mp #!";
		if(t != null && Utils.stringToInt(args[args.length - 1]) == -1) return "Invalid match number!";
		
		String user = Utils.toUser(e, pe);
		
		if(t != null){
			int matchNumber = Utils.stringToInt(args[args.length - 3]);
			
			Match match = Match.getMatch(t, matchNumber);
			
			if(match == null) return "Match does not exist!";
			
			if(match.isMatchAdmin(user)){
				if(match.getGame() == null) return "Game is stopped!";
				
				match.getGame().getFirstTeam().setPoints(Utils.stringToInt(args[args.length - 2]));
				match.getGame().getSecondTeam().setPoints(Utils.stringToInt(args[args.length - 1]));
				match.getGame().getResultManager().updateScores(true);
				
				return "Scores switched: " + Utils.stringToInt(args[args.length - 2]) + " - " + Utils.stringToInt(args[args.length - 1]);
			}
			
			return "";
		}
		
		for(Tournament tournament : Tournament.tournaments)
			for(Match match : new ArrayList<Match>(Match.getMatches(tournament)))
				if(match.getGame() != null && match.getGame().getMpNum() == Utils.stringToInt(args[0])){
					if(match.isMatchAdmin(user)){
						match.getGame().getFirstTeam().setPoints(Utils.stringToInt(args[args.length - 2]));
						match.getGame().getSecondTeam().setPoints(Utils.stringToInt(args[args.length - 1]));
						match.getGame().getResultManager().updateScores(true);
						
						return "Scores switched: " + Utils.stringToInt(args[args.length - 2]) + " - " + Utils.stringToInt(args[args.length - 1]);
					}
				}
		
		return "Could not switch scores!";
	}
	
}
