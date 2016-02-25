package me.smc.sb.irccommands;

import org.pircbotx.PircBotX;
import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.PrivateMessageEvent;

import me.smc.sb.multi.Match;
import me.smc.sb.multi.Tournament;
import me.smc.sb.utils.Utils;

public class ListGamesCommand extends IRCCommand{

	public ListGamesCommand(){
		super("Lists all running games.",
			  "<tournament name> ",
			  null,
			  "tournamentlistgames");
	}

	@Override
	public void onCommand(MessageEvent<PircBotX> e, PrivateMessageEvent<PircBotX> pe, String discord, String[] args){
		if(!Utils.checkArguments(e, pe, discord, args, 1)) return;
		
		String tournamentName = "";
		
		for(int i = 0; i < args.length; i++) tournamentName += args[i] + " ";
		Tournament t = Tournament.getTournament(tournamentName.substring(0, tournamentName.length() - 1));
		
		if(t == null){Utils.info(e, pe, discord, "Invalid tournament!"); return;}
		
		String user = Utils.toUser(e, pe);
		
		for(Match match : t.getMatches())
			if(match.getGame() != null && match.isMatchAdmin(user == null ? discord : user))
				Utils.info(e, pe, discord, "MP #" + match.getGame().getMpNum() + " (match #" + match.getMatchNum() + ") " +
											match.getFirstTeam().getTeamName() + " vs " + match.getSecondTeam().getTeamName());
	}
	
}
