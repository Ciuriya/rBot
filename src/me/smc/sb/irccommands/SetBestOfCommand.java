package me.smc.sb.irccommands;

import org.pircbotx.PircBotX;
import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.PrivateMessageEvent;

import me.smc.sb.multi.Tournament;
import me.smc.sb.perm.Permissions;
import me.smc.sb.utils.Utils;

public class SetBestOfCommand extends IRCCommand{

	public SetBestOfCommand(){
		super("Sets a match's best of.",
			  "<tournament name> <match num> <best of #>",
			  Permissions.IRC_BOT_ADMIN,
			  "mpsetbestof");
	}

	@Override
	public void onCommand(MessageEvent<PircBotX> e, PrivateMessageEvent<PircBotX> pe, String discord, String[] args){
		if(!Utils.checkArguments(e, pe, discord, args, 3)) return;
		
		String tournamentName = "";
		
		for(int i = 0; i < args.length - 2; i++) tournamentName += args[i] + " ";
		Tournament t = Tournament.getTournament(tournamentName.substring(0, tournamentName.length() - 1));
		
		if(t == null){Utils.info(e, pe, discord, "Invalid tournament!"); return;}
		if(Utils.stringToInt(args[args.length - 2]) == -1){Utils.info(e, pe, discord, "Match number needs to be a number!"); return;}
		if(t.getMatch(Utils.stringToInt(args[args.length - 2])) == null){Utils.info(e, pe, discord, "The match is invalid!"); return;}
		if(Utils.stringToInt(args[args.length - 1]) <= 0){Utils.info(e, pe, discord, "The best of number isn't valid!"); return;}
		
		t.getMatch(Utils.stringToInt(args[args.length - 2])).setBestOf(Utils.stringToInt(args[args.length - 1]));
		t.getMatch(Utils.stringToInt(args[args.length - 2])).save(false);
		
		Utils.info(e, pe, discord, "Match set to best of " + Utils.stringToInt(args[args.length - 1]) + "!");
	}
	
}
