package me.smc.sb.irccommands;

import org.pircbotx.PircBotX;
import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.PrivateMessageEvent;

import me.smc.sb.multi.Match;
import me.smc.sb.multi.Tournament;
import me.smc.sb.perm.Permissions;
import me.smc.sb.utils.Utils;

public class CreateMatchCommand extends IRCCommand{

	public CreateMatchCommand(){
		super("Creates a multiplayer instance for a tournament match.",
			  "<tournament name> <nb players> ",
			  Permissions.IRC_BOT_ADMIN,
			  "mpcreate");
	}

	@Override
	public void onCommand(MessageEvent<PircBotX> e, PrivateMessageEvent<PircBotX> pe, String discord, String[] args){
		if(!Utils.checkArguments(e, pe, discord, args, 2)) return;
		
		String tournamentName = "";
		
		for(int i = 0; i < args.length - 1; i++) tournamentName += args[i] + " ";
		Tournament t = Tournament.getTournament(tournamentName.substring(0, tournamentName.length() - 1));
		
		if(t == null){Utils.info(e, pe, discord, "Invalid tournament!"); return;}
		if(Utils.stringToInt(args[args.length - 1]) == -1){Utils.info(e, pe, discord, "Player count needs to be a number!"); return;}
		
		Match match = new Match(t, Utils.stringToInt(args[args.length - 1]));
		Utils.info(e, pe, discord, "Created match #" + match.getMatchNum() + " for " + match.getPlayers() + " players!");
	}

}
