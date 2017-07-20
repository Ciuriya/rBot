package me.smc.sb.irccommands;

import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.PrivateMessageEvent;

import me.smc.sb.perm.Permissions;
import me.smc.sb.tourney.Tournament;
import me.smc.sb.utils.Utils;

public class SetPickWaitTimeCommand extends IRCCommand{

	public SetPickWaitTimeCommand(){
		super("Sets the time (in seconds) that players have to pick a map, if exceeded, warmups are skipped, picks have the team lose their turn. 0 or less means unlimited.",
			  "<tournament name> <time> ",
			  Permissions.TOURNEY_ADMIN,
			  "setpickwaittime");
	}

	@Override
	public String onCommand(MessageEvent e, PrivateMessageEvent pe, String discord, String[] args){
		String argCheck = Utils.checkArguments(args, 2);
		if(argCheck.length() > 0) return argCheck;
		
		String tournamentName = "";
		
		for(int i = 0; i < args.length - 1; i++) tournamentName += args[i] + " ";
		Tournament t = Tournament.getTournament(tournamentName.substring(0, tournamentName.length() - 1));
		
		if(t == null) return "Invalid tournament!";
		
		String user = Utils.toUser(e, pe);
		
		if(t.isAdmin(user)){
			t.set("pickWaitTime", Utils.stringToInt(args[args.length - 1]));
			t.save(false);
			
			return "Set the tournament's pick wait time to " + t.getInt("pickWaitTime") + "!";
		}
		
		return "";
	}
	
}
