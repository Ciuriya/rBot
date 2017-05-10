package me.smc.sb.irccommands;

import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.PrivateMessageEvent;

import me.smc.sb.multi.PickStrategy;
import me.smc.sb.multi.Tournament;
import me.smc.sb.perm.Permissions;
import me.smc.sb.utils.Utils;

public class SetPickStrategyCommand extends IRCCommand{

	public SetPickStrategyCommand(){
		super("Sets the tourney's picking strategy (any map, mod picking, etc.)",
			  "<tournament name> <strategy name (regular, mod)> ",
			  Permissions.TOURNEY_ADMIN,
			  "setpickstrategy");
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
			t.setPickStrategy(args[args.length - 1]);
			t.save(false);
			
			return "Set the tournament's pick strategy to " + PickStrategy.getStrategyName(t.getPickStrategy()) + "!";
		}
		
		return "";
	}
	
}
