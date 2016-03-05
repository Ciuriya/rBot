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
	public void onCommand(MessageEvent<PircBotX> e, PrivateMessageEvent<PircBotX> pe, String discord, String[] args){
		if(!Utils.checkArguments(e, pe, discord, args, 2)) return;
		
		String tournamentName = "";
		
		for(int i = 0; i < args.length - 3; i++) tournamentName += args[i] + " ";
		Tournament t = Tournament.getTournament(tournamentName.substring(0, tournamentName.length() - 1));
		
		if(t == null){Utils.info(e, pe, discord, "Invalid tournament!"); return;}
		if(Utils.stringToInt(args[args.length - 3]) == -1){Utils.info(e, pe, discord, "Match number needs to be a number!"); return;}
		if(Utils.stringToInt(args[args.length - 2]) == -1){Utils.info(e, pe, discord, "Scores need to be numbers!"); return;}
		if(Utils.stringToInt(args[args.length - 1]) == -1){Utils.info(e, pe, discord, "Scores need to be numbers!"); return;}
		
		Match match = t.getMatch(Utils.stringToInt(args[args.length - 3]));
		
		String user = Utils.toUser(e, pe);
		
		if(match.isMatchAdmin(user) && match.getGame() != null){
			match.getGame().setScores(Utils.stringToInt(args[args.length - 2]), Utils.stringToInt(args[args.length - 1]));
			Utils.info(e, pe, discord, "Scores switched: " + Utils.stringToInt(args[args.length - 2]) + " - " + Utils.stringToInt(args[args.length - 1]));
		}
	}
	
}
