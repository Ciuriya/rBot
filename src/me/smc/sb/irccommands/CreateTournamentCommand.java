package me.smc.sb.irccommands;

import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.PrivateMessageEvent;

import me.smc.sb.perm.Permissions;
import me.smc.sb.tourney.Tournament;
import me.smc.sb.utils.Utils;

public class CreateTournamentCommand extends IRCCommand{

	public CreateTournamentCommand(){
		super("Creates a tournament.",
			  "<tournament name> (game mode 0/1/2/3) ",
			  Permissions.IRC_BOT_ADMIN,
			  "tournamentcreate");
	}
	
	@Override
	public String onCommand(MessageEvent e, PrivateMessageEvent pe, String discord, String[] args){		
		String argCheck = Utils.checkArguments(args, 1);
		if(argCheck.length() > 0) return argCheck;
		
		String tournamentName = "";
		int tournamentArgLength = args.length;
		
		if(args[args.length - 1].length() == 1){
			int mode = Utils.stringToInt(args[args.length - 1]);
			if(mode >= 0 && mode <= 3)
				tournamentArgLength--;
		}
		
		for(int i = 0; i < tournamentArgLength; i++) tournamentName += args[i] + " ";
		if(Tournament.getTournament(tournamentName.substring(0, tournamentName.length() - 1)) != null)
			return "This tournament already exists!";
		
		Tournament t = new Tournament(tournamentName.substring(0, tournamentName.length() - 1));
		
		if(tournamentArgLength < args.length)
			t.set("mode", Utils.stringToInt(args[args.length - 1]));
		
		return "Created the " + tournamentName.substring(0, tournamentName.length() - 1) + " tournament!";
	}
	
}
