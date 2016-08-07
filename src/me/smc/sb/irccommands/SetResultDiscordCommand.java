package me.smc.sb.irccommands;

import org.pircbotx.PircBotX;
import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.PrivateMessageEvent;

import me.smc.sb.multi.Tournament;
import me.smc.sb.perm.Permissions;
import me.smc.sb.utils.Utils;

public class SetResultDiscordCommand extends IRCCommand{

	public SetResultDiscordCommand(){
		super("Sets the discord channel to send match results in.",
			  "<tournament name> ",
			  Permissions.IRC_BOT_ADMIN,
			  "setresultdiscord");
	}
	
	@Override
	public String onCommand(MessageEvent<PircBotX> e, PrivateMessageEvent<PircBotX> pe, String discord, String[] args){
		String argCheck = Utils.checkArguments(args, 1);
		if(argCheck.length() > 0) return argCheck;
		
		String tournamentName = "";
		
		for(int i = 0; i < args.length; i++) tournamentName += args[i] + " ";
		
		if(Tournament.getTournament(tournamentName.substring(0, tournamentName.length() - 1)) == null)
			return "The tournament does not exist!";
		
		if(discord == null) return "You need to use this command in the discord channel you wish to receive results into!";
		
		Tournament.getTournament(tournamentName.substring(0, tournamentName.length() - 1)).setResultDiscord(discord);
		Tournament.getTournament(tournamentName.substring(0, tournamentName.length() - 1)).save(false);
		return "The result discord was set!";
	}
	
}
