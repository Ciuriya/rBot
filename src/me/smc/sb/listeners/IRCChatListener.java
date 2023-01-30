package me.smc.sb.listeners;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;

import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.PrivateMessageEvent;

import me.smc.sb.irccommands.IRCCommand;
import me.smc.sb.main.Main;
import me.smc.sb.tourney.AlternativeScoringLobby;
import me.smc.sb.tourney.BanchoHandler;
import me.smc.sb.tourney.Match;
import me.smc.sb.tourney.Tournament;
import me.smc.sb.utils.Log;
import me.smc.sb.utils.Utils;

public class IRCChatListener extends ListenerAdapter{

	public static List<String> pmList = new ArrayList<>();
	public static LinkedList<String> pmsToSend = new LinkedList<>();
	public static Timer pmQueue;
	private boolean started = false;
	public static Map<String, BanchoHandler> gamesListening = new HashMap<>();
	public static List<String> gameCreatePMs = new ArrayList<>();
	
	@Override
	public void onPrivateMessage(PrivateMessageEvent e){
		if(Utils.isTwitch(e)) return;
		
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
						Utils.sendDM(Main.api.retrieveUserById(basePM.split("\\|\\|")[0]).complete().openPrivateChannel().complete(), 
										 basePM.split("\\|\\|")[1]);
					}
				}
			}, 2000, 2000);
		}
	}
	
	@Override
	public void onMessage(MessageEvent e){
		boolean twitch = Utils.isTwitch(e);
		
		if(!Utils.verifyChannel(e) && !twitch) return;
		
		String message = e.getMessage();

		if(!twitch){
			Log.logger.log(Level.INFO, "IRC/" + e.getUser().getNick() + ": " + message);
			if(verifyBanchoFeedback(e)) return;
		}
		
		if(message.startsWith("!")) Utils.info(e, null, null, IRCCommand.handleCommand(e, null, null, message.substring(1)));
	}
	
	private boolean verifyBanchoFeedback(MessageEvent e){
		if(e.getUser().getNick().equalsIgnoreCase("BanchoBot"))
			for(String game : gamesListening.keySet())
				if(e.getChannel().getName().equalsIgnoreCase(game)){
					gamesListening.get(game).handleMessage(e.getMessage());
					
					return true;
				}
		
		return false;
	}

	private boolean verifyGameCreationPM(PrivateMessageEvent e){
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
			
			if(gamesListening.containsKey("ASL" + gameName) && gamesListening.get("ASL" + gameName) instanceof AlternativeScoringLobby){
				AlternativeScoringLobby lobby = (AlternativeScoringLobby) gamesListening.get("ASL" + gameName);
				
				gamesListening.remove("ASL" + gameName);
				gamesListening.put("#mp_" + mpLink.split("mp\\/")[1], lobby);
				
				lobby.setMultiChannel("#mp_" + mpLink.split("mp\\/")[1]);
				lobby.lobbyCreated();
				
				return true;
			}
			
			tournamentName = gameName.split(":")[0];

			gameCreatePMs.add(mpLink.split("mp\\/")[1] + "|" + gameName);
			
			Tournament t = Tournament.getTournament(tournamentName);
			if(t == null) return false;
			
			if(gameName.endsWith("TEMP LOBBY")){
				gameCreatePMs.remove(mpLink.split("mp\\/")[1] + "|" + gameName);
				
				long delay = t.getTempLobbyDecayTime() - System.currentTimeMillis();
				
				if(delay > 0){
					new Timer().schedule(new TimerTask(){
						public void run(){
							Main.banchoRegulator.sendPriorityMessage("#mp_" + mpLink.split("mp\\/")[1], "!mp close");
						}
					}, delay);
				}
				
				return true;
			}
			
			int next = 0;
			
			while(t != null){
				Log.logger.log(Level.INFO, "Finding match in tournament: " + t.get("name") + " (Display: " + t.get("displayName") + ")");
				
				if(findMatch(t, gameName, mpLink)) return true;
				else{
					next++;
					t = Tournament.getTournament(tournamentName, next);
				}
			}
		}
		
		return false;
	}
	
	private boolean findMatch(Tournament t, String gameName, String mpLink){
		for(Match match : Match.getMatches(t)){
			if(match == null) continue;
			
			Log.logger.log(Level.INFO, "Match: " + match.getLobbyName() + " | Current: " + gameName);
			
			if(match.getLobbyName().equalsIgnoreCase(gameName) &&
			   match.getGame() != null){
				Log.logger.log(Level.INFO, "Launched match.");
				
				gameCreatePMs.remove(mpLink.split("mp\\/")[1] + "|" + match.getLobbyName());
				gamesListening.put("#mp_" + mpLink.split("mp\\/")[1], match.getGame().getBanchoHandle());
				
				new Thread(new Runnable(){
					public void run(){
						match.getGame().start("#mp_" + mpLink.split("mp\\/")[1], mpLink);
					}
				}).start();
				
				return true;
			}
		}
		
		return false;
	}
	
}
