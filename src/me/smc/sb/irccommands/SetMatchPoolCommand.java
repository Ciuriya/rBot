package me.smc.sb.irccommands;

import org.pircbotx.PircBotX;
import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.PrivateMessageEvent;

import me.smc.sb.multi.Tournament;
import me.smc.sb.perm.Permissions;
import me.smc.sb.utils.Utils;

public class SetMatchPoolCommand extends IRCCommand{

	public SetMatchPoolCommand(){
		super("Sets a match's map pool.",
			  "<tournament name> <match num> <map pool num>",
			  Permissions.TOURNEY_ADMIN,
			  "mpsetpool");
	}
	
	@Override
	public String onCommand(MessageEvent<PircBotX> e, PrivateMessageEvent<PircBotX> pe, String discord, String[] args){
		String argCheck = Utils.checkArguments(args, 3);
		if(argCheck.length() > 0) return argCheck;
		
		String tournamentName = "";
		
		for(int i = 0; i < args.length - 2; i++) tournamentName += args[i] + " ";
		Tournament t = Tournament.getTournament(tournamentName.substring(0, tournamentName.length() - 1));
		
		if(t == null) return "Invalid tournament!";
		if(Utils.stringToInt(args[args.length - 2]) == -1) return "Match number needs to be a number!";
		if(Utils.stringToInt(args[args.length - 1]) == -1) return "Map pool number needs to be a number!";
		if(t.getMatch(Utils.stringToInt(args[args.length - 2])) == null) return "The match is invalid!";
		if(t.getPool(Utils.stringToInt(args[args.length - 1])) == null) return "The map pool is invalid!";
		
		String user = Utils.toUser(e, pe);
		
		if(t.isAdmin(user)){
			t.getMatch(Utils.stringToInt(args[args.length - 2])).setMapPool(t.getPool(Utils.stringToInt(args[args.length - 1])));
			t.getMatch(Utils.stringToInt(args[args.length - 2])).save(false);
			
			return "Set match #" + args[args.length - 2] + "'s map pool to map pool #" + args[args.length - 1] + "!";
		}
		
		return "";
	}
	
}
