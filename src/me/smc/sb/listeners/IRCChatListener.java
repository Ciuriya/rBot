package me.smc.sb.listeners;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
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

	public static List<String> pmList = new ArrayList<>();
	public static LinkedList<String> pmsToSend = new LinkedList<>();
	public static Timer pmQueue;
	private boolean started = false;
	public static Map<String, Game> gamesListening = new HashMap<>();
	public static List<String> gameCreatePMs = new ArrayList<>();
	
	@Override
	public void onPrivateMessage(PrivateMessageEvent<PircBotX> e){
		String message = e.getMessage();
		Log.logger.log(Level.INFO, "PM/" + e.getUser().getNick() + ": " + message);
		if(verifyGameCreationPM(e)) return;
		
		if(!pmList.isEmpty())
			for(String id : pmList)
				addToPMQueue(id, "PM/" + e.getUser().getNick() + ": " + message);
		
		if(message.startsWith("!")) Utils.info(null, e, null, IRCCommand.handleCommand(null, e, null, message.substring(1)));
	}
	
	private void addToPMQueue(String id, String message){
		pmsToSend.add(id + "||" + message);
		
		if(!started){
			started = true;
			
			Timer pmQueue = new Timer();
			
			pmQueue.scheduleAtFixedRate(new TimerTask(){
				public void run(){
					if(!pmsToSend.isEmpty()){
						String basePM = pmsToSend.getLast();
						pmsToSend.removeLast();
						Utils.infoBypass(Main.api.getUserById(basePM.split("\\|\\|")[0]).getPrivateChannel(), basePM.split("\\|\\|")[1]);
					}
				}
			}, 2000, 2000);
		}
	}
	
	@Override
	public void onMessage(MessageEvent<PircBotX> e){
		String message = e.getMessage();
		Log.logger.log(Level.INFO, "IRC/" + e.getUser().getNick() + ": " + message);	
		if(verifyBanchoFeedback(e)) return;
		
		if(message.startsWith("!")) Utils.info(e, null, null, IRCCommand.handleCommand(e, null, null, message.substring(1)));
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

			gameCreatePMs.add(mpLink.split("mp\\/")[1] + "|" + gameName);
			
			Tournament t = Tournament.getTournament(tournamentName);
			if(t == null) return false;
			
			for(Match match : t.getMatches())
				if(match.getLobbyName().equalsIgnoreCase(gameName) &&
				   match.getGame() != null){
					match.getGame().start("#mp_" + mpLink.split("mp\\/")[1], mpLink);
					return true;
				}
			
			Log.logger.log(Level.INFO, "------------ Failed Game: " + gameName);
		}
		return false;
	}
	
}
