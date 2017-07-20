package me.smc.sb.tourney;

import me.smc.sb.main.Main;
import me.smc.sb.tourney.GameState;

public class BanchoHandler{
	
	private Game game;

	public BanchoHandler(Game game){
		this.game = game;
	}
	
	public void sendMessage(String message, boolean priority){
		sendMessage(game.multiChannel, message, priority);
	}
	
	public void sendMessage(String channel, String message, boolean priority){
		if(priority) Main.banchoRegulator.sendPriorityMessage(channel, message);
		else Main.banchoRegulator.sendMessage(channel, message);
	}
	
	public void handleMessage(String message){
		if(message.contains("joined in")) game.lobbyManager.join(message);
		else if(message.contains("left the game.")) game.lobbyManager.leave(message.replace(" left the game.", "").replaceAll(" ", "_"));
		else if(message.startsWith("Beatmap: ")) game.selectionManager.updateMap(message.split(" ")[1]); // if the map changes without notice
		else if(message.contains("All players are ready") && game.state.eq(GameState.READYING)) game.readyManager.playersReady();
		else if(message.startsWith("Slot ") && game.state.eq(GameState.VERIFYING)) game.readyManager.verifyPlayer(message);
		else if(message.contains("The match has started!")) game.readyManager.matchStarted();
		else if(message.contains("The match has finished!")) game.resultManager.analyseResults();
		else if(message.contains("finished playing")) game.resultManager.addResult(message);
	}
	
	public void kickPlayer(String player, String reason){
		if(reason.length() > 0) game.banchoHandle.sendMessage(reason, false); 
		game.banchoHandle.sendMessage("!mp kick " + player.replaceAll(" ", "_"), true); 
	}
}
