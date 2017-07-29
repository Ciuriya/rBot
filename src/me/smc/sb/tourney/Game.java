package me.smc.sb.tourney;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;

import me.smc.sb.irccommands.AlertStaffCommand;
import me.smc.sb.irccommands.BanMapCommand;
import me.smc.sb.irccommands.ChangeWarmupModCommand;
import me.smc.sb.irccommands.ContestCommand;
import me.smc.sb.irccommands.PassTurnCommand;
import me.smc.sb.irccommands.RandomCommand;
import me.smc.sb.irccommands.SelectMapCommand;
import me.smc.sb.irccommands.SkipRematchCommand;
import me.smc.sb.irccommands.SkipWarmupCommand;
import me.smc.sb.irccommands.TimeoutCommand;
import me.smc.sb.listeners.IRCChatListener;
import me.smc.sb.tourney.GameState;
import me.smc.sb.utils.Log;
import me.smc.sb.utils.RemotePatyServerUtils;
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
	protected ResultManager resultManager;
	protected GameFeed feed;
	protected Timer messageUpdater;
	protected int roomSize;
	protected int pausesLeft;
	protected int pauseState; // 0 = none, 1 = fTeam, 2 = sTeam, 3 = ok
	protected long lastPickTime;
	protected Timer timeoutTimer;
	protected PlayingTeam nextTeam; // actually currentTeam, I'm just stupid
	protected GameState state;
	
	public Game(Match match){
		this.match = match;
		this.match.setGame(this);
		this.banchoHandle = new BanchoHandler(this);
		this.lobbyManager = new LobbyManager(this);
		this.selectionManager = new SelectionManager(this);
		this.readyManager = new ReadyManager(this);
		this.resultManager = new ResultManager(this);
		this.firstTeam = new PlayingTeam(match.getFirstTeam(), this);
		this.secondTeam = new PlayingTeam(match.getSecondTeam(), this);
		this.pausesLeft = match.getTournament().getInt("pausesAllowed");
		this.state = GameState.WAITING;
		
		banchoHandle.sendMessage("BanchoBot", "!mp make " + match.getLobbyName(), true);
	}
	
	public void start(String multiChannel, String mpLink){
		this.multiChannel = multiChannel;
		this.mpLink = mpLink;
		
		IRCChatListener.gamesListening.put(multiChannel, banchoHandle);
		
		setupGame();
		
		new Timer().schedule(new TimerTask(){
			public void run(){
				if(state.eq(GameState.WAITING)){
					int fPlayers = firstTeam.getCurrentPlayers().size();
					int sPlayers = secondTeam.getCurrentPlayers().size();
					
					if(fPlayers == 0) secondTeam.addPoint();
					if(sPlayers == 0) firstTeam.addPoint();
					if(fPlayers == 0 && sPlayers == 0){
						firstTeam.setPoints(0);
						secondTeam.setPoints(0);
					}
					
					if(fPlayers == 0 || sPlayers == 0) stop();
				}
			}
		}, match.getTournament().getInt("gracePeriodTime") * 1000);
		
		try{
			feed = new GameFeed(this);
			feed.updateTwitch("Waiting for players to join the lobby...");
		}catch(Exception e){}
		
		if(match.getTournament().get("alertDiscord").length() > 0) AlertStaffCommand.gamesAllowedToAlert.add(this);
		
		firstTeam.inviteTeam(0, 60000);
		secondTeam.inviteTeam(0, 60000);
		
		match.setGame(this);
	}
	
	public void setupGame(){
		roomSize = match.getMatchSize();
		
		banchoHandle.sendMessage("!mp lock", true);
		banchoHandle.sendMessage("!mp size " + roomSize, true);
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
				
				firstTeam.roll = -1;
				secondTeam.roll = -1;
				
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
					
					if(!TimeoutCommand.gamesAllowedToTimeout.contains(Game.this) && pausesLeft > 0)
						TimeoutCommand.gamesAllowedToTimeout.add(Game.this);
					
					feed.updateDiscord();
					banchoHandle.sendMessage("This match is reffed by a bot! " + 
											 "If you have suggestions or have found a bug, please report in our [http://discord.gg/0f3XpcqmwGkNseMR discord server] or to Smc. " +
											 "Thank you!", false);
					
					if(messageUpdater != null) messageUpdater.cancel();
					
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
		
		lastPickTime = System.currentTimeMillis();
		
		messageUpdater = new Timer();
		messageUpdater.scheduleAtFixedRate(new TimerTask(){
			public void run(){
				if(waitTime > 0){
					if(System.currentTimeMillis() >= lastPickTime + (waitTime * 1000)) return;
					else{
						for(String message : messages)
							banchoHandle.sendMessage(message, false);
						
						String time = Utils.df(Math.ceil(((lastPickTime / 1000 + waitTime) - (System.currentTimeMillis() / 1000))) / 60, 0);
						banchoHandle.sendMessage(time + " minute" + (Utils.stringToDouble(time) >= 2 ? "s" : "") + " left!" + 
												(pausesLeft > 0 ? " (Use !timeout if you need a small break!)" : ""), false);
					}
				}else 
					for(String message : messages)
						banchoHandle.sendMessage(message, false);
			}
		}, delay * 1000, 60000);
	}
	
	public void handlePause(boolean pause){
		if(pause){
			if(state.eq(GameState.PAUSED)) return;
			
			selectionManager.clearPickTimer();
			SkipRematchCommand.gamesAllowedToSkip.remove(this);
			ContestCommand.gamesAllowedToContest.remove(this);
			resultManager.skipRematchState = 0;
			resultManager.contestState = 0;
			selectionManager.selectionStartTime = 0;
			
			if(messageUpdater != null) messageUpdater.cancel();
			if(selectionManager.lobbyUpdater != null) selectionManager.lobbyUpdater.cancel();
			if(state.eq(GameState.PLAYING)) banchoHandle.sendMessage("!mp abort", true);
			if(state.eq(GameState.PRESTART)) banchoHandle.sendMessage("!mp aborttimer", true);
			
			state = GameState.PAUSED;
			readyManager.switchPlaying(false, true);
		}else{
			if(!state.eq(GameState.PAUSED)) return;
			
			if(timeoutTimer != null){
				timeoutTimer.cancel();
				timeoutTimer = null;
			}
			
			state = GameState.WAITING;
			
			if(firstTeam.getRoll() == -1 || secondTeam.getRoll() == -1) lobbyManager.setupRolling();
			else if(selectionManager.warmupsLeft  > 0) selectionManager.selectWarmups();
			else if(selectionManager.bansLeft > 0) selectionManager.selectBans();
			else if(selectionManager.map != null) readyManager.startReadyWait();
			else selectionManager.selectPicks();
		}
	}
	
	public void timeout(String userName){
		if(state.eq(GameState.PAUSED) || state.eq(GameState.WAITING) || state.eq(GameState.ROLLING) || state.eq(GameState.ENDED) || pausesLeft == 0){
			banchoHandle.sendMessage("You cannot timeout right now!", false);
			
			return;
		}
		
		Player player = lobbyManager.findPlayer(userName);
		PlayingTeam team = lobbyManager.findTeam(player);
		
		if(team != null){
			boolean fTeam = lobbyManager.isOnFirstTeam(player);
			
			switch(pauseState){
				case 0: pauseState = fTeam ? 1 : 2;
						banchoHandle.sendMessage(team.getTeam().getTeamName() + " has asked for a temporary timeout!", false);
						break;
				case 1: if(!fTeam) pauseState = 3; break;
				case 2: if(fTeam) pauseState = 3; break;
				default: return;
			}
			
			if(pauseState == 3){
				int length = match.getTournament().getInt("pauseLength");
				
				pausesLeft--;
				banchoHandle.sendMessage("A " + Utils.toDuration(length) + " timeout has been issued!", false);
				pauseState = 0;
				
				if(pausesLeft == 0)
					TimeoutCommand.gamesAllowedToTimeout.remove(Game.this);
				
				handlePause(true);
				
				if(timeoutTimer != null){
					timeoutTimer.cancel();
					timeoutTimer = null;
				}
				
				timeoutTimer = new Timer();
				timeoutTimer.schedule(new TimerTask(){
					public void run(){			
						handlePause(false);
					}
				}, length * 1000);
			}
		}
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
	
	public ResultManager getResultManager(){
		return resultManager;
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
	
	public String getMpLink(){
		return mpLink;
	}
	
	public void stop(){
		selectionManager.clearPickTimer();
		
		if(multiChannel != null)
			match.getTournament().getTwitchHandler().stopStreaming(this);
		
		RandomCommand.waitingForRolls.remove(match.getFirstTeam());
		RandomCommand.waitingForRolls.remove(match.getSecondTeam());
		SelectMapCommand.pickingTeams.remove(match.getFirstTeam());
		SelectMapCommand.pickingTeams.remove(match.getSecondTeam());
		PassTurnCommand.passingTeams.remove(match.getFirstTeam());
		PassTurnCommand.passingTeams.remove(match.getSecondTeam());
		ChangeWarmupModCommand.gamesAllowedToChangeMod.remove(this);
		BanMapCommand.banningTeams.remove(match.getFirstTeam());
		BanMapCommand.banningTeams.remove(match.getSecondTeam());
		SkipRematchCommand.gamesAllowedToSkip.remove(this);
		SkipWarmupCommand.gamesAllowedToSkip.remove(this);
		ContestCommand.gamesAllowedToContest.remove(this);
		AlertStaffCommand.gamesAllowedToAlert.remove(this);
		IRCChatListener.gamesListening.remove(multiChannel);
		
		Team winningTeam = firstTeam.getPoints() > secondTeam.getPoints() ? match.getFirstTeam() : match.getSecondTeam();
		
		if(firstTeam.getPoints() != secondTeam.getPoints()){
			Team losingTeam = winningTeam.getTeamName().equalsIgnoreCase(match.getFirstTeam().getTeamName()) ? match.getSecondTeam() : match.getFirstTeam();
			
			if(match.getTournament().getConditionalTeams().size() > 0){
				for(String conditional : new HashMap<String, Match>(match.getTournament().getConditionalTeams()).keySet()){
					int conditionalNum = Utils.stringToInt(conditional.split(" ")[1]);
					
					Log.logger.log(Level.INFO, "Checking conditional match... (" + conditional + ")");
					if(match.getMatchNum() != conditionalNum && !match.getServerID().equalsIgnoreCase(conditional.split(" ")[1])) continue;
					
					Log.logger.log(Level.INFO, "Winning team: " + winningTeam.getTeamName() + " | Losing team: " + losingTeam.getTeamName());
					
					Team cTeam = conditional.split(" ")[0].equalsIgnoreCase("winner") ? winningTeam : losingTeam;
					Match conditionalMatch = match.getTournament().getConditionalTeams().get(conditional);
					boolean fTeam = Utils.stringToInt(conditional.split(" ")[2]) == 1;
					
					conditionalMatch.setTeams(fTeam ? cTeam : conditionalMatch.getFirstTeam(), 
											  !fTeam ? cTeam : conditionalMatch.getSecondTeam());
					conditionalMatch.save(false);
					
					Log.logger.log(Level.INFO, "Set conditional team " + cTeam.getTeamName() + " to match #" + conditionalMatch.getMatchNum());
				}
			}
		}
		
		if(match.getTournament().getBool("usingTourneyServer") && match.getServerID().length() > 0 && winningTeam.getServerTeamID() != 0){
			new Thread(new Runnable(){
				public void run(){
					RemotePatyServerUtils.setMPLinkAndWinner(mpLink, winningTeam, match.getServerID(), 
															 match.getTournament().get("name"), firstTeam.getPoints(), 
															 secondTeam.getPoints());
				}
			}).start();
		}
		
		String shortGameEndMsg = match.getFirstTeam().getTeamName() + " (" + firstTeam.getPoints() + 
				  				 ") vs (" + secondTeam.getPoints() + ") " + match.getSecondTeam().getTeamName() + 
				  				 " - " + mpLink;
		
		banchoHandle.sendMessage("!mp close", false);
		
		for(String admin : match.getMatchAdmins())
			banchoHandle.sendMessage(admin.replaceAll(" ", "_"), shortGameEndMsg, false);
		
		for(String admin : match.getTournament().getStringList("tournament-admins"))
			banchoHandle.sendMessage(admin.replaceAll(" ", "_"), shortGameEndMsg, false);
		
		state = GameState.ENDED;
		Log.logger.log(Level.INFO, shortGameEndMsg);
		
		if(messageUpdater != null){
			messageUpdater.cancel();
			messageUpdater = null;
		}
		
		if(selectionManager.lobbyUpdater != null){
			selectionManager.lobbyUpdater.cancel();
			selectionManager.lobbyUpdater = null;
		}
		
		if(System.currentTimeMillis() > match.getTournament().getTempLobbyDecayTime()){
			match.getTournament().setTempLobbyDecayTime();
			banchoHandle.sendMessage("BanchoBot", "!mp make " + match.getTournament().get("displayName") + ": TEMP LOBBY", true);
		}
		
		try{
			feed.updateDiscord().join();
		}catch (InterruptedException e){}
		
		firstTeam = null;
		secondTeam = null;
		banchoHandle = null;
		lobbyManager = null;
		selectionManager = null;
		readyManager = null;
		resultManager = null;
		feed = null;
		nextTeam = null;
		
		match.setGame(null);
		Match.removeMatch(match.getTournament(), match.getMatchNum());
		match = null;
	}
}
