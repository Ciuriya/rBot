package me.smc.sb.tourney;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import org.json.JSONArray;
import org.json.JSONObject;

import me.smc.sb.listeners.IRCChatListener;
import me.smc.sb.main.Main;
import me.smc.sb.scoringstrategies.ScoringStrategy;
import me.smc.sb.tracking.Mods;
import me.smc.sb.tracking.OsuMultiRequest;
import me.smc.sb.tracking.OsuRequest;
import me.smc.sb.tracking.TrackedPlay;
import me.smc.sb.utils.Utils;

public class AlternativeScoringLobby extends BanchoHandler{
	
	private String creator;
	private String multiChannel;
	private ScoringStrategy strategy;
	private List<String> results;
	private java.util.Map<String, Integer> playerMods;
	private java.util.Map<String, Integer> playerSlots;
	private boolean verifying;
	private String currentMap;
	private int playerCount;
	
	public AlternativeScoringLobby(){
		super(null);
		
		results = new ArrayList<>();
		playerMods = new HashMap<>();
		playerSlots = new HashMap<>();
		verifying = false;
		currentMap = "";
	}
	
	@Override
	public void sendMessage(String message, boolean priority){
		sendMessage(multiChannel, message, priority);
	}

	@Override
	public void handleMessage(String message){
		if(message.contains("The match has started!")) matchStarted();
		else if(message.contains("The match has finished!")) matchEnded();
		else if(message.contains("All players are ready")) sendMessage("!mp start 5", true);
		else if(message.contains("finished playing")) results.add(message);
		else if(message.startsWith("Beatmap: ")) updateMap(message.split(" ")[1]);
		else if(message.startsWith("Slot ") && verifying) verifyPlayer(message);
		else if(message.startsWith("Players:")){
			int pc = Utils.stringToInt(message.split(" ")[1]);
			
			if(pc != -1) playerCount = pc;
		}else if(message.contains("joined in")){
			if(creator.equalsIgnoreCase(message.split(" joined in")[0]))
				sendMessage("!mp host " + creator, true);
		}else if(message.contains("left the game.")) playerLeft();
	}
	
	public void start(String creator, String name, String scoringStrategy){
		this.creator = creator.replaceAll(" ", "_");
		strategy = ScoringStrategy.findStrategy(scoringStrategy);
		
		IRCChatListener.gamesListening.put("ASL" + name, this);
		sendMessage("BanchoBot", "!mp make " + name, true);
	}
	
	public void lobbyCreated(){
		sendMessage(multiChannel, "!mp set 0 0", true);
		sendMessage(multiChannel, "!mp addref " + creator, true);
		sendMessage(multiChannel, "!mp invite " + creator, true);
	}
	
	public void matchStarted(){
		verifying = true;
		results.clear();
		playerMods.clear();
		playerSlots.clear();
		
		sendMessage(multiChannel, "!mp settings", true);
	}
	
	public void verifyPlayer(String message){
		int modList = 0;
		int slot = Utils.stringToInt(message.split(" ")[1]);
		
		if(message.contains("[") && message.contains(" / ")){
			String[] sBracketSplit = message.split("\\[");
			String fullEndSection = sBracketSplit[sBracketSplit.length - 1].split("\\]")[0];
			
			if(fullEndSection.startsWith("Host \\/")) fullEndSection = fullEndSection.replace("Host / ", "");
			
			String mods = fullEndSection.contains("/") ? fullEndSection.split("\\/")[1].substring(1) : "";
			
			if(mods.length() > 0)
				for(String mod : mods.split(", ")){
					modList += Mods.getMods(mod);
				}
		}
			
		message = Utils.removeExcessiveSpaces(message);
		
		String[] spaceSplit = message.split(" ");
		String playerName = "";
		boolean start = false;
		
		for(int i = 0; i < spaceSplit.length; i++){
			if(spaceSplit[i].startsWith("[Team") || spaceSplit[i].startsWith("[Host"))
				break;
			
			if(spaceSplit[i].contains("osu.ppy.sh")){
				start = true;
				continue;
			}
			
			if(start) playerName += spaceSplit[i] + " ";
		}
		
		playerName = playerName.substring(0, playerName.length() - 1);

		playerSlots.put(playerName, slot);
		playerMods.put(playerName, modList);
	}
	
	public void updateMap(String map){
		currentMap = map;
	}
	
	public void matchEnded(){
		verifying = false;
		
		new Timer().schedule(new TimerTask(){
			public void run(){
				OsuRequest multiRequest = new OsuMultiRequest(multiChannel.split("_")[1]);
				Object multiMatchObj = Main.hybridRegulator.sendRequest(multiRequest, 15000, true);
				JSONArray multiMatch = null;
				List<String> players = new ArrayList<>();
				java.util.Map<Integer, JSONObject> plays = new HashMap<>();
				int globalMods = 0;
				long bestScore = 0;
				String bestPlayer = "";
				
				if(multiMatchObj != null && multiMatchObj instanceof JSONArray){
					multiMatch = (JSONArray) multiMatchObj;
					plays = ResultManager.getPlays(multiMatch, Utils.stringToInt(currentMap.split("\\/b\\/")[1]));
					globalMods = multiMatch.getJSONObject(multiMatch.length() - 1).getInt("mods");
				}
				
				for(String result : results){
					String playerName = result.substring(0, result.indexOf(" finished"));
					
					if(!players.contains(playerName)){
						players.add(playerName);
						
						long score = Utils.stringToInt(result.split("Score: ")[1].split(",")[0]);
						JSONObject play = null;
						int slot = playerSlots.get(playerName) - 1;
						
						if(plays.containsKey(slot)){
							play = plays.get(slot);
							play.put("enabled_mods", playerMods.get(playerName) + globalMods);
						}
						
						if(play != null){
							score = strategy.calculateScore(playerName, new TrackedPlay(play, 0), false, AlternativeScoringLobby.this);
							
							if(score > bestScore){
								bestScore = score;
								bestPlayer = playerName;
							}
						}
					}
				}
				
				if(bestScore > 0) sendMessage(bestPlayer + " wins this round!", false);
			}
		}, 10000);
	}
	
	public void playerLeft(){
		playerCount--;
		
		if(playerCount <= 0) close();
	}
	
	public void setMultiChannel(String multiChannel){
		this.multiChannel = multiChannel;
	}
	
	public void close(){
		IRCChatListener.gamesListening.remove(multiChannel);
		sendMessage(multiChannel, "!mp close", true);
	}
	
}
