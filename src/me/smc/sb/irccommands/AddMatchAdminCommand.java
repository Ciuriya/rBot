package me.smc.sb.irccommands;

import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.PrivateMessageEvent;

import me.smc.sb.multi.Match;
import me.smc.sb.multi.Tournament;
import me.smc.sb.perm.Permissions;
import me.smc.sb.utils.Utils;

public class AddMatchAdminCommand extends IRCCommand{

	public AddMatchAdminCommand(){
		super("Adds a match admin to the match.",
			  "<tournament name> <match number> <match admin (use underscores, not spaces)>",
			  Permissions.IRC_BOT_ADMIN,
			  "mpaddadmin");
	}

	@Override
	public String onCommand(MessageEvent e, PrivateMessageEvent pe, String discord, String[] args){		
		String argCheck = Utils.checkArguments(args, 2);
		if(argCheck.length() > 0) return argCheck;
		
		String tournamentName = "";
		
		for(int i = 0; i < args.length - 2; i++) tournamentName += args[i] + " ";
		Tournament t = Tournament.getTournament(tournamentName.substring(0, tournamentName.length() - 1));
		
		if(t == null) return "Invalid tournament!";
		if(Utils.stringToInt(args[args.length - 2]) == -1) return "Match number needs to be a number!";
		
		Match match = t.getMatch(Utils.stringToInt(args[args.length - 2]));
		
		match.addMatchAdmin(args[args.length - 1]);
		match.save(false);
		
		return "Added " + args[args.length - 1] + " to the admins of match #" + match.getMatchNum();
	}
	
}
