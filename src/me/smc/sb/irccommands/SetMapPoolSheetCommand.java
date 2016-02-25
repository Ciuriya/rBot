package me.smc.sb.irccommands;

import org.pircbotx.PircBotX;
import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.PrivateMessageEvent;

import me.smc.sb.multi.Tournament;
import me.smc.sb.perm.Permissions;
import me.smc.sb.utils.Utils;

public class SetMapPoolSheetCommand extends IRCCommand{

	public SetMapPoolSheetCommand(){
		super("Sets the sheet which lists the maps in the map pool.",
			  "<tournament name> <map pool number> <sheet url> ",
			  Permissions.IRC_BOT_ADMIN,
			  "poolsetsheet");
	}

	@Override
	public void onCommand(MessageEvent<PircBotX> e, PrivateMessageEvent<PircBotX> pe, String discord, String[] args){
		if(!Utils.checkArguments(e, pe, discord, args, 3)) return;
		
		String tournamentName = "";
		
		for(int i = 0; i < args.length - 2; i++) tournamentName += args[i] + " ";
		Tournament t = Tournament.getTournament(tournamentName.substring(0, tournamentName.length() - 1));
		
		if(t == null){Utils.info(e, pe, discord, "Invalid tournament!"); return;}
		
		if(Utils.stringToInt(args[args.length - 2]) == -1){Utils.info(e, pe, discord, "Map pool number needs to be a number!"); return;}
		if(t.getPool(Utils.stringToInt(args[args.length - 2])) == null){Utils.info(e, pe, discord, "The map pool is invalid!"); return;}
		
		t.getPool(Utils.stringToInt(args[args.length - 2])).setSheetUrl(args[args.length - 1]);
		t.getPool(Utils.stringToInt(args[args.length - 2])).save(false);
		
		Utils.info(e, pe, discord, "Set the map pool's sheet to " + args[args.length - 1] + "!");
	}
	
}
