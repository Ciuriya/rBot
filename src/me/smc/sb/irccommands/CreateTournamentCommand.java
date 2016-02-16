package me.smc.sb.irccommands;

import org.pircbotx.PircBotX;
import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.PrivateMessageEvent;

import me.smc.sb.multi.Tournament;
import me.smc.sb.perm.Permissions;
import me.smc.sb.utils.Utils;

public class CreateTournamentCommand extends IRCCommand{

	public CreateTournamentCommand(){
		super("Creates a tournament.",
			  "<tournament name> ",
			  Permissions.IRC_BOT_ADMIN,
			  "tournamentcreate");
	}
	
	@Override
	public void onCommand(MessageEvent<PircBotX> e, PrivateMessageEvent<PircBotX> pe, String discord, String[] args){
		if(!Utils.checkArguments(e, pe, discord, args, 1)) return;
		
		String tournamentName = "";
		
		for(int i = 0; i < args.length; i++) tournamentName += args[i] + " ";
		new Tournament(tournamentName.substring(0, tournamentName.length() - 1));
		
		Utils.info(e, pe, discord, "Created the " + tournamentName.substring(0, tournamentName.length() - 1) + " tournament!");
	}
	
}
