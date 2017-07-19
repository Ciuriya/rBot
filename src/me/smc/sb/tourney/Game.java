package me.smc.sb.tourney;

import java.util.ArrayList;
import java.util.List;

import me.smc.sb.irccommands.AlertStaffCommand;
import me.smc.sb.listeners.IRCChatListener;
import me.smc.sb.utils.Utils;

public class Game{

	public Match match;
	protected String multiChannel;
	protected String mpLink;
	protected PlayingTeam firstTeam;
	protected PlayingTeam secondTeam;
	protected BanchoHandler banchoHandle;
	protected GameFeed feed;
	protected int roomSize;
	protected boolean finished;
	
	public Game(Match match){
		this.match = match;
		this.match.setGame(this);
		this.banchoHandle = new BanchoHandler(this);
		this.firstTeam = new PlayingTeam(match.getFirstTeam(), this);
		this.secondTeam = new PlayingTeam(match.getSecondTeam(), this);
		this.finished = false;
		
		banchoHandle.sendMessage("BanchoBot", "!mp make " + match.getLobbyName(), true);
	}
	
	public void start(String multiChannel, String mpLink){
		this.multiChannel = multiChannel;
		this.mpLink = mpLink;
		
		IRCChatListener.gamesListening.put(multiChannel, banchoHandle);
		
		setupGame();
		
		firstTeam.inviteTeam(0, 60000);
		secondTeam.inviteTeam(0, 60000);
		
		feed = new GameFeed(this);
		feed.updateTwitch("Waiting for players to join the lobby...");
		
		if(match.getTournament().get("alertDiscord").length() > 0) AlertStaffCommand.gamesAllowedToAlert.add(this);
	}
	
	public void setupGame(){
		roomSize = match.getMatchSize();
		
		banchoHandle.sendMessage("!mp lock", true);
		banchoHandle.sendMessage("!mp size" + roomSize, true);
		banchoHandle.sendMessage("!mp set 2 " + (match.getTournament().getBool("scoreV2") ? "4" : "0"), true);
		
		String admins = "";
		
		if(!match.getMatchAdmins().isEmpty()){
			for(String admin : match.getMatchAdmins())
				admins += admin.replaceAll(" ", "_") + ", ";
		}
		
		ArrayList<String> tourneyAdmins = match.getTournament().getStringList("tournament-admins");
		
		if(!tourneyAdmins.isEmpty())
			for(String admin : tourneyAdmins)
				admins += admin.replaceAll(" ", "_") + ", ";
		
		admins = admins.substring(0, admins.length() - 2);
		
		if(admins.length() > 0)
			banchoHandle.sendMessage("!mp addref " + admins, true);
	}
	
	public List<Player> getCurrentPlayers(){
		List<Player> current = new ArrayList<>();
		
		current.addAll(firstTeam.getCurrentPlayers());
		current.addAll(secondTeam.getCurrentPlayers());
		
		return current;
	}
	
	public boolean verify(String playerName){
		return firstTeam.getTeam().has(playerName.replaceAll(" ", "_")) ||
			   secondTeam.getTeam().has(playerName.replaceAll(" ", "_"));
	}
	
	public int getMpNum(){
		return Utils.stringToInt(multiChannel.replace("#mp_", ""));
	}
}
