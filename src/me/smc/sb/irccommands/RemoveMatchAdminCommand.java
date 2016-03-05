package me.smc.sb.irccommands;

import org.pircbotx.PircBotX;
import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.PrivateMessageEvent;

import me.smc.sb.multi.Match;
import me.smc.sb.multi.Tournament;
import me.smc.sb.perm.Permissions;
import me.smc.sb.utils.Utils;

public class RemoveMatchAdminCommand extends IRCCommand{

	public RemoveMatchAdminCommand(){
		super("Removes a match admin from the match.",
			  "<tournament name> <match number> <match admin (use underscores, not spaces)>",
			  Permissions.IRC_BOT_ADMIN,
			  "mpremoveadmin");
	}

	@Override
	public void onCommand(MessageEvent<PircBotX> e, PrivateMessageEvent<PircBotX> pe, String discord, String[] args){
		if(!Utils.checkArguments(e, pe, discord, args, 2)) return;
		
		String tournamentName = "";
		
		for(int i = 0; i < args.length - 2; i++) tournamentName += args[i] + " ";
		Tournament t = Tournament.getTournament(tournamentName.substring(0, tournamentName.length() - 1));
		
		if(t == null){Utils.info(e, pe, discord, "Invalid tournament!"); return;}
		if(Utils.stringToInt(args[args.length - 2]) == -1){Utils.info(e, pe, discord, "Match number needs to be a number!"); return;}
		
		Match match = t.getMatch(Utils.stringToInt(args[args.length - 2]));
		
		match.removeMatchAdmin(args[args.length - 1]);
		match.save(false);
		
		Utils.info(e, pe, discord, "Removed " + args[args.length - 1] + " to the admins of match #" + match.getMatchNum());
	}
	
}
