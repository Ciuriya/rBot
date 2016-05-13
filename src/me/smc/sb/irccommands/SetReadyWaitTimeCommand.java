package me.smc.sb.irccommands;

import org.pircbotx.PircBotX;
import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.PrivateMessageEvent;

import me.smc.sb.multi.Tournament;
import me.smc.sb.perm.Permissions;
import me.smc.sb.utils.Utils;

public class SetReadyWaitTimeCommand extends IRCCommand{

	public SetReadyWaitTimeCommand(){
		super("Sets the time (in seconds) that players have to ready up, if exceeded, the match will attempt to start. 0 or less means unlimited.",
			  "<tournament name> <time> ",
			  Permissions.IRC_BOT_ADMIN,
			  "tournamentsetreadywaittime");
	}

	@Override
	public String onCommand(MessageEvent<PircBotX> e, PrivateMessageEvent<PircBotX> pe, String discord, String[] args){
		String argCheck = Utils.checkArguments(args, 2);
		if(argCheck.length() > 0) return argCheck;
		
		String tournamentName = "";
		
		for(int i = 0; i < args.length - 1; i++) tournamentName += args[i] + " ";
		Tournament t = Tournament.getTournament(tournamentName.substring(0, tournamentName.length() - 1));
		
		if(t == null) return "Invalid tournament!";
		
		t.setReadyWaitTime(Utils.stringToInt(args[args.length - 1]));
		t.save(false);
		
		return "Set the tournament's ready wait time to " + t.getReadyWaitTime() + "!";
	}
	
}
