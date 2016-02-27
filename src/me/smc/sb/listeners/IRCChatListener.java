package me.smc.sb.listeners;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import org.pircbotx.PircBotX;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.PrivateMessageEvent;

import me.smc.sb.irccommands.IRCCommand;
import me.smc.sb.main.Main;
import me.smc.sb.multi.Game;
import me.smc.sb.multi.Match;
import me.smc.sb.multi.Tournament;
import me.smc.sb.utils.Log;
import me.smc.sb.utils.Utils;

public class IRCChatListener extends ListenerAdapter<PircBotX>{

	public static boolean yieldPMs = false;
	public static Map<String, Game> gamesListening = new HashMap<>();
	
	@Override
	public void onPrivateMessage(PrivateMessageEvent<PircBotX> e){
		String message = e.getMessage();
		Log.logger.log(Level.INFO, "PM/" + e.getUser().getNick() + ": " + message);
		if(verifyGameCreationPM(e)) return;
		
		if(yieldPMs) Utils.infoBypass(Main.api.getUserById("91302128328392704").getPrivateChannel(), "PM/" + e.getUser().getNick() + ": " + message);
		if(message.startsWith("!")) IRCCommand.handleCommand(null, e, null, message.substring(1));
	}
	
	@Override
	public void onMessage(MessageEvent<PircBotX> e){
		String message = e.getMessage();
		Log.logger.log(Level.INFO, "IRC/" + e.getUser().getNick() + ": " + message);	
		if(verifyBanchoFeedback(e)) return;
		
		if(message.startsWith("!")) IRCCommand.handleCommand(e, null, null, message.substring(1));
	}
	
	private boolean verifyBanchoFeedback(MessageEvent<PircBotX> e){
		if(e.getUser().getNick().equalsIgnoreCase("BanchoBot"))
			for(String game : gamesListening.keySet())
				if(e.getChannel().getName().equalsIgnoreCase(game)){
					gamesListening.get(game).handleBanchoFeedback(e.getMessage());
					return true;
				}
		
		return false;
	}

	private boolean verifyGameCreationPM(PrivateMessageEvent<PircBotX> e){
		if(e.getUser().getNick().equalsIgnoreCase("BanchoBot") &&
		   e.getMessage().contains("Created the tournament match")){
			String trim = e.getMessage().replace("Created the tournament match ", "");
			String[] trimSplit = trim.split(" ");
			String mpLink = trimSplit[0];
			String gameName = "";
			String tournamentName = "";
			
			for(int i = 1; i < trimSplit.length; i++)
				gameName += trimSplit[i] + " ";
			gameName = gameName.substring(0, gameName.length() - 1);

			tournamentName = gameName.split(" ")[0];
			tournamentName = tournamentName.substring(0, tournamentName.length() - 1);

			Tournament t = Tournament.getTournament(tournamentName);
			if(t == null) return false;
			
			for(Match match : t.getMatches())
				if(match.getLobbyName().equalsIgnoreCase(gameName) &&
				   match.getGame() != null){
					match.getGame().start("#mp_" + mpLink.split("mp\\/")[1], mpLink);
					return true;
				}
		}
		return false;
	}
	
}
