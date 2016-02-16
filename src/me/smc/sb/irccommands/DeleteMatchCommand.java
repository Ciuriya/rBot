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
	public void onCommand(MessageEvent<PircBotX> e, PrivateMessageEvent<PircBotX> pe, String discord, String[] args){
		if(!Utils.checkArguments(e, pe, discord, args, 2)) return;
		
		String tournamentName = "";
		
		for(int i = 0; i < args.length - 1; i++) tournamentName += args[i] + " ";
		Tournament t = Tournament.getTournament(tournamentName.substring(0, tournamentName.length() - 1));
		
		if(t == null){Utils.info(e, pe, discord, "Invalid tournament!"); return;}
		if(Utils.stringToInt(args[args.length - 1]) == -1){Utils.info(e, pe, discord, "Match number needs to be a number!"); return;}
		
		if(t.removeMatch(Utils.stringToInt(args[args.length - 1])))
			Utils.info(e, pe, discord, "Deleted match #" + args[args.length - 1] + "!");
		else Utils.info(e, pe, discord, "Match does not exist!");
	}
	
}
