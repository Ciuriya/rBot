package me.smc.sb.irccommands;

import org.pircbotx.PircBotX;
import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.PrivateMessageEvent;

import me.smc.sb.multi.Match;
import me.smc.sb.multi.Tournament;
import me.smc.sb.utils.Utils;

public class SetGameScoreCommand extends IRCCommand{

	public SetGameScoreCommand(){
		super("Sets the scores of both teams in the game.",
			  "<tournament name> <match number> <team 1 score> <team 2 score>",
			  null,
			  "setscores");
	}

	@Override
	public String onCommand(MessageEvent<PircBotX> e, PrivateMessageEvent<PircBotX> pe, String discord, String[] args){
		String argCheck = Utils.checkArguments(args, 2);
		if(argCheck.length() > 0) return argCheck;
		
		String tournamentName = "";
		
		for(int i = 0; i < args.length - 3; i++) tournamentName += args[i] + " ";
		Tournament t = Tournament.getTournament(tournamentName.substring(0, tournamentName.length() - 1));
		
		if(t == null) return "Invalid tournament!";
		if(Utils.stringToInt(args[args.length - 3]) == -1) return "Match number needs to be a number!";
		if(Utils.stringToInt(args[args.length - 2]) == -1) return "Scores need to be numbers!";
		if(Utils.stringToInt(args[args.length - 1]) == -1) return "Scores need to be numbers!";
		
		Match match = t.getMatch(Utils.stringToInt(args[args.length - 3]));
		
		String user = Utils.toUser(e, pe);
		
		if(match.isMatchAdmin(user) && match.getGame() != null){
			match.getGame().setScores(Utils.stringToInt(args[args.length - 2]), Utils.stringToInt(args[args.length - 1]));
			return "Scores switched: " + Utils.stringToInt(args[args.length - 2]) + " - " + Utils.stringToInt(args[args.length - 1]);
		}
		
		return "";
	}
	
}
