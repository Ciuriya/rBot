package me.smc.sb.irccommands;

import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.PrivateMessageEvent;

import me.smc.sb.perm.Permissions;
import me.smc.sb.tourney.Match;
import me.smc.sb.tourney.Tournament;
import me.smc.sb.utils.Utils;

public class ListGamesCommand extends IRCCommand{

	public ListGamesCommand(){
		super("Lists all running games.",
			  "<tournament name> ",
			  Permissions.TOURNEY_ADMIN,
			  "listgames");
	}

	@Override
	public String onCommand(MessageEvent e, PrivateMessageEvent pe, String discord, String[] args){
		String argCheck = Utils.checkArguments(args, 1);
		if(argCheck.length() > 0) return argCheck;
		
		String tournamentName = "";
		
		for(int i = 0; i < args.length; i++) tournamentName += args[i] + " ";
		Tournament t = Tournament.getTournament(tournamentName.substring(0, tournamentName.length() - 1));
		
		if(t == null) return "Invalid tournament!";
		
		String user = Utils.toUser(e, pe);
		
		String msg = "";
		
		for(Match match : Match.getMatches(t))
			if(match.getGame() != null && match.isMatchAdmin(user))
				msg += "MP #" + match.getGame().getMpNum() + " (match #" + match.getMatchNum() + ") " +
					   match.getFirstTeam().getTeamName() + " vs " + match.getSecondTeam().getTeamName() + "\n";
		
		if(msg.length() > 0) msg = msg.substring(0, msg.length() - 1);
		
		return msg;
	}
	
}
