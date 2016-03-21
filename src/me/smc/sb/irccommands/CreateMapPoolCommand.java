package me.smc.sb.irccommands;

import org.pircbotx.PircBotX;
import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.PrivateMessageEvent;

import me.smc.sb.multi.MapPool;
import me.smc.sb.multi.Tournament;
import me.smc.sb.perm.Permissions;
import me.smc.sb.utils.Utils;

public class CreateMapPoolCommand extends IRCCommand{

	public CreateMapPoolCommand(){
		super("Creates a map pool for use in a tournament.",
			  "<tournament name> ",
			  Permissions.IRC_BOT_ADMIN,
			  "poolcreate");
	}
	
	@Override
	public String onCommand(MessageEvent<PircBotX> e, PrivateMessageEvent<PircBotX> pe, String discord, String[] args){
		String argCheck = Utils.checkArguments(args, 1);
		if(argCheck.length() > 0) return argCheck;
		
		String tournamentName = "";
		
		for(int i = 0; i < args.length; i++) tournamentName += args[i] + " ";
		Tournament t = Tournament.getTournament(tournamentName.substring(0, tournamentName.length() - 1));
		
		if(t == null) return "Invalid tournament!";
		
		MapPool pool = new MapPool(t);
		return "Created map pool #" + pool.getPoolNum() + "!";
	}
	
}
