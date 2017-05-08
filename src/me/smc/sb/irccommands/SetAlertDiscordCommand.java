package me.smc.sb.irccommands;

import org.pircbotx.PircBotX;
import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.PrivateMessageEvent;

import me.smc.sb.multi.Tournament;
import me.smc.sb.perm.Permissions;
import me.smc.sb.utils.Utils;

public class SetAlertDiscordCommand extends IRCCommand{

	public SetAlertDiscordCommand(){
		super("Sets the discord channel to send staff alerts in.",
			  "<tournament name> ",
			  Permissions.TOURNEY_ADMIN,
			  "setalertdiscord");
	}
	
	@Override
	public String onCommand(MessageEvent<PircBotX> e, PrivateMessageEvent<PircBotX> pe, String discord, String[] args){
		String argCheck = Utils.checkArguments(args, 1);
		if(argCheck.length() > 0) return argCheck;
		
		String tournamentName = "";
		
		for(int i = 0; i < args.length; i++) tournamentName += args[i] + " ";
		
		Tournament t = Tournament.getTournament(tournamentName.substring(0, tournamentName.length() - 1));
		if(t == null) return "The tournament does not exist!";
		
		String user = Utils.toUser(e, pe);
		
		if(t.isAdmin(user)){
			if(discord == null) return "You need to use this command in the discord channel you wish to receive alerts into!";
			
			t.setAlertDiscord(discord);
			t.save(false);
			
			return "The alert discord was set!";
		}
		
		return "";
	}
	
}
