package me.smc.sb.irccommands;

import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.PrivateMessageEvent;

import me.smc.sb.multi.GameState;
import me.smc.sb.multi.Match;
import me.smc.sb.multi.Tournament;
import me.smc.sb.perm.Permissions;
import me.smc.sb.utils.Utils;

public class PauseCommand extends IRCCommand{

	public PauseCommand(){
		super("Pauses the tournament match",
			  "<tournament name> <match number> ",
			  Permissions.TOURNEY_ADMIN,
			  "pause");
	}

	@Override
	public String onCommand(MessageEvent e, PrivateMessageEvent pe, String discord, String[] args){
		String argCheck = Utils.checkArguments(args, 2);
		if(argCheck.length() > 0) return argCheck;
		
		String tournamentName = "";
		
		for(int i = 0; i < args.length - 1; i++) tournamentName += args[i] + " ";
		Tournament t = Tournament.getTournament(tournamentName.substring(0, tournamentName.length() - 1));
		
		if(t == null) return "Invalid tournament!";
		if(Utils.stringToInt(args[args.length - 1]) == -1) return "Match number needs to be a number!";
		
		Match match = t.getMatch(Utils.stringToInt(args[args.length - 1]));
		
		if(match.getGame() == null) return "The game is not started!";
		
		String user = Utils.toUser(e, pe);
		
		if(match.isMatchAdmin(user)){
			if(match.getGame().getState().eq(GameState.PAUSED))
				return "The game is already paused!";
			
			match.getGame().handlePause(true);
			return "Game paused!";
		}
		
		return "";
	}
	
}
