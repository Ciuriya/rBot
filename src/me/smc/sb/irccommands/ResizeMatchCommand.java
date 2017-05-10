package me.smc.sb.irccommands;

import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.PrivateMessageEvent;

import me.smc.sb.multi.Match;
import me.smc.sb.multi.Tournament;
import me.smc.sb.perm.Permissions;
import me.smc.sb.utils.Utils;

public class ResizeMatchCommand extends IRCCommand{

	public ResizeMatchCommand(){
		super("Resizes a match (currently running or not).",
			  "<tournament name> <match num> <players> ",
			  Permissions.TOURNEY_ADMIN,
			  "resizematch");
	}
	
	@Override
	public String onCommand(MessageEvent e, PrivateMessageEvent pe, String discord, String[] args){
		String argCheck = Utils.checkArguments(args, 3);
		if(argCheck.length() > 0) return argCheck;
		
		String tournamentName = "";
		
		for(int i = 0; i < args.length - 2; i++) tournamentName += args[i] + " ";
		Tournament t = Tournament.getTournament(tournamentName.substring(0, tournamentName.length() - 1));
		
		if(t == null) return "Invalid tournament!";
		if(Utils.stringToInt(args[args.length - 2]) == -1) return "Match number needs to be a number!";
		
		Match match = t.getMatch(Utils.stringToInt(args[args.length - 2]));
		if(match == null) return "The match is invalid!";
		
		if(Utils.stringToInt(args[args.length - 1]) == -1) return "Player amount must be a number!";
		if(Utils.stringToInt(args[args.length - 1]) < 2) return "Player amount must be greater or equal to 2!";
		
		String user = Utils.toUser(e, pe);
		
		if(match.isMatchAdmin(user)){
			match.resize(Utils.stringToInt(args[args.length - 1]));
			match.save(false);
			
			return "Set match #" + args[args.length - 2] + "'s player size to " + 
					Utils.stringToInt(args[args.length - 1]);
		}
		
		return "";
	}
	
}
