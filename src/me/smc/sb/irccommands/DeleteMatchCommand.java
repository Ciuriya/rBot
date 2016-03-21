package me.smc.sb.irccommands;

import org.pircbotx.PircBotX;
import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.PrivateMessageEvent;

import me.smc.sb.multi.Tournament;
import me.smc.sb.perm.Permissions;
import me.smc.sb.utils.Utils;

public class DeleteMatchCommand extends IRCCommand{

	public DeleteMatchCommand(){
		super("Deletes a multiplayer instance.",
			  "<tournament name> <match number> ",
			  Permissions.IRC_BOT_ADMIN,
			  "mpdelete");
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
		
		if(t.removeMatch(Utils.stringToInt(args[args.length - 1])))
			return "Deleted match #" + args[args.length - 1] + "!";
		else return "Match does not exist!";
	}
	
}
