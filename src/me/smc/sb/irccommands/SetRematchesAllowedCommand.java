package me.smc.sb.irccommands;

import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.PrivateMessageEvent;

import me.smc.sb.perm.Permissions;
import me.smc.sb.tourney.Tournament;
import me.smc.sb.utils.Utils;

public class SetRematchesAllowedCommand extends IRCCommand{

	public SetRematchesAllowedCommand(){
		super("Set rematches allowed per match per team.",
			  "<tournament name> <amount> ",
			  Permissions.TOURNEY_ADMIN,
			  "setrematchesallowed");
	}

	@Override
	public String onCommand(MessageEvent e, PrivateMessageEvent pe, String discord, String[] args){
		String argCheck = Utils.checkArguments(args, 2);
		if(argCheck.length() > 0) return argCheck;
		
		String tournamentName = "";
		
		for(int i = 0; i < args.length - 1; i++) tournamentName += args[i] + " ";
		Tournament t = Tournament.getTournament(tournamentName.substring(0, tournamentName.length() - 1));
		
		if(t == null) return "Invalid tournament!";
		if(Utils.stringToInt(args[args.length - 1]) == -1) return "Invalid amount.";		
		
		String user = Utils.toUser(e, pe);
		
		if(t.isAdmin(user)){
			t.set("rematchesAllowed", Utils.stringToInt(args[args.length - 1]));
			
			return "Set the tournament's rematches allowed per match to " + t.getInt("rematchesAllowed") + "!";
		}
		
		return "";
	}
	
}
