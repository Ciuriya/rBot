package me.smc.sb.tourney;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import me.smc.sb.irccommands.AlertStaffCommand;
import me.smc.sb.irccommands.PassTurnCommand;
import me.smc.sb.irccommands.RandomCommand;
import me.smc.sb.listeners.IRCChatListener;
import me.smc.sb.utils.Utils;

public class Game{

	public Match match;
	protected String multiChannel;
	protected String mpLink;
	protected PlayingTeam firstTeam;
	protected PlayingTeam secondTeam;
	protected BanchoHandler banchoHandle;
	protected LobbyManager lobbyManager;
	protected SelectionManager selectionManager;
	protected ReadyManager readyManager;
	protected GameFeed feed;
	protected Timer messageUpdater;
	protected int roomSize;
	protected long lastPickTime;
	protected PlayingTeam nextTeam; // actually currentTeam, I'm just stupid
	protected GameState state;
	
	public Game(Match match){
		this.match = match;
		this.match.setGame(this);
		this.banchoHandle = new BanchoHandler(this);
		this.lobbyManager = new LobbyManager(this);
		this.selectionManager = new SelectionManager(this);
		this.readyManager = new ReadyManager(this);
		this.firstTeam = new PlayingTeam(match.getFirstTeam(), this);
		this.secondTeam = new PlayingTeam(match.getSecondTeam(), this);
		this.state = GameState.WAITING;
		
		banchoHandle.sendMessage("BanchoBot", "!mp make " + match.getLobbyName(), true);
	}
	
	public void start(String multiChannel, String mpLink){
		this.multiChannel = multiChannel;
		this.mpLink = mpLink;
		
		IRCChatListener.gamesListening.put(multiChannel, banchoHandle);
		
		setupGame();
		
		firstTeam.inviteTeam(0, 60000);
		secondTeam.inviteTeam(0, 60000);
		
		new Timer().schedule(new TimerTask(){
			public void run(){
				if(state.eq(GameState.WAITING)){
					int fPlayers = firstTeam.getCurrentPlayers().size();
					int sPlayers = secondTeam.getCurrentPlayers().size();
					
					if(fPlayers == 0) firstTeam.addPoint();
					else if(sPlayers == 0) secondTeam.addPoint();
					
					if(fPlayers == 0 || sPlayers == 0) stop();
				}
			}
		}, match.getTournament().getInt("gracePeriodTime") * 1000);
		
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
	
	public void checkRolls(){
		int first = firstTeam.getRoll();
		int second = secondTeam.getRoll();
		
		if(first != -1 && second != -1){
			if(first == second){
				banchoHandle.sendMessage("Rolls were equal! Please reroll using !random", false);
				
				RandomCommand.waitingForRolls.add(firstTeam);
				RandomCommand.waitingForRolls.add(secondTeam);
				
				return;
			}
			
			if(first > second) nextTeam = firstTeam;
			else nextTeam = secondTeam;
			
			PassTurnCommand.passingTeams.add(nextTeam);
			
			banchoHandle.sendMessage(nextTeam.getTeam().getTeamName() + 
									 ", you can use !pass within the next 20 seconds to let the other " + getTeamIndicator() + " start instead!",
									 false);
	
			if(messageUpdater != null) messageUpdater.cancel();
			
			final PlayingTeam rollWinTeam = nextTeam;
			
			Timer t = new Timer();
			t.schedule(new TimerTask(){
				public void run(){
					PassTurnCommand.passingTeams.remove(rollWinTeam);
					
					feed.updateDiscord();
					banchoHandle.sendMessage("This match is reffed by a bot! " + 
											 "If you have suggestions or have found a bug, please report in our [http://discord.gg/0f3XpcqmwGkNseMR discord server] or to Smc. " +
											 "Thank you!", false);
					
					selectionManager.selectWarmups();
				}
			}, 20000);
		}
	}
	
	protected void messageUpdater(String...messages){
		messageUpdater(0, 0, messages);
	}
	
	protected void messageUpdater(int delay, long waitTime, String...messages){
		if(messageUpdater != null) messageUpdater.cancel();
		if(messages == null || messages.length <= 0) return;
		
		messageUpdater = new Timer();
		messageUpdater.scheduleAtFixedRate(new TimerTask(){
			public void run(){
				if(waitTime > 0){
					if(System.currentTimeMillis() >= lastPickTime + (waitTime * 1000)) return;
					else{
						for(String message : messages)
							banchoHandle.sendMessage(message, false);
						
						String time = Utils.df(Math.ceil(((lastPickTime / 1000 + waitTime) - (System.currentTimeMillis() / 1000))) / 60, 0);
						banchoHandle.sendMessage(time + " minute" + (Utils.stringToDouble(time) >= 2 ? "s" : "") + " left!", false);
					}
				}else 
					for(String message : messages)
						banchoHandle.sendMessage(message, false);
			}
		}, delay * 1000, 60000);
	}
	
	public PlayingTeam getFirstTeam(){
		return firstTeam;
	}
	
	public PlayingTeam getSecondTeam(){
		return secondTeam;
	}
	
	public PlayingTeam getOppositeTeam(PlayingTeam team){
		if(team.getTeam().getTeamName().equalsIgnoreCase(firstTeam.getTeam().getTeamName()))
			return secondTeam;
		else return firstTeam;
	}
	
	public void switchNextTeam(){
		if(nextTeam.getTeam().getTeamName().equalsIgnoreCase(firstTeam.getTeam().getTeamName()))
			nextTeam = secondTeam;
		else nextTeam = firstTeam;
	}
	
	public PlayingTeam getNextTeam(){
		return nextTeam;
	}
	
	// team for team tourney, player for solo tourney
	public String getTeamIndicator(){
		if(match.getTournament().getInt("type") == 0) return "team";
		else return "player";
	}
	
	public BanchoHandler getBanchoHandle(){
		return banchoHandle;
	}
	
	public LobbyManager getLobbyManager(){
		return lobbyManager;
	}
	
	public SelectionManager getSelectionManager(){
		return selectionManager;
	}
	
	public ReadyManager getReadyManager(){
		return readyManager;
	}
	
	public GameFeed getGameFeed(){
		return feed;
	}
	
	public GameState getState(){
		return state;
	}
	
	public int getMpNum(){
		return Utils.stringToInt(multiChannel.replace("#mp_", ""));
	}
	
	public void stop(){
		
	}
}
