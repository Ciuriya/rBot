package me.smc.sb.tourney;

import java.util.logging.Level;

import me.smc.sb.main.Main;
import me.smc.sb.utils.Log;

public class Game{

	public Match match;
	private String multiChannel;
	private String mpLink;
	
	public Game(Match match){
		this.match = match;
		
		this.match.setGame(this);
	}
	
	public void setupLobby(){
		try{
			Main.ircBot.sendIRC().joinChannel("BanchoBot");
		}catch(Exception ex){
			Log.logger.log(Level.INFO, "Could not talk to BanchoBot!");
		}
		
		Main.banchoRegulator.sendPriorityMessage("BanchoBot", "!mp make " + match.getLobbyName());
	}
	
	public void start(String multiChannel, String mpLink){
		this.multiChannel = multiChannel;
		this.mpLink = mpLink;
		
		//finish this lmao (have fun scrub)
	}
	
}
