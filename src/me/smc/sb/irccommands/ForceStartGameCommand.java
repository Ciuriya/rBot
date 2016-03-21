package me.smc.sb.irccommands;

import org.pircbotx.PircBotX;
import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.PrivateMessageEvent;

import me.smc.sb.multi.Match;
import me.smc.sb.multi.Tournament;
import me.smc.sb.utils.Utils;

public class ForceStartGameCommand extends IRCCommand{

	public ForceStartGameCommand(){
		super("Force starts the tournament match.",
			  "<tournament name> <match number> ",
			  null,
			  "fstart");
	}

	@Override
	public String onCommand(MessageEvent<PircBotX> e, PrivateMessageEvent<PircBotX> pe, String discord, String[] args){
		String argCheck = Utils.checkArguments(args, 2);
		if(argCheck.length() > 0) return argCheck;
		
		String tournamentName = "";
		
		for(int i = 0; i < args.length - 1; i++) tournamentName += args[i] + " ";
		Tournament t = Tournament.getTournament(tournamentName.substring(0, tournamentName.length() - 1));
		
		if(t == null) return "Invalid tournament!";
		if(Utils.stringToInt(args[args.length - 1]) == -1) return "Match number needs to be a number!";
		
		Match match = t.getMatch(Utils.stringToInt(args[args.length - 1]));
		
		String user = Utils.toUser(e, pe);
		
		if(match.isMatchAdmin(user)){
			match.start();
			return "Game force started!";
		}
		
		return "";
	}
	
}
