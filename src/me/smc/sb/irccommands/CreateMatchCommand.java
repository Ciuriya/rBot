package me.smc.sb.irccommands;

import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.PrivateMessageEvent;

import me.smc.sb.perm.Permissions;
import me.smc.sb.tourney.Match;
import me.smc.sb.tourney.Tournament;
import me.smc.sb.utils.Utils;

public class CreateMatchCommand extends IRCCommand{

	public CreateMatchCommand(){
		super("Creates a multiplayer instance for a tournament match.",
			  "<tournament name> ",
			  Permissions.TOURNEY_ADMIN,
			  "mpcreate");
	}

	@Override
	public String onCommand(MessageEvent e, PrivateMessageEvent pe, String discord, String[] args){		
		String argCheck = Utils.checkArguments(args, 1);
		if(argCheck.length() > 0) return argCheck;
		
		String tournamentName = "";
		
		for(int i = 0; i < args.length; i++) tournamentName += args[i] + " ";
		Tournament t = Tournament.getTournament(tournamentName.substring(0, tournamentName.length() - 1));
		
		if(t == null) return "Invalid tournament!";
		
		String user = Utils.toUser(e, pe);
		
		if(t.isAdmin(user)){
			Match match = new Match(t);
			
			return "Created match #" + match.getMatchNum();
		}
		
		return "";
	}

}
