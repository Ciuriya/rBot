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
	public String onCommand(MessageEvent<PircBotX> e, PrivateMessageEvent<PircBotX> pe, String discord, String[] args){
		String argCheck = Utils.checkArguments(args, 3);
		if(argCheck.length() > 0) return argCheck;
		
		String tournamentName = "";
		
		for(int i = 0; i < args.length - 2; i++) tournamentName += args[i] + " ";
		Tournament t = Tournament.getTournament(tournamentName.substring(0, tournamentName.length() - 1));
		
		if(t == null) return "Invalid tournament!";
		if(Utils.stringToInt(args[args.length - 2]) == -1) return "Match number needs to be a number!";
		if(t.getMatch(Utils.stringToInt(args[args.length - 2])) == null) return "The match is invalid!";
		if(Utils.stringToInt(args[args.length - 1]) <= 0) return "The best of number is invalid!";
		
		t.getMatch(Utils.stringToInt(args[args.length - 2])).setBestOf(Utils.stringToInt(args[args.length - 1]));
		t.getMatch(Utils.stringToInt(args[args.length - 2])).save(false);
		
		return "Match set to best of " + Utils.stringToInt(args[args.length - 1]) + "!";
	}
	
}
