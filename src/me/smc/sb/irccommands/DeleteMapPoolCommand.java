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
			  Permissions.IRC_BOT_ADMIN,
			  "pooldelete");
	}
	
	@Override
	public void onCommand(MessageEvent<PircBotX> e, PrivateMessageEvent<PircBotX> pe, String discord, String[] args){
		if(!Utils.checkArguments(e, pe, discord, args, 2)) return;
		
		String tournamentName = "";
		
		for(int i = 0; i < args.length - 1; i++) tournamentName += args[i] + " ";
		Tournament t = Tournament.getTournament(tournamentName.substring(0, tournamentName.length() - 1));
		
		if(t == null){Utils.info(e, pe, discord, "Invalid tournament!"); return;}
		if(Utils.stringToInt(args[args.length - 1]) == -1){Utils.info(e, pe, discord, "Map pool number needs to be a number!"); return;}
		
		if(t.removePool(Utils.stringToInt(args[args.length - 1])))
			Utils.info(e, pe, discord, "Deleted map pool #" + args[args.length - 1] + "!");
		else Utils.info(e, pe, discord, "Map pool does not exist!");
	}
	
}
