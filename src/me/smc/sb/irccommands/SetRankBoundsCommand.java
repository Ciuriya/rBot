package me.smc.sb.irccommands;

import org.pircbotx.PircBotX;
import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.PrivateMessageEvent;

import me.smc.sb.multi.Tournament;
import me.smc.sb.perm.Permissions;
import me.smc.sb.utils.Utils;

public class SetRankBoundsCommand extends IRCCommand{

	public SetRankBoundsCommand(){
		super("Sets the rank bounds used to verify player ranks.",
			  "<tournament name> <lower bound (closest to 1)> <upper bound> ",
			  Permissions.TOURNEY_ADMIN,
			  "setrankbounds");
	}
	
	@Override
	public String onCommand(MessageEvent<PircBotX> e, PrivateMessageEvent<PircBotX> pe, String discord, String[] args){
		String argCheck = Utils.checkArguments(args, 3);
		if(argCheck.length() > 0) return argCheck;
		
		String tournamentName = "";
		
		for(int i = 0; i < args.length - 2; i++) tournamentName += args[i] + " ";
		
		Tournament t = Tournament.getTournament(tournamentName.substring(0, tournamentName.length() - 1));
		
		if(t == null) return "The tournament does not exist!";
		
		String user = Utils.toUser(e, pe);
		
		if(t.isAdmin(user)){
			int lower = Utils.stringToInt(args[args.length - 2]);
			int upper = Utils.stringToInt(args[args.length - 1]);
			
			if(lower < 0) lower = 0;
			if(upper < 0) upper = 0;
			
			t.setLowerRankBound(lower);
			t.setUpperRankBound(upper);
			t.save(false);
			
			return "The rank bounds are now " + lower + " to " + upper + "!";
		}
		
		return "";
	}
	
}
