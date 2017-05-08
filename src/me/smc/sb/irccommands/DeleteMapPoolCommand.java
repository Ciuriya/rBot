package me.smc.sb.irccommands;

import org.pircbotx.PircBotX;
import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.PrivateMessageEvent;

import me.smc.sb.multi.Tournament;
import me.smc.sb.perm.Permissions;
import me.smc.sb.utils.Utils;

public class DeleteMapPoolCommand extends IRCCommand{

	public DeleteMapPoolCommand(){
		super("Deletes a map pool.",
			  "<tournament name> <map pool number> ",
			  Permissions.TOURNEY_ADMIN,
			  "pooldelete");
	}
	
	@Override
	public String onCommand(MessageEvent<PircBotX> e, PrivateMessageEvent<PircBotX> pe, String discord, String[] args){
		String argCheck = Utils.checkArguments(args, 2);
		if(argCheck.length() > 0) return argCheck;
		
		String tournamentName = "";
		
		for(int i = 0; i < args.length - 1; i++) tournamentName += args[i] + " ";
		Tournament t = Tournament.getTournament(tournamentName.substring(0, tournamentName.length() - 1));
		
		if(t == null) return "Invalid tournament!";
		if(Utils.stringToInt(args[args.length - 1]) == -1) return "Map pool number needs to be a number!";
		
		String user = Utils.toUser(e, pe);
		
		if(t.isAdmin(user)){
			if(t.removePool(Utils.stringToInt(args[args.length - 1])))
				return "Deleted map pool #" + args[args.length - 1] + "!";
			else return "Map pool does not exist!";
		}
		
		return "";
	}
	
}
