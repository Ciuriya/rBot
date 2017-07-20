package me.smc.sb.irccommands;

import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.PrivateMessageEvent;

import me.smc.sb.main.Main;
import me.smc.sb.perm.Permissions;
import me.smc.sb.tourney.Match;
import me.smc.sb.tourney.Tournament;
import me.smc.sb.tourney.TwitchHandler;
import me.smc.sb.utils.Utils;

public class ChangeStreamedMatchCommand extends IRCCommand{

	public ChangeStreamedMatchCommand(){
		super("Changes the currently streamed match.",
			  "<tournament name> <match number> ",
			  Permissions.TOURNEY_ADMIN,
			  "changestream");
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
		
		Match match = Match.getMatch(t, Utils.stringToInt(args[args.length - 1]));
		
		if(!match.isMatchAdmin(Utils.toUser(e, pe))) return "";
		if(match.getGame() != null){
			TwitchHandler handler = t.getTwitchHandler();
			
			handler.forceStream(match.getGame());
			Main.twitchRegulator.sendMessage(match.getTournament().get("twitchChannel"),
					"Game switched to " + match.getLobbyName());

			return "Switched to " + match.getLobbyName() + "!";
		}
		
		return "";
	}
	
}
