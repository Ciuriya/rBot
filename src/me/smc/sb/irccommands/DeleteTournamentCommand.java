package me.smc.sb.irccommands;

import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.PrivateMessageEvent;

import me.smc.sb.perm.Permissions;
import me.smc.sb.tourney.Tournament;
import me.smc.sb.utils.Utils;

public class DeleteTournamentCommand extends IRCCommand{
	
	public DeleteTournamentCommand(){
		super("Deletes a tournament.",
			  "<tournament name> ",
			  Permissions.IRC_BOT_ADMIN,
			  "tournamentdelete");
	}

	@Override
	public String onCommand(MessageEvent e, PrivateMessageEvent pe, String discord, String[] args){
		String argCheck = Utils.checkArguments(args, 1);
		if(argCheck.length() > 0) return argCheck;
		
		String tournamentName = "";
		
		for(int i = 0; i < args.length; i++) tournamentName += args[i] + " ";
		Tournament t = Tournament.getTournament(tournamentName.substring(0, tournamentName.length() - 1));
		
		if(t == null) return "Invalid tournament!";
		
		t.delete();
		return "Deleted tournament " + tournamentName.substring(0, tournamentName.length() - 1) + "!";
	}

}
