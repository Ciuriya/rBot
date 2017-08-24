package me.smc.sb.irccommands;

import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.PrivateMessageEvent;

import me.smc.sb.perm.Permissions;
import me.smc.sb.tourney.AlternativeScoringLobby;
import me.smc.sb.utils.Utils;

public class StartAlternativeScoringLobbyCommand extends IRCCommand{

	public StartAlternativeScoringLobbyCommand(){
		super("Starts an alternative scoring lobby",
			  "<name> <scoring strategy> ",
			  Permissions.IRC_BOT_ADMIN,
			  "startasl");
	}
	
	@Override
	public String onCommand(MessageEvent e, PrivateMessageEvent pe, String discord, String[] args){
		String argCheck = Utils.checkArguments(args, 2);
		if(argCheck.length() > 0) return argCheck;
		
		String name = "";
		
		for(int i = 0; i < args.length - 1; i++) name += args[i] + " ";
		
		name = name.substring(0, name.length() - 1);
		
		AlternativeScoringLobby asl = new AlternativeScoringLobby();
		
		asl.start(Utils.toUser(e, pe), name, args[args.length - 1]);
		
		return "Started an alternative scoring match! You will be invited soon.";
	}
	
}
