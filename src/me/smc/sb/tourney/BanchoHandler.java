package me.smc.sb.tourney;

import me.smc.sb.main.Main;

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
	}
	
	public void kickPlayer(String player, String reason){
		if(reason.length() > 0) game.banchoHandle.sendMessage(reason, false); 
		game.banchoHandle.sendMessage("!mp kick " + player.replaceAll(" ", "_"), true); 
	}
}
