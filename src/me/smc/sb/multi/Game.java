package me.smc.sb.multi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.logging.Level;

import org.json.JSONObject;
import org.pircbotx.Channel;

import me.smc.sb.irccommands.AlertStaffCommand;
import me.smc.sb.irccommands.BanMapCommand;
import me.smc.sb.irccommands.ChangeWarmupModCommand;
import me.smc.sb.irccommands.ContestCommand;
import me.smc.sb.irccommands.InvitePlayerCommand;
import me.smc.sb.irccommands.JoinMatchCommand;
import me.smc.sb.irccommands.PassTurnCommand;
import me.smc.sb.irccommands.RandomCommand;
import me.smc.sb.irccommands.SelectMapCommand;
import me.smc.sb.irccommands.SkipRematchCommand;
import me.smc.sb.irccommands.SkipWarmupCommand;
import me.smc.sb.listeners.IRCChatListener;
import me.smc.sb.main.Main;
import me.smc.sb.utils.Log;
import me.smc.sb.utils.RemotePatyServerUtils;
import me.smc.sb.utils.Utils;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageChannel;

public abstract class Game{

	public Match match;
	protected String multiChannel;
	protected String mpLink;
	protected String mapMod = "";
	protected int waitingForCaptains = 2;
	protected int fTeamPoints = 0, sTeamPoints = 0;
	protected int warmupsLeft = 2;
	protected int rollsLeft = 2;
	protected int previousRoll = 0;
	protected int playersChecked = 0;
	protected int roomSize = 0;
	protected int skipRematchState = 0; //0 = none, 1 = f skipped, 2 = s skipped, 3 = both skip
	protected int contestState = 0; //^
	protected int expectedPlayers = 0;
	protected int lastModPickedFTeam = 0;
	protected int lastModPickedSTeam = 0;
	protected int mapLength = 0;
	protected long lastPickTime = 0;
	protected long mapSelectedTime = 0;
	protected boolean mapSelected = false;
	protected boolean fTeamFirst = true;
	protected boolean validMods = true;
	protected boolean allowedToStart = true;
	public boolean streamed = false;
	protected boolean lastRematchFTeam = false;
	protected GameState state;
	protected int rematchesLeftFTeam = 0;
	protected int rematchesLeftSTeam = 0;
	protected List<String> invitesSent;
	protected List<String> playersInRoom;
	protected List<String> captains;
	protected List<String> joinQueue;
	protected java.util.Map<Integer, String> hijackedSlots;
	protected List<Map> bans;
	protected List<String> bansWithNames;
	protected List<Map> mapsPicked;
	protected List<Player> playersSwapped;
	protected java.util.Map<Player, Integer> verifyingSlots;
	protected LinkedList<String> banchoFeedback;
	protected List<Player> playersRankChecked;
	protected Team selectingTeam;
	protected Team banningTeam;
	protected Team lastWinner;
	protected Map map;
	protected Map previousMap;
	protected Map lastPlayedMap;
	protected Timer mapUpdater;
	protected Timer messageUpdater;
	protected List<Timer> pickTimers;
	protected Message discordResultMessage;
	
	public static Game createGame(Match match){
		if(match.getTournament().getTournamentType() == 0)
			return new TeamGame(match);
		else return new SoloGame(match);
	}
	
	public Game(Match match){
		this.match = match;
		this.multiChannel = "";
		this.mpLink = "";
		this.invitesSent = new ArrayList<>();
		this.playersInRoom = new ArrayList<>();
		this.banchoFeedback = new LinkedList<>();
		this.bans = new ArrayList<>();
		this.bansWithNames = new ArrayList<>();
		this.mapsPicked = new ArrayList<>();
		this.captains = new ArrayList<>();
		this.joinQueue = new ArrayList<>();
		this.playersSwapped = new ArrayList<>();
		this.hijackedSlots = new HashMap<>();
		this.verifyingSlots = new HashMap<>();
		this.pickTimers = new ArrayList<>();
		this.playersRankChecked = new ArrayList<>();
		this.state = GameState.WAITING;
		this.match.setGame(this);
		this.rematchesLeftFTeam = match.getTournament().getRematchesAllowed();
		this.rematchesLeftSTeam = match.getTournament().getRematchesAllowed();
		
		setupLobby();
	}
	
	public void start(String multiChannel, String mpLink){
		this.multiChannel = multiChannel;
		this.mpLink = mpLink;
		
		IRCChatListener.gameCreatePMs.remove(multiChannel.replace("#mp_", "") + "|" + match.getLobbyName());
		IRCChatListener.gamesListening.put(multiChannel, this);
		
		setupGame();
		
		initialInvite();
		
		String resultDiscord = match.getTournament().getResultDiscord();
		
		String gameMessage = buildDiscordResultMessage(false) + "```";
		
		if(resultDiscord != null){
			MessageChannel channel = null;
		
			if(Main.api.getTextChannelById(resultDiscord) != null){
				channel = Main.api.getTextChannelById(resultDiscord);
			}else channel = Main.api.getPrivateChannelById(resultDiscord);
			
			if(channel != null){
				Utils.fakeInfo(channel, gameMessage);
				
				channel.sendMessage(gameMessage).queue(new Consumer<Message>(){
					@Override
					public void accept(Message t){
						discordResultMessage = t;
					}
				});
			}
		}
		
		updateTwitch("Waiting for players to join the lobby...");
		
		AlertStaffCommand.gamesAllowedToAlert.add(this);
	}
	
	public void setupGame(){
		sendPriorityMessage("!mp lock");
		sendPriorityMessage("!mp size " + match.getPlayers());
		sendPriorityMessage("!mp set 2 " + (match.getTournament().isScoreV2() ? "4" : "0"));
		
		String admins = "";
		
		if(!match.getMatchAdmins().isEmpty()){
			for(String admin : match.getMatchAdmins())
				admins += admin.replaceAll(" ", "_") + ", ";
		}
		
		if(!match.getTournament().getMatchAdmins().isEmpty())
			for(String admin : match.getTournament().getMatchAdmins())
				admins += admin.replaceAll(" ", "_") + ", ";
		
		admins = admins.substring(0, admins.length() - 2);
		
		if(admins.length() > 0)
			sendPriorityMessage("!mp addref " + admins);
		
		roomSize = match.getPlayers();
	}
	
	public abstract void initialInvite();
	
	public boolean isWarmingUp(){
		return warmupsLeft != 0;
	}
	
	public PickStrategy getPickStrategy(){
		return match.getTournament().getPickStrategy();
	}
	
	protected void messageUpdater(String...messages){
		messageUpdater(0, false, messages);
	}
	
	protected void messageUpdater(int delay, boolean usePickTime, String...messages){
		if(messageUpdater != null) messageUpdater.cancel();
		if(messages == null || messages.length <= 0) return;
		
		messageUpdater = new Timer();
		messageUpdater.scheduleAtFixedRate(new TimerTask(){
			public void run(){
				if(usePickTime && getPickWaitTime() > 0){
					if(System.currentTimeMillis() >= lastPickTime + (getPickWaitTime() * 1000)) return;
					else{
						for(String message : messages)
							sendMessage(message);
						
						String time = Utils.df(Math.ceil(((lastPickTime / 1000 + getPickWaitTime()) - (System.currentTimeMillis() / 1000))) / 60, 0);
						sendMessage(time + " minute" + (Utils.stringToDouble(time) >= 2 ? "s" : "") + " left!");
					}
				}else 
					for(String message : messages)
						sendMessage(message);
			}
		}, delay * 1000, 60000);
	}
	
	public int getPickWaitTime(){
		if(banningTeam != null) return match.getTournament().getBanWaitTime();
		else if(mapSelected) return match.getTournament().getReadyWaitTime();
		
		return match.getTournament().getPickWaitTime();
	}
	
	public void setScores(int fTeamScore, int sTeamScore){
		this.fTeamPoints = fTeamScore;
		this.sTeamPoints = sTeamScore;
		
		updateScores(false);
	}
	
	public abstract void allowTeamInvites();
	
	protected void mapSelection(int part){
		mapSelectedTime = 0;
		mapMod = "";
		mapLength = 0;
		
		switch(part){
			case 1:
				allowTeamInvites();
				
				RandomCommand.waitingForRolls.put(match.getFirstTeam(), this);
				RandomCommand.waitingForRolls.put(match.getSecondTeam(), this);
				break;
			case 2:
				if(state.eq(GameState.PAUSED)) return;
				
				if(selectingTeam != null) SelectMapCommand.pickingTeams.remove(selectingTeam);
				
				PassTurnCommand.passingTeams.remove(fTeamFirst ? match.getFirstTeam() : match.getSecondTeam());

				if(match.getTournament().isSkippingWarmups()){
					warmupsLeft = 0;
					mapSelection(3);
					return;
				}
				
				selectingTeam = findNextTeamToPick();
				map = null;
				
				SelectMapCommand.pickingTeams.put(selectingTeam, this);
				
				if(!ChangeWarmupModCommand.gamesAllowedToChangeMod.contains(this))
					ChangeWarmupModCommand.gamesAllowedToChangeMod.add(this);
				
				if(!SkipWarmupCommand.gamesAllowedToSkip.contains(this))
					SkipWarmupCommand.gamesAllowedToSkip.add(this);
				
				startMapUpdater();
				
				pickTimer(false);
				messageUpdater(0, true, selectingTeam.getTeamName() + ", please pick a warmup map using !select <map url> +MOD, the mod is optional and only takes DT/HT", 
							  "If the lobby order is not correct, do not worry about it, it will be fixed.",
							  "To skip this warmup, please use !warmupskip");
				break;
			case 3: 
				if(state.eq(GameState.PAUSED)) return;
				
				if(getPickStrategy() instanceof ModPickStrategy){
					mapSelection(4);
					return;
				}
				
				if(selectingTeam != null) SelectMapCommand.pickingTeams.remove(selectingTeam);
				clearPickTimers();
				
				ChangeWarmupModCommand.gamesAllowedToChangeMod.remove(this);
				SkipWarmupCommand.gamesAllowedToSkip.remove(this);
				
				banningTeam = findNextTeamToBan();
				
				if(bans.size() >= 4){
					updateResults(false);
					mapSelection(4);
					break;
				}

				BanMapCommand.banningTeams.put(banningTeam, this);

				pickTimer(false);
				messageUpdater(0, true, banningTeam.getTeamName() + ", please ban a map using !ban <map url> or !ban <map #>" +
						   (match.getMapPool().getSheetUrl().length() > 0 ? " [" + match.getMapPool().getSheetUrl() + 
						   " You can find the maps here]" : ""));
				
				if(bans.size() > 0) updateResults(false);
				
				break;
			case 4:
				if(state.eq(GameState.PAUSED)) return;
				
				if(selectingTeam != null) SelectMapCommand.pickingTeams.remove(selectingTeam);
				if(banningTeam != null) BanMapCommand.banningTeams.remove(banningTeam);
				
				ChangeWarmupModCommand.gamesAllowedToChangeMod.remove(this);
				SkipWarmupCommand.gamesAllowedToSkip.remove(this);
				
				clearPickTimers();
				
				banningTeam = null;
				selectingTeam = findNextTeamToPick();
				map = null;
				
				SelectMapCommand.pickingTeams.put(selectingTeam, this);
				
				startMapUpdater();
				
				pickTimer(false);
				
				String selectUsage = "!select <map url> or !select <map #>";
				
				if(getPickStrategy() instanceof ModPickStrategy) selectUsage = "!select <mod>";
				
				messageUpdater(0, true, selectingTeam.getTeamName() + ", please pick a map using " + selectUsage +
						   (match.getMapPool().getSheetUrl().length() > 0 ? " [" + match.getMapPool().getSheetUrl() + 
						   " You can find the maps here]" : ""), 
						   "If the lobby order is not correct, do not worry about it, it will be fixed.");
				break;
			default: break;
		}
	}
	
	private void pickTimer(boolean returning){
		if(state.eq(GameState.PLAYING)) return;
		
		lastPickTime = System.currentTimeMillis();
		
		if(returning){
			if(warmupsLeft > 0) messageUpdater(0, true, selectingTeam.getTeamName() + ", please pick a warmup map using !select <map url>");
			else if(banningTeam != null) 
				messageUpdater(0, true, banningTeam.getTeamName() + ", please ban a map using !ban <map url> or !ban <map #>" +
							  (match.getMapPool().getSheetUrl().length() > 0 ? " [" + match.getMapPool().getSheetUrl() + 
							  " You can find the maps here]" : ""));
			else if(mapSelected) return;
			else{
				String selectUsage = "!select <map url> or !select <map #>";
				
				if(getPickStrategy() instanceof ModPickStrategy) selectUsage = "!select <mod>";
				
				messageUpdater(0, true, selectingTeam.getTeamName() + ", please pick a map using " + selectUsage +
						   (match.getMapPool().getSheetUrl().length() > 0 ? " [" + match.getMapPool().getSheetUrl() + 
						   " You can find the maps here]" : ""), 
						   "If the lobby order is not correct, do not worry about it, it will be fixed.");
			}
		}
		
		int pickTime = getPickWaitTime();
		
		if(pickTime <= 0) return;
		
		Timer t = new Timer();
		t.schedule(new TimerTask(){
			public void run(){
				if(match != null && state != GameState.PLAYING){
					messageUpdater.cancel();
					
					if(banningTeam != null){
						bans.add(new Map("https://osu.ppy.sh/b/1", 1, null));
						
						BanMapCommand.banningTeams.remove(banningTeam);
						
						mapSelection(3);
						return;
					}
					
					if(mapSelected){
						sendMessage("Attempting to force start...");

						readyCheck(true);
						return;
					}
					
					if(warmupsLeft > 0){
						warmupsLeft--;
						mapUpdater.cancel();
						
						sendMessage(selectingTeam.getTeamName() + " has taken too long to pick a warmup, they will not get a warmup!");
						
						if(warmupsLeft == 0) mapSelection(3);
						else mapSelection(2);
						return;
					}
					
					sendMessage(selectingTeam.getTeamName() + " has taken too long to select a map!");
					
					fTeamFirst = !fTeamFirst;
					SelectMapCommand.pickingTeams.remove(selectingTeam);
					selectingTeam = findNextTeamToPick();
					SelectMapCommand.pickingTeams.put(selectingTeam, Game.this);
					
					pickTimer(true);
				}
			}
		}, pickTime * 1000);
		
		pickTimers.add(t);
	}
	
	private void startMapUpdater(){
		previousMap = null;
		
		mapUpdater = new Timer();
		mapUpdater.scheduleAtFixedRate(new TimerTask(){
			public void run(){
				if(mapSelectedTime != 0 && mapSelectedTime + (long)(getPickWaitTime() / 2 * 1000) <= System.currentTimeMillis()){
					mapUpdater.cancel();
					return;
				}
				
				if(map != null){
					if(previousMap != null && previousMap.getURL().equalsIgnoreCase(map.getURL())) return;
					
					previousMap = map;
					changeMap(map);
				}
			}
		}, 10000, 10000);	
	}
	
	private void changeMap(Map map){
		sendMessage("!mp map " + map.getBeatmapID() + " " + match.getTournament().getMode());
		
		if(mapMod.length() == 0 || warmupsLeft == 0)
			sendMessage("!mp mods " + getMod(map));
		
		this.map = map;
		this.previousMap = map;
		
		try{
			String bloodcat = map.getBloodcatLink();
			
			if(bloodcat.length() > 0)
				sendMessage("[" + bloodcat + " A bloodcat download link is available for this map here.]");
		}catch(Exception e){}
	}
	
	public String getMod(Map map){
		String mods = "";
		
		switch(map.getCategory()){
			case 5: case 1: mods = "Freemod"; break;
			case 2: mods = "HD"; break;
			case 3: mods = "HR"; break;
			case 4: mods = "DT"; break;
			default: mods = "None"; break;
		}
		
		return mods;
	}
	
	private double getModMultiplier(String mod){
		switch(mod){
			case "HD": return 1.06;
			case "HR": return match.getTournament().isScoreV2() ? 1.1 : 1.06;
			case "DT": return match.getTournament().isScoreV2() ? 1.2 : 1.12;
			case "FL": return 1.12;
			default: return 1;
		}
	}
	
	public void acceptRoll(String player, int roll){
		Team team = findTeam(player);
		
		if(!RandomCommand.waitingForRolls.containsKey(team))
			return;
			
		rollsLeft--;
		RandomCommand.waitingForRolls.remove(team);
		
		if(rollsLeft == 0){
			boolean fTeam = teamToBoolean(team);
			
			if(roll == previousRoll){
				sendMessage("Rolls were equal! Please reroll using !random.");
				RandomCommand.waitingForRolls.put(match.getFirstTeam(), this);
				RandomCommand.waitingForRolls.put(match.getSecondTeam(), this);
				rollsLeft = 2;
				return;
			}
			
			if(roll > previousRoll) fTeamFirst = fTeam;
			else fTeamFirst = !fTeam;
			
			PassTurnCommand.passingTeams.put(fTeamFirst ? match.getFirstTeam() : match.getSecondTeam(), this);
			
			sendMessage((fTeamFirst ? match.getFirstTeam().getTeamName() : match.getSecondTeam().getTeamName()) + 
					        ", you can use !pass within the next 20 seconds to let the other team start instead!");
			
			if(messageUpdater != null) messageUpdater.cancel();
			
			final boolean ffTeam = fTeamFirst;
			
			Timer t = new Timer();
			t.schedule(new TimerTask(){
				public void run(){
					if(ffTeam != fTeamFirst) return;
					
					updateResults(false);
					sendMessage("This match is reffed by a bot! If you have suggestions or have found a bug, please report in our [http://discord.gg/0f3XpcqmwGkNseMR discord server] or to Smc. Thank you!");
					mapSelection(2);
				}
			}, 20000);
		}else previousRoll = roll;
	}
	
	public void passFirstTurn(){
		fTeamFirst = !fTeamFirst;
		mapSelection(2);
	}
	
	@SuppressWarnings("deprecation")
	public void stop(){
		clearPickTimers();
		
		if(multiChannel != null)
			match.getTournament().stopStreaming(this);
		
		waitingForCaptains = 0;
		
		for(String player : invitesSent)
			JoinMatchCommand.gameInvites.remove(player);
		
		InvitePlayerCommand.allowedInviters.remove(match.getFirstTeam());
		InvitePlayerCommand.allowedInviters.remove(match.getSecondTeam());
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
		
		Team winningTeam = fTeamPoints > sTeamPoints ? match.getFirstTeam() : match.getSecondTeam();
		
		if(fTeamPoints != sTeamPoints){
			Team losingTeam = winningTeam.getTeamName().equalsIgnoreCase(match.getFirstTeam().getTeamName()) ? match.getSecondTeam() : match.getFirstTeam();
			
			if(match.getTournament().getConditionalTeams().size() > 0){
				for(String conditional : new HashMap<String, Match>(match.getTournament().getConditionalTeams()).keySet()){
					int conditionalNum = Utils.stringToInt(conditional.split(" ")[1]);
					if(match.getMatchNum() != conditionalNum && match.getServerID() != conditional.split(" ")[1]) continue;
					
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
		
		if(match.getTournament().isUsingTourneyServer() && match.getServerID().length() > 0 && winningTeam.getServerTeamID() != 0){
			RemotePatyServerUtils.setMPLinkAndWinner(mpLink, winningTeam, match.getServerID(), match.getTournament().getName(), fTeamPoints, sTeamPoints);
		}
		
		String shortGameEndMsg = match.getFirstTeam().getTeamName() + " (" + fTeamPoints + 
				  				 ") vs (" + sTeamPoints + ") " + match.getSecondTeam().getTeamName() + 
				  				 " - " + mpLink;
		
		sendMessage("!mp close");
		
		for(String admin : match.getMatchAdmins())
			pmUser(admin, shortGameEndMsg);
		
		for(String admin : match.getTournament().getMatchAdmins())
			pmUser(admin, shortGameEndMsg);
		
		updateResults(true);

		Log.logger.log(Level.INFO, shortGameEndMsg);
		
		if(messageUpdater != null){
			messageUpdater.cancel();
			messageUpdater = null;
		}
		
		if(mapUpdater != null){
			mapUpdater.cancel();
			mapUpdater = null;
		}
		
		if(System.currentTimeMillis() > match.getTournament().getTempLobbyDecayTime()){
			match.getTournament().setTempLobbyDecayTime();
			
			try{
				Main.ircBot.sendIRC().joinChannel("BanchoBot");
			}catch(Exception ex){
				Log.logger.log(Level.INFO, "Could not talk to BanchoBot!");
			}
			
			Main.banchoRegulator.sendPriorityMessage("BanchoBot", "!mp make " + match.getTournament().getDisplayName() + ": TEMP LOBBY");
		}
		
		invitesSent.clear(); invitesSent = null;
		playersInRoom.clear(); playersInRoom = null;
		banchoFeedback.clear(); banchoFeedback = null;
		bans.clear(); bans = null;
		bansWithNames.clear(); bansWithNames = null;
		mapsPicked.clear(); mapsPicked = null;
		captains.clear(); captains = null;
		joinQueue.clear(); joinQueue = null;
		playersSwapped.clear(); playersSwapped = null;
		hijackedSlots.clear(); hijackedSlots = null;
		verifyingSlots.clear(); verifyingSlots = null;
		pickTimers.clear(); pickTimers = null;
		playersRankChecked.clear(); playersRankChecked = null;
		banningTeam = null;
		fTeamFirst = false;
		fTeamPoints = 0;
		map = null;
		mapSelected = false;
		mapSelectedTime = 0;
		mpLink = null;
		multiChannel = null;
		mapMod = null;
		mapLength = 0;
		playersChecked = 0;
		previousMap = null;
		lastPlayedMap = null;
		previousRoll = 0;
		rematchesLeftFTeam = 0;
		rematchesLeftSTeam = 0;
		lastModPickedFTeam = 0;
		lastModPickedSTeam = 0;
		expectedPlayers = 0;
		rollsLeft = 0;
		roomSize = 0;
		selectingTeam = null;
		state = null;
		sTeamPoints = 0;
		validMods = false;
		streamed = false;
		waitingForCaptains = 0;
		warmupsLeft = 0;
		contestState = 0;
		skipRematchState = 0;
		lastPickTime = 0;
		lastWinner = null;
		discordResultMessage = null;
		match.setGame(null);
		
		match.getTournament().removeMatch(match.getMatchNum());
		match = null;
		
		Thread.currentThread().stop();
	}
	
	private String buildDiscordResultMessage(boolean finished){
		String message = "**Match #" + match.getMatchNum() + " in " + match.getTournament().getName() + 
						 "**\n" + mpLink + " ```\n" + match.getFirstTeam().getTeamName() + " - " +
						 match.getSecondTeam().getTeamName() + "\n";
		
		for(int i = 0; i < match.getFirstTeam().getTeamName().length() - 1; i++)
			message += " ";
				
		message += fTeamPoints + " | " + sTeamPoints + "\n";
		
		for(int i = 0; i < match.getFirstTeam().getTeamName().length() - 3; i++)
			message += " ";
		
		message += "Best of " + match.getBestOf() + "\n\n";
		
		message += "Status: " + (finished ? "ended" : getMatchStatus());
		
		if(!finished && playersInRoom.size() > 0){
			message += "\n\nLobby\n";
			
			java.util.Map<Integer, String> players = new HashMap<>();
			LinkedList<String> unslottedPlayers = new LinkedList<>();
			
			LinkedList<Player> fullList = new LinkedList<Player>(match.getFirstTeam().getPlayers());
			fullList.addAll(match.getSecondTeam().getPlayers());
			
			for(String inRoom : playersInRoom){
				Player pl = findPlayer(inRoom);
				
				if(pl.getSlot() == -1) unslottedPlayers.add(inRoom);
				else players.put(pl.getSlot(), inRoom);
			}
			
			for(int i = 1; i <= match.getPlayers(); i++)
				if(!players.containsKey(i))
					players.put(i, "----");
			
			java.util.Map<Integer, String> orderedMap = new TreeMap<>(players);
			
			for(String name : orderedMap.values())
				message += name + "\n";
		}
		
		return message;
	}
	
	private String getMatchStatus(){
		if(rollsLeft != 0) return "pre-warmup";
		else if(warmupsLeft > 0) return "warm-up (" + (warmupsLeft == 2 ? 1 : 2) + "/2)";
		else if(bans.size() < 4) return "bans (" + bans.size() + "/4)";
		else return "ongoing";
	}
	
	private boolean connectToStream(){
		if(match.getTournament().isStreamed(this)) streamed = true;
		else{
			streamed = match.getTournament().startStreaming(this);
			
			if(streamed)
				Main.twitchRegulator.sendMessage(match.getTournament().getTwitchChannel(),
												 "Game switched to " + match.getLobbyName());
		}
		
		return streamed;
	}
	
	private void updateResults(boolean finished){
		if(!finished){
			new Thread(new Runnable(){
				public void run(){
					actualDiscordResultUpdate(finished);
				}
			}).start();
		}else actualDiscordResultUpdate(finished);
	}
	
	private void actualDiscordResultUpdate(boolean finished){
		try{
			String resultDiscord = match.getTournament().getResultDiscord();
			
			String localMessage = buildDiscordResultMessage(finished);
			
			if(resultDiscord != null && discordResultMessage != null){
				if(bansWithNames.size() > 0){
					localMessage += (finished ? "\n" : "") + "\nBans\n";
					
					for(String banned : bansWithNames)
						localMessage += banned + "\n";
				}
				
				discordResultMessage.editMessage(localMessage + "```").queue(new Consumer<Message>(){
					@Override
					public void accept(Message t){
						discordResultMessage = t;
					}
				});
			}
		}catch(Exception e){
			Log.logger.log(Level.SEVERE, e.getMessage(), e);
		}
	}
	
	protected void updateTwitch(String message, int delay){
		Timer t = new Timer();
		t.schedule(new TimerTask(){
			public void run(){
				updateTwitch(message);
			}
		}, delay * 1000);
	}
	
	public void updateTwitch(String message){
		new Thread(new Runnable(){
			public void run(){
				if(connectToStream()){
					Timer t = new Timer();
					t.schedule(new TimerTask(){
						public void run(){
							if(!Main.twitchBot.getUserBot().getChannels().stream().anyMatch(c -> c.getName().equals("#" + match.getTournament().getTwitchChannel())))
								Main.twitchBot.sendIRC().joinChannel("#" + match.getTournament().getTwitchChannel());
						}
					}, (long) (30000.0 / 20.0 + 500));
					
					Main.twitchRegulator.sendMessage(match.getTournament().getTwitchChannel(), message);
				}
			}
		}).start();
	}
	
	private void pmUser(String user, String message){
		try{
			Main.ircBot.sendIRC().joinChannel(user);
		}catch(Exception ex){
			Log.logger.log(Level.INFO, "Could not talk to " + user + "!");
		}
		
		Main.banchoRegulator.sendMessage(user, message);
	}
	
	private void setupLobby(){
		try{
			Main.ircBot.sendIRC().joinChannel("BanchoBot");
		}catch(Exception ex){
			Log.logger.log(Level.INFO, "Could not talk to BanchoBot!");
		}
		
		Main.banchoRegulator.sendPriorityMessage("BanchoBot", "!mp make " + match.getLobbyName());
		
		Timer check = new Timer();
		check.schedule(new TimerTask(){
			public void run(){
				if(multiChannel.length() == 0){
					List<String> connectedMPs = new ArrayList<String>();
					for(Channel channel : Main.ircBot.getUserBot().getChannels())
						connectedMPs.add(channel.getName().replace("#mp_", ""));
					
					for(Match match : match.getTournament().getMatches())
						if(match.getGame() != null)
							if(match.getGame().getMpNum() != -1) 
								connectedMPs.remove(String.valueOf(match.getGame().getMpNum()));
					
					if(!connectedMPs.isEmpty() && !IRCChatListener.gameCreatePMs.isEmpty())
						for(String unusedMP : connectedMPs)
							for(String gamePM : new ArrayList<String>(IRCChatListener.gameCreatePMs))
								if(gamePM.split("\\|")[0].equalsIgnoreCase(unusedMP))
									for(Match match : match.getTournament().getMatches())
										if(match.getGame() != null && match.getLobbyName().equalsIgnoreCase(gamePM.split("\\|")[1])){
											match.getGame().start("#mp_" + unusedMP, "https://osu.ppy.sh/mp/" + unusedMP);
											return;
										}
				}
			}
		}, 60000);
	}
	
	public Team getSelectingTeam(){
		return selectingTeam;
	}
	
	public int getMpNum(){
		return Utils.stringToInt(multiChannel.replace("#mp_", ""));
	}
	
	public GameState getState(){
		return state;
	}
	
	public void handleMapSelect(String map, boolean select, String mod){
		if(state.eq(GameState.PAUSED)) return;
		
		getPickStrategy().handleMapSelect(this, map, select, mod);
	}
	
	public void handlePause(boolean pause){
		if(pause){
			if(state.eq(GameState.PAUSED)) return;
			
			clearPickTimers();
			
			SkipRematchCommand.gamesAllowedToSkip.remove(this);
			ContestCommand.gamesAllowedToContest.remove(this);
			skipRematchState = 0;
			contestState = 0;
			mapSelectedTime = 0;
			
			if(messageUpdater != null) messageUpdater.cancel();
			if(mapUpdater != null) mapUpdater.cancel();
			
			if(state.eq(GameState.PLAYING)){
				sendMessage("!mp abort");
				sendMessage("!mp aborttimer");
			}
			
			state = GameState.PAUSED;
			
			banchoFeedback.clear();
			
			switchPlaying(false, true);
		}else{
			if(!state.eq(GameState.PAUSED)) return;
			
			state = GameState.WAITING;
			
			if(rollsLeft > 0) mapSelection(1);
			else if(mapSelected) prepareReadyCheck();
			else if(warmupsLeft > 0) mapSelection(2);
			else if(bans.size() < 4) mapSelection(3);
			else mapSelection(4);
		}
	}
	
	public boolean forceSelect(boolean revertScore, Map newMap){
		if(rollsLeft > 0){
			sendMessage("You cannot force select whilst " + (isTeamGame() ? "teams" : "players") + " have not rolled yet!");
			return false;
		}else if(warmupsLeft != 0){
			sendMessage("You cannot force select whilst " + (isTeamGame() ? "teams" : "players") + " have not finished warmups yet!");
			return false;
		}else if(bans.size() < 4){
			sendMessage("You cannot force select whilst " + (isTeamGame() ? "teams" : "players") + " have not banned yet!");
			return false;
		}
		
		if(newMap != null) changeMap(newMap);
		else{
			map = lastPlayedMap;
			previousMap = map;
		}
		
		if(revertScore && lastWinner != null && map != null){
			if(teamToBoolean(lastWinner)) fTeamPoints--;
			else sTeamPoints--;
			
			setScores(fTeamPoints, sTeamPoints);
			
			int mapId = match.getMapPool().getMapId(map);
			
			if(mapId != 0){
				int tourneyId = 0;
				
				try{
					tourneyId = RemotePatyServerUtils.fetchTournamentId(match.getTournament().getName());
				}catch(Exception e){
					Log.logger.log(Level.SEVERE, "Could not fetch tourney id", e);
				}
				
				if(tourneyId != 0){
					RemotePatyServerUtils.incrementMapValue(mapId, match.getMapPool().getPoolNum(), tourneyId, "pickcount", -1);
				}
			}
		}
		
		skipRematchState = 0;
		SkipRematchCommand.gamesAllowedToSkip.remove(Game.this);
		
		SelectMapCommand.pickingTeams.remove(selectingTeam);
		
		if(mapUpdater != null) mapUpdater.cancel();
		if(messageUpdater != null) messageUpdater.cancel();
		mapSelected = true;
		banchoFeedback.clear();
		switchPlaying(false, true);

		state = GameState.WAITING;
		
		prepareReadyCheck();
		
		return true;
	}
	
	public void acceptSkipRematch(String player){
		Team team = findTeam(player);
		
		switch(skipRematchState){
			case 0: skipRematchState = teamToBoolean(team) ? 1 : 2;
					sendMessage(team.getTeamName() + " has voted to skip the rematch!");
					break;
			case 1: if(teamToBoolean(team)) break;
					else{skipRematchState = 3; break;}
			case 2: if(!teamToBoolean(team)) break;
					else{skipRematchState = 3; break;}
			default: return;
		}
		
		if(skipRematchState == 3){
			messageUpdater.cancel();
			SkipRematchCommand.gamesAllowedToSkip.remove(this);
			
			sendMessage("The rematch has been skipped.");
			
			skipRematchState = 0;
			
			if(lastRematchFTeam) rematchesLeftFTeam++;
			else rematchesLeftSTeam++;
			
			mapSelected = false;
			clearPickTimers();
			
			if(teamToBoolean(lastWinner)) fTeamPoints++;
			else sTeamPoints++;
			
			updateScores(true);
			
			mapSelection(4);
		}
	}
	
	public void acceptContest(String player){
		Team team = findTeam(player);
		
		switch(contestState){
			case 0: contestState = teamToBoolean(team) ? 1 : 2;
					sendMessage(team.getTeamName() + " has voted to contest the score!");
					break;
			case 1: if(teamToBoolean(team)) break;
					else{contestState = 3; break;}
			case 2: if(!teamToBoolean(team)) break;
					else{contestState = 3; break;}
			default: return;
		}
		
		if(contestState == 3){		
			sendMessage("The contest has been accepted.");
			
			contestState = 0;
			
			boolean selectAfter = false;
			
			if(fTeamPoints > Math.floor(match.getBestOf() / 2) || sTeamPoints > Math.floor(match.getBestOf() / 2))
				selectAfter = true;
			
			
			if(teamToBoolean(lastWinner)){
				fTeamPoints--;
				sTeamPoints++;
				lastWinner = match.getSecondTeam();
			}else{
				sTeamPoints--;
				fTeamPoints++;
				lastWinner = match.getFirstTeam();
			}
			
			updateScores(false);
			
			if(selectAfter) mapSelection(4);
		}
	}
	
	public void acceptWarmupSkip(String player){
		Team team = findTeam(player);
		
		if(selectingTeam.getTeamName().equalsIgnoreCase(team.getTeamName())){
			clearPickTimers();
			
			SkipWarmupCommand.gamesAllowedToSkip.remove(this);
			sendMessage("The warmup has been skipped!");

			warmupsLeft--;
			
			updateResults(false);
			
			if(warmupsLeft == 0) mapSelection(3);
			else mapSelection(2);
		}
	}
	
	public void acceptWarmupModChange(String player, String mod){
		if(warmupsLeft > 0 && findTeam(player).getTeamName().equalsIgnoreCase(selectingTeam.getTeamName())){
			double modMultiplier = 1;
			
			if(mod.length() > 0){
				if(mod.equalsIgnoreCase("DT") || mod.equalsIgnoreCase("NC"))
					modMultiplier = 1.5;
				else if(mod.equalsIgnoreCase("HT"))
					modMultiplier = 0.75;
			}
			
			if(mapLength / modMultiplier > 270){
				sendMessage("This would make the warmup too long! The maximum length is 4m30s.");
				return;
			}
			
			sendMessage("!mp mods " + (mod.toUpperCase().equals("NM") ? "" : mod.toUpperCase() + " ") + "Freemod");
		}
	}
	
	protected void prepareReadyCheck(){
		clearPickTimers();
		
		String message = "Waiting for all players to ready up...";
		
		if(match.getTournament().isScoreV2()) message = "Waiting for all players to ready up, stable (fallback) scores will not count!";
		
		pickTimer(false);
		
		messageUpdater(0, true, message, "The match will force start after the timer. You may change map if needed (not in case of disconnection)");
	}
	
	protected boolean checkMap(Map map){
		return !mapsPicked.contains(map);
	}
	
	public void handleBanchoFeedback(String message){
		if(message.contains("All players are ready") && state.eq(GameState.WAITING)) readyCheck(true);
		else if(message.contains("left the game.")) playerLeft(message);
		else if(message.contains("The match has finished!")) playEnded();
		else if(message.contains("The match has started!") && !state.eq(GameState.PLAYING)) unexpectedMatchStart();
		else if(message.contains("joined in")) acceptExternalInvite(message);
		else if(message.startsWith("Slot ") && state.eq(GameState.FIXING)) fixLobby(message);
		else if(message.startsWith("Slot ") && state.eq(GameState.VERIFYING)) verifyLobby(message);
		else if(message.startsWith("Slot ") && state.eq(GameState.RESIZING)) ((TeamGame) this).resize(message);
		else if(message.startsWith("Beatmap: ")) updateMap(message.split(" ")[1]);
		else if(message.startsWith("Players: ") && (state.eq(GameState.FIXING) || state.eq(GameState.VERIFYING)))
			expectedPlayers = Utils.stringToInt(message.split(" ")[1]);
		else banchoFeedback.add(message);
	}
	
	private void unexpectedMatchStart(){
		state = GameState.PLAYING;
		sendMessage("!mp settings");
	}
	
	private void updateMap(String link){
		if(map != null && map.getURL().equalsIgnoreCase(link)) return;
		
		if(state.eq(GameState.PLAYING)){
			mapSelected = true;
			
			this.map = match.getMapPool().findMap(link);
			mapsPicked.add(map);
			
			return;
		}
		
		if(!mapSelected){
			state = GameState.WAITING;
			mapSelected = true;
			
			this.map = match.getMapPool().findMap(link);
			
			JSONObject jsMap = Map.getMapInfo(map.getBeatmapID(), match.getTournament().getMode(), true);
			
			updateTwitch(getMod(map).replace("None", "Nomod") + " pick: " + jsMap.getString("artist") + " - " + 
		    	  	 	 jsMap.getString("title") + " [" + jsMap.getString("version") + "] was picked by " + 
		    	  	 	 selectingTeam.getTeamName() + "!");
			
			prepareReadyCheck();
			return;
		}else if(mapSelected){
			this.map = match.getMapPool().findMap(link);
			
			JSONObject jsMap = Map.getMapInfo(map.getBeatmapID(), match.getTournament().getMode(), true);
			
			updateTwitch(getMod(map).replace("None", "Nomod") + " pick: " + jsMap.getString("artist") + " - " + 
		    	  	 	 jsMap.getString("title") + " [" + jsMap.getString("version") + "] was picked by " + 
		    	  	 	 selectingTeam.getTeamName() + "!");
			
			return;
		}
	}
	
	private void readyCheck(boolean bancho){
		if(state.eq(GameState.CONFIRMING) || state.eq(GameState.PLAYING) || state.eq(GameState.PAUSED)) return;
		
		if(playersInRoom.size() == match.getPlayers() && mapSelected && state.eq(GameState.WAITING)){ 
			if((map.getCategory() != 5 && previousMap == null) || (previousMap != null && map.getCategory() != 5 
																  && !previousMap.getURL().equalsIgnoreCase(map.getURL()))){
				changeMap(map);
				Utils.sleep(2500);
				return;
			}
			
			if(mapUpdater != null) mapUpdater.cancel();
			if(messageUpdater != null) messageUpdater.cancel();
			
			SelectMapCommand.pickingTeams.remove(selectingTeam);
			
			verifyLobby(null);
		}else if(mapSelected && state.equals(GameState.WAITING)) fixLobby(null);
		else if((state.eq(GameState.VERIFYING) || state.eq(GameState.FIXING)) && playersInRoom.size() == match.getPlayers()){
			finalModCheck();
		}else if(!bancho){
			state = GameState.WAITING;
			prepareReadyCheck();
		}
	}
	
	private void verifyLobby(String message){
		if(message == null && !state.eq(GameState.VERIFYING)){
			state = GameState.VERIFYING;
			
			playersSwapped.clear();
			verifyingSlots.clear();
			expectedPlayers = 0;
			
			verifyMods();
			
			sendPriorityMessage("!mp settings");
			
			Timer t = new Timer();
			t.schedule(new TimerTask(){
				public void run(){
					if(state.eq(GameState.VERIFYING) && playersSwapped.isEmpty()) //playersChecked?
						sendPriorityMessage("!mp settings");
				}
			}, 2500);
			return;
		}else fixPlayer(message, false);
	}
	
	private void fixLobby(String message){
		if(message == null && !state.eq(GameState.FIXING)){
			state = GameState.FIXING;
			
			messageUpdater.cancel();
			playersSwapped.clear();
			verifyingSlots.clear();
			expectedPlayers = 0;
			
			verifyMods();
			
			sendPriorityMessage("!mp settings");
			playersInRoom.clear();
			
			Timer t = new Timer();
			t.schedule(new TimerTask(){
				public void run(){
					if(state.eq(GameState.FIXING) && playersSwapped.isEmpty())
						sendPriorityMessage("!mp settings");
				}
			}, 2500);
			return;
		}else fixPlayer(message, true);
	}
	
	private void fixPlayer(String message, boolean fixing){
		int slot = Utils.stringToInt(message.split(" ")[1]);
		boolean hasMod = false;
		double modMultiplier = 1;
		
		if(message.endsWith("]") && (map.getCategory() == 1 || map.getCategory() == 5) && warmupsLeft <= 0){
			String[] sBracketSplit = message.split("\\[");
			
			String mods = sBracketSplit[sBracketSplit.length - 1].split("\\]")[0];
			
			if(mods.split(" ").length <= 2){
				if(map.getCategory() == 1) playersChecked++; 
			}else{
				boolean validMod = true;
				
				loop: for(String mod : mods.split(", ")){
					if(mod.contains("/") && mod.contains("Team")) mod = mod.split("\\/")[1].substring(1);
					switch(mod.toLowerCase()){
						case "hidden": modMultiplier *= 1.06; break;
						case "hardrock": modMultiplier *= (match.getTournament().isScoreV2() ? 1.1 : 1.06); break;
						case "flashlight": modMultiplier *= 1.12; break;
						case "easy": modMultiplier *= 0.5; break;
						default: validMod = false; break loop;
					}
				}
				
				if(!validMod){					
					sendMessage("You can only use HD, HR, EZ or FL! Make sure you do not have any other mods!");
					sendMessage("!mp map " + map.getBeatmapID() + " 0");
					validMods = false;
				}else if(map.getCategory() == 1) hasMod = true;
			}
		}else if(map.getCategory() == 1 && warmupsLeft <= 0) playersChecked++;
		
		message = Utils.removeExcessiveSpaces(message);
		
		String[] spaceSplit = message.split(" ");
		
		String beforeLink = "";
		
		int count = 0;
		for(String arg : spaceSplit){
			if(arg.contains("osu.ppy.sh")) break;
			
			beforeLink += arg + " ";
			
			count++;
		}
		
		if(beforeLink.toLowerCase().contains("no map"))
			allowedToStart = false;
		
		String player = "";
		String teamColor = "";

		for(int i = count + 1; i < spaceSplit.length; i++){
			if(spaceSplit[i].equalsIgnoreCase("[Team")){
				count = i;
				break;
			}
			if(spaceSplit[i].equalsIgnoreCase("[Host")){
				count = i + 2;
				break;
			}
			
			player += spaceSplit[i] + "_";
		}
		
		player = player.substring(0, player.length() - 1);
		
		if(fixing) playersInRoom.add(player);
		
		Team team = findTeam(player);
		
		if(team == null){
			sendMessage("!mp kick " + player);
			sendMessage(player + " is not on a team!");
			
			allowedToStart = false;
			return;
		}
		
		Player p = findPlayer(player);
		
		if(map.getCategory() > 1 && map.getCategory() != 5)
			modMultiplier = getModMultiplier(getMod(map));
		
		if(p != null){
			p.setHasMod(hasMod);
			p.setModMultiplier(modMultiplier);
			playersChecked++;	
		}
		
		if(fixing) p.setSlot(slot);
		
		teamColor = spaceSplit[count + 1].replaceAll(" ", "");
				
		boolean fTeam = teamToBoolean(team);
		
		if(fTeam && (!teamColor.equalsIgnoreCase("Blue") && !teamColor.contains("Blue")))
			sendMessage("!mp team " + p.getIRCTag() + " blue");
		else if(!fTeam && (!teamColor.equalsIgnoreCase("Red") && !teamColor.contains("Red")))
			sendMessage("!mp team " + p.getIRCTag() + " red");
		
		verifyingSlots.put(p, slot);
		
		if(verifyingSlots.size() >= match.getPlayers() || 
				(expectedPlayers > 0 && verifyingSlots.size() >= expectedPlayers)){
			lobbySwapFixing();
			readyCheck(false);
		}
	}
	
	private void lobbySwapFixing(){
		List<Player> fTeamList = new ArrayList<Player>();
		List<Player> sTeamList = new ArrayList<Player>();
		
		for(Player pl : verifyingSlots.keySet()){
			Team team = findTeam(pl.getName().replaceAll(" ", "_"));
			
			boolean fTeam = teamToBoolean(team);
			
			if(fTeam && !fTeamList.contains(pl)){
				fTeamList.add(pl);
				
				if(fTeamList.size() > match.getPlayers() / 2){
					sendMessage("!mp kick " + pl.getIRCTag());
					sendMessage(pl.getName() + " was kicked to the team being full!");
					fTeamList.remove(pl);
					
					continue;
				}
			}else if(!fTeam && !sTeamList.contains(pl)){
				sTeamList.add(pl);
				
				if(sTeamList.size() > match.getPlayers() / 2){
					sendMessage("!mp kick " + pl.getIRCTag());
					sendMessage(pl.getName() + " was kicked to the team being full!");
					sTeamList.remove(pl);
					
					continue;
				}
			}
			
			if(playersSwapped.contains(pl)) continue;
			
			Team oppositeTeam = fTeam ? match.getSecondTeam() : match.getFirstTeam();
			
			swapPositions(fTeam, verifyingSlots.get(pl), pl, oppositeTeam);
		}
	}
	
	private void swapPositions(boolean fTeam, int slot, Player p, Team oppositeTeam){
		if(fTeam && slot > match.getPlayers() / 2 || !fTeam && slot <= match.getPlayers() / 2){	
			playersSwapped.add(p);
			
			for(Player pl : oppositeTeam.getPlayers()){
				if(!verifyingSlots.containsKey(pl)) continue;
				
				if(fTeam && verifyingSlots.get(pl) <= match.getPlayers() / 2 || 
					!fTeam && verifyingSlots.get(pl) > match.getPlayers() / 2){
					roomSize += 1;
					playersSwapped.add(pl);
					
					int newSlot = verifyingSlots.get(pl);
					pl.setSlot(slot);
					p.setSlot(newSlot);
					sendMessage("!mp size " + roomSize);
					sendMessage("!mp move " + p.getIRCTag() + " " + roomSize);
					sendMessage("!mp move " + pl.getIRCTag() + " " + slot);
					sendMessage("!mp move " + p.getIRCTag() + " " + newSlot);
					
					roomSize -= 1;
					sendMessage("!mp size " + roomSize);
					break;
				}
			}
		}
	}
	
	private void clearPickTimers(){
		try{
			if(!pickTimers.isEmpty())
				for(Timer t : new ArrayList<Timer>(pickTimers)){
					if(t != null) t.cancel();	
					pickTimers.remove(t);
				}	
		}catch(Exception e){
			Log.logger.log(Level.INFO, e.getMessage(), e);
		}
	}
	
	private void startMatch(int timer, boolean backupCall){	
		if(state.eq(GameState.PAUSED)) return;
		
		clearPickTimers();
		
		if(!backupCall){
			Timer t = new Timer();
			t.schedule(new TimerTask(){
				public void run(){
					if(playersInRoom.size() == match.getPlayers() && !state.eq(GameState.PLAYING))
						startMatch(0, true);
				}
			}, timer * 1000 + 1000);	
		}
		
		SkipRematchCommand.gamesAllowedToSkip.remove(this);
		ContestCommand.gamesAllowedToContest.remove(this);
		skipRematchState = 0;
		contestState = 0;
		mapSelectedTime = 0;
		
		if(messageUpdater != null) messageUpdater.cancel();
		
		if(roomSize > match.getPlayers()){
			roomSize = match.getPlayers();
			sendMessage("!mp size " + roomSize);
		}
		
		if(!mapsPicked.contains(map)){
			mapsPicked.add(map);
			
			if(warmupsLeft == 0 && bans.size() == 4){
				if(teamToBoolean(selectingTeam)) 
					lastModPickedFTeam = map.getCategory();
				else lastModPickedSTeam = map.getCategory();
			}
		}
		
		banchoFeedback.clear();
		
		lastPlayedMap = map;
		mapSelected = false;
		switchPlaying(true, false);
		
		if(timer == -1) return;
		
		state = GameState.PLAYING;
		
		if(timer != 0) sendMessage("!mp start " + timer);
		else sendMessage("!mp start");
		
		updateResults(false);
		updateTwitch("The match has begun!");
	}
	
	protected void switchPlaying(boolean playing, boolean full){
		if(full){
			LinkedList<Player> tempList = new LinkedList<>(match.getFirstTeam().getPlayers());
			tempList.addAll(match.getSecondTeam().getPlayers());
			
			for(Player pl : tempList){
				pl.setPlaying(playing);
				pl.setVerified(false);
			}
		}else
			for(String player : playersInRoom){
				Player pl = findPlayer(player);
				pl.setPlaying(playing);
				pl.setVerified(false);
			}
	}
	
	private int calculateMissingScores(boolean fTeam){ //v2 only
		int score = 0;
		
		Team team = fTeam ? match.getFirstTeam() : match.getSecondTeam();
		
		LinkedList<Player> tempList = new LinkedList<>(team.getPlayers());
		
		for(Player pl : tempList)
			if(pl.isPlaying() && !pl.isVerified())
				score += 1000000 * pl.getModMultiplier() + 100000;
		
		return score;
	}
	
	private void verifyMods(){
		if(map.getCategory() == 1 && warmupsLeft <= 0){
			playersChecked = 0;
			LinkedList<Player> tempList = new LinkedList<>(match.getFirstTeam().getPlayers());
			tempList.addAll(match.getSecondTeam().getPlayers());
			
			for(Player pl : tempList){
				pl.setHasMod(false);
				pl.setModMultiplier(1);
				switchPlaying(false, true);
			}
		}
	}
	
	private void finalModCheck(){
		if(playersChecked >= match.getPlayers() && validMods && map.getCategory() == 1 && warmupsLeft <= 0 && allowedToStart){
			playersChecked = 0;
			int fTeamModCount = 0, sTeamModCount = 0;
			
			for(Player pl : match.getFirstTeam().getPlayers())
				if(pl.hasMod()) fTeamModCount++;
			
			for(Player pl : match.getSecondTeam().getPlayers())
				if(pl.hasMod()) sTeamModCount++;
			
			int countNeeded = (int) Math.ceil((double) match.getPlayers() / 4.0);
			
			if(fTeamModCount < countNeeded || sTeamModCount < countNeeded){
				sendMessage("You need to have at least " + countNeeded + " mod user" + 
							(countNeeded <= 1 ? "" : "s") + " per team!");
				sendMessage("!mp map " + map.getBeatmapID() + " 0");
				state = GameState.WAITING;
				
				prepareReadyCheck();
			}else startMatch(0, false);
		}else if(!validMods || !allowedToStart){
			validMods = true;
			allowedToStart = true;
			state = GameState.WAITING;
			prepareReadyCheck();
		}else startMatch(5, false);
	}
	
	protected abstract void playerLeft(String message);
	
	protected boolean isTeamGame(){
		return match.getTournament().getTournamentType() == 0;
	}
	
	protected void playEnded(){
		if(state.eq(GameState.WAITING)) return;
		state = GameState.CONFIRMING;
		
		Timer t = new Timer();
		t.schedule(new TimerTask(){
			public void run(){
				clearPickTimers();
				
				state = GameState.WAITING;
				validMods = true;
				mapSelected = false;
				
				float fTeamScore = 0, sTeamScore = 0;
				List<String> fTeamPlayers = new ArrayList<>();
				List<String> sTeamPlayers = new ArrayList<>();
				long totalScore = 0, targetRangeScore = 0;
				long totalPasses = 0, targetRangePasses = 0;
				long totalSubmits = 0;

				for(String feedback : banchoFeedback){
					if(feedback.contains("finished playing")){
						String player = feedback.substring(0, feedback.indexOf(" finished"));
						
						if(fTeamPlayers.contains(player) || sTeamPlayers.contains(player)) continue;
						
						boolean fTeam = teamToBoolean(findTeam(player));
						
						if(fTeam) fTeamPlayers.add(player);
						else sTeamPlayers.add(player);
						
						Player pl = findPlayer(player);
						pl.setVerified(true);
						
						totalSubmits++;
						
						if(feedback.split(",")[1].substring(1).split("\\)")[0].equalsIgnoreCase("PASSED")){
							int score = Utils.stringToInt(feedback.split("Score: ")[1].split(",")[0]);
							
							if(score > 1500000 && match.getTournament().isScoreV2()){
								sendMessage(player + " is on fallback, please use stable! His score is not counted for this pick.");
								continue;
							}
							
							if(fTeam) fTeamScore += score;
							else sTeamScore += score;
							
							totalScore += score;
							
							totalPasses++;
							
							if(pl.getRank() > 0 && match.getTournament().isWithinTargetBounds(pl.getRank())){
								targetRangeScore += score;
								targetRangePasses++;
							}
						}
					}
				}
				
				if(warmupsLeft > 0){
					String updateMessage = "";
					
					if(fTeamScore == sTeamScore) updateMessage = "Both " + (isTeamGame() ? "teams" : "players") + " have tied!";
					else
						updateMessage = (fTeamScore > sTeamScore ? match.getFirstTeam().getTeamName() :
										 match.getSecondTeam().getTeamName()) + " won by " + 
										 Utils.veryLongNumberDisplay((long) Math.abs(fTeamScore - sTeamScore)) + " points!";
					
					warmupsLeft--;
					
					sendMessage(updateMessage);
					updateTwitch(updateMessage, 20);
					
					updateResults(false);
					
					if(warmupsLeft == 0) mapSelection(3);
					else mapSelection(2);
				}else{
					rematch: if((int) Math.abs(fTeamPlayers.size() - sTeamPlayers.size()) != 0){	
						boolean fTeamDC = sTeamPlayers.size() > fTeamPlayers.size();
						
						if((fTeamDC && fTeamScore < sTeamScore) || (!fTeamDC && sTeamScore < fTeamScore)){
							if(match.getTournament().isScoreV2()){
								float fScore = fTeamScore + calculateMissingScores(true);
								float sScore = sTeamScore + calculateMissingScores(false);
								if((fTeamDC && sScore > fScore) || (!fTeamDC && fScore > sScore))
									break rematch;
							}
							
							if((fTeamDC ? rematchesLeftFTeam : rematchesLeftSTeam) > 0){
								if(fTeamDC) rematchesLeftFTeam--;
								else rematchesLeftSTeam--;
								
								lastRematchFTeam = fTeamDC;
								
								lastWinner = fTeamScore > sTeamScore ? match.getFirstTeam() : match.getSecondTeam();
								
								sendMessage("Someone has disconnected, there will be a rematch!");
								sendMessage("If you do not wish to rematch, both " + (isTeamGame() ? "teams" : "players") + " need to use !skiprematch");
								
								updateTwitch("There was a disconnection, the match will be replayed!");
								
								skipRematchState = 0;
								SkipRematchCommand.gamesAllowedToSkip.add(Game.this);
								
								mapSelected = true;
								banchoFeedback.clear();
								switchPlaying(false, true);
								
								prepareReadyCheck();
								return;
							}
						}
					}
					
					if(fTeamScore == sTeamScore){
						sendMessage("Both " + (isTeamGame() ? "teams" : "players") + " have tied, there will be a rematch!");
						mapSelected = true;
						banchoFeedback.clear();
						
						switchPlaying(false, true);
						
						updateTwitch("Both " + (isTeamGame() ? "teams" : "players") + " have tied, there will be a rematch!");
						
						return;
					}
					
					if(match.getTournament().isUsingMapStats()){
						int mapId = match.getMapPool().getMapId(map);
						
						if(mapId != 0){
							int tourneyId = 0;
							
							try{
								tourneyId = RemotePatyServerUtils.fetchTournamentId(match.getTournament().getName());
							}catch(Exception e){
								Log.logger.log(Level.SEVERE, "Could not fetch tourney id", e);
							}
							
							if(tourneyId != 0){
								RemotePatyServerUtils.incrementMapValue(mapId, match.getMapPool().getPoolNum(), tourneyId, "pickcount", 1);
								
								if(sTeamScore > 0 && fTeamScore > 0){
									RemotePatyServerUtils.incrementMapValue(mapId, match.getMapPool().getPoolNum(), tourneyId, "total_score", totalScore);
									RemotePatyServerUtils.incrementMapValue(mapId, match.getMapPool().getPoolNum(), tourneyId, "target_range_score", targetRangeScore);
								}
								
								RemotePatyServerUtils.incrementMapValue(mapId, match.getMapPool().getPoolNum(), tourneyId, "target_range_passed", targetRangePasses);
								RemotePatyServerUtils.incrementMapValue(mapId, match.getMapPool().getPoolNum(), tourneyId, "plays_submitted", totalSubmits);
								RemotePatyServerUtils.incrementMapValue(mapId, match.getMapPool().getPoolNum(), tourneyId, "plays_passed", totalPasses);
							}
						}
					}
					
					boolean fTeamWon = fTeamScore > sTeamScore;
					
					String updateMessage = (fTeamScore > sTeamScore ? match.getFirstTeam().getTeamName() :
						 					match.getSecondTeam().getTeamName()) + " won by " + 
						 					Utils.veryLongNumberDisplay((long) Math.abs(fTeamScore - sTeamScore)) + " points!";
					
					sendMessage(updateMessage);
					updateTwitch(updateMessage, 20);
					
					if(fTeamWon) fTeamPoints++;
					else sTeamPoints++;
					
					lastWinner = fTeamScore > sTeamScore ? match.getFirstTeam() : match.getSecondTeam();
					
					updateScores(true);
				}
				
				switchPlaying(false, true);
				
				banchoFeedback.clear();
			}
		}, 10000);
	}
	
	protected void updateScores(boolean selectMaps){
		sendMessage(match.getFirstTeam().getTeamName() + " " + fTeamPoints + " | " +
				sTeamPoints + " " + match.getSecondTeam().getTeamName() +
				"      Best of " + match.getBestOf());
		
		updateResults(false);
		updateTwitch(match.getFirstTeam().getTeamName() + " " + fTeamPoints + " | " +
					 sTeamPoints + " " + match.getSecondTeam().getTeamName() + " BO" + match.getBestOf(), 20);
	
		if(fTeamPoints + sTeamPoints == match.getBestOf() - 1 && fTeamPoints == sTeamPoints){
			changeMap(match.getMapPool().findTiebreaker());
			
			mapSelected = true;
			if(mapUpdater != null) mapUpdater.cancel();
			
			contestMessage();
		}else if(fTeamPoints > Math.floor(match.getBestOf() / 2) || sTeamPoints > Math.floor(match.getBestOf() / 2)){
			String winningTeam = (fTeamPoints > sTeamPoints ? match.getFirstTeam().getTeamName() : match.getSecondTeam().getTeamName());
			
			sendMessage(winningTeam + " has won this game!");
			sendMessage("The lobby is ending in 30 seconds, thanks for playing!");
			sendMessage("!mp timer");
			
			if(mapUpdater != null) mapUpdater.cancel();
			
			updateTwitch(winningTeam + " has won this game! " + mpLink, 20);
			
			Timer twitchCloseDelay = new Timer();
			twitchCloseDelay.schedule(new TimerTask(){
				public void run(){
					if(fTeamPoints > Math.floor(match.getBestOf() / 2) || sTeamPoints > Math.floor(match.getBestOf() / 2))
						match.getTournament().stopStreaming(Game.this);
				}
			}, 25500);
			
			Timer time = new Timer();
			time.schedule(new TimerTask(){
				public void run(){
					if(fTeamPoints > Math.floor(match.getBestOf() / 2) || sTeamPoints > Math.floor(match.getBestOf() / 2))
						stop();
				}
			}, 30000);
			
			contestMessage();
			
			return;
		}else if(selectMaps){
			contestMessage();
			mapSelection(4);
		}
	}
	
	protected void contestMessage(){
		ContestCommand.gamesAllowedToContest.add(this);
		contestState = 0;
		
		sendMessage("If you wish to give the other team the point instead, both teams please use !contest");
	}
	
	protected void acceptExternalInvite(String message){
		String player = message.split(" joined in")[0].replaceAll(" ", "_");
		if(playersInRoom.contains(player)) return;
		
		if(!verifyPlayer(player)){
			sendMessage(player + " tried to join, but they are not on either team! Contact a tournament organizer if you believe this to be an error."); 
			sendMessage("!mp kick " + player); 
			return;
		}
		
		if(joinQueue.contains(player)) joinQueue.remove(player);
		
		Player pl = findPlayer(player);
		String userID = Utils.getOsuPlayerId(pl.getName());
		
		if(!userID.equals("-1")) pl.setUserID(userID);
		
		if(joinQueue.isEmpty()){
			joinQueue.add(player);
			joinRoom(player, true);
		}else{
			int slot = Utils.stringToInt(message.split("joined in slot ")[1].split(" for team")[0]);
			findPlayer(player).setSlot(slot);
			hijackedSlots.put(slot, player);
		}
	}
	
	protected void sendPriorityMessage(String command){
		Main.banchoRegulator.sendPriorityMessage(multiChannel, command);
	}
	
	protected void sendMessage(String command){
		Main.banchoRegulator.sendMessage(multiChannel, command);
	}
	
	public void invitePlayer(String player){
		if(!invitesSent.contains(player)) invitesSent.add(player);
		JoinMatchCommand.gameInvites.put(player, this);
		
		sendInviteMessages(player);
	}
	
	protected void sendInviteMessages(String player){
		pmUser(player, "You have been invited to join: " + match.getLobbyName());
		pmUser(player, "Reply with !join to join the game. Please note that this command is reusable.");
		
		Log.logger.log(Level.INFO, "Invited " + player + " to join " + multiChannel);
	}
	
	public void acceptInvite(String player){
		if(playersInRoom.contains(player.replaceAll(" ", "_")) || playersInRoom.size() >= match.getPlayers()) return;
		if(!verifyPlayer(player.replaceAll(" ", "_"))) return;
		
		if(joinQueue.contains(player.replaceAll(" ", "_"))) return;
		else joinQueue.add(player.replaceAll(" ", "_"));
		
		Player pl = findPlayer(player);
		String userID = Utils.getOsuPlayerId(pl.getName());
		
		if(!userID.equals("-1")) pl.setUserID(userID);

		joinRoom(player.replaceAll(" ", "_"), false);
	}
	
	protected void joinRoom(String player, boolean hijack){
		playersInRoom.add(player.replaceAll(" ", "_"));
		assignSlotAndTeam(player.replaceAll(" ", "_"), hijack);
	}
	
	private void advanceQueue(String player, List<String> hijackers){
		joinQueue.remove(player);
		
		if(joinQueue.size() > 0){
			String nextPlayer = "";
			
			for(String pl : joinQueue){
				nextPlayer = pl;
				break;
			}
			
			joinRoom(nextPlayer, false);
		}
		
		if(!hijackers.isEmpty()){
			for(String hijacker : hijackers)
				joinRoom(hijacker, false);
			
			roomSize = match.getPlayers();
			sendMessage("!mp size " + roomSize);
		}
		
		if(!hijackedSlots.isEmpty()){
			for(int slot : hijackedSlots.keySet()){
				String hijackingPlayer = hijackedSlots.get(slot);
				
				playersInRoom.add(hijackingPlayer);
				
				hijackedSlots.remove(slot);
				if(!assignSlotAndTeam(hijackingPlayer, true)){
					Player pl = findPlayer(hijackingPlayer);
					playersInRoom.remove(hijackingPlayer);
					
					if(pl == null) sendMessage("!mp kick " + hijackingPlayer);
					else sendMessage("!mp kick " + pl.getIRCTag());
				}
			}
		}
	}
	
	protected Team findTeam(String player){
		for(Player p : match.getFirstTeam().getPlayers())
			if(p.eq(player)) return match.getFirstTeam();
		
		for(Player p : match.getSecondTeam().getPlayers())
			if(p.eq(player)) return match.getSecondTeam();
		
		return null;
	}

	protected boolean teamToBoolean(Team team){
		return team.getTeamName().equalsIgnoreCase(match.getFirstTeam().getTeamName());
	}
	
	protected Player findPlayer(String player){
		LinkedList<Player> fullList = new LinkedList<Player>(match.getFirstTeam().getPlayers());
		fullList.addAll(match.getSecondTeam().getPlayers());
		
		for(Player p : fullList)
			if(p.eq(player)) return p;
		
		return null;
	}
	
	protected Team findNextTeamToPick(){
		int totalPicked = fTeamPoints + sTeamPoints;
		
		if(warmupsLeft == 1) totalPicked = 1;
		
		boolean fTeam = true;
		
		fTeam = (totalPicked + 2) % 2 == 0 ? fTeamFirst : !fTeamFirst; 
		
		return fTeam ? match.getFirstTeam() : match.getSecondTeam();
	}
	
	protected Team findNextTeamToBan(){
		int totalBanned = bans.size();
		
		boolean fTeam = true;
		
		fTeam = (totalBanned + 2) % 2 == 0 ? fTeamFirst : !fTeamFirst;
		
		return fTeam ? match.getFirstTeam() : match.getSecondTeam();
	}
	
	public boolean verifyPlayer(String player){
		return findTeam(player) != null;
	}
	
	protected boolean isCaptain(String player){
		for(String pl : captains)
			if(player.replaceAll(" ", "_").equalsIgnoreCase(pl)){
				return verifyPlayer(player);
			}
		
		return false;
	}
	
	private boolean assignSlotAndTeam(String player, boolean hijack){
		if(!joinQueue.contains(player)) joinQueue.add(player);
		
		Team team = findTeam(player);
		boolean fTeam = teamToBoolean(team);
		String color = fTeam ? "blue" : "red";
		Player pl = findPlayer(player);
		int i = fTeam ? 1 : (match.getPlayers() / 2 + 1);
		int upperBound = fTeam ? (match.getPlayers() / 2 + 1) : (match.getPlayers() + 1);
		
		pl.setSlot(-1);
		
		List<Integer> usedSlots = new ArrayList<>();
		List<String> hijackers = new ArrayList<>();
		
		if(isTeamGame())
			for(Player p : team.getPlayers())
				if(playersInRoom.contains(p.getName().replaceAll(" ", "_")) && p.getSlot() != -1)
					usedSlots.add(p.getSlot());
		
		for(; i < upperBound; i++){
			if(hijackedSlots.containsKey(i)){
				String hijackingPlayer = hijackedSlots.get(i);
				
				Player hijackPl = findPlayer(hijackingPlayer);
				
				if(hijack){
					roomSize++;

					sendMessage("!mp size " + roomSize);
					
					if(hijackPl == null) sendMessage("!mp move " + hijackingPlayer + " " + roomSize);
					else sendMessage("!mp move " + hijackPl.getIRCTag() + " " + roomSize);
					
					hijackedSlots.remove(i);
					hijackers.add(hijackingPlayer);
				}else{
					playersInRoom.add(hijackingPlayer);
					
					hijackedSlots.remove(i);
					if(!assignSlotAndTeam(hijackingPlayer, true)){
						playersInRoom.remove(hijackingPlayer);
						
						if(hijackPl == null) sendMessage("!mp kick " + hijackingPlayer);
						else sendMessage("!mp kick " + hijackPl.getIRCTag());
					}else{
						usedSlots.clear();
						
						for(Player p : team.getPlayers())
							if(playersInRoom.contains(p.getName().replaceAll(" ", "_")) && p.getSlot() != -1)
								usedSlots.add(p.getSlot());
					}	
				}
			}
			
			if(!usedSlots.contains(i)){
				/*if(match.getTournament().isUsingConfirms()){
					try{
						if(!RemotePatyServerUtils.isConfirmed(pl.getName(), 
								RemotePatyServerUtils.fetchTournamentId(match.getTournament().getName()))){
							sendMessage(pl.getName() + " is not confirmed, to join the lobby he/she needs to confirm via SmtBot!");
							pmUser(pl.getName(), "You are not confirmed! Please message SmtBot with !confirm " + match.getTournament().getName() +
							" " + team.getServerTeamID() + "!");
							playersInRoom.remove(player);
							return false;
						}
					}catch(Exception e){
						Log.logger.log(Level.SEVERE, e.getMessage(), e);
					}
				}*/
				
				pl.setSlot(i);
				sendMessage("!mp move " + pl.getIRCTag() + " " + i);
				sendMessage("!mp team " + pl.getIRCTag() + " " + color);
				
				int lower = match.getTournament().getLowerRankBound();
				int upper = match.getTournament().getUpperRankBound();
				
				if(lower > upper){
					int temp = upper;
					
					upper = lower;
					lower = temp;
				}
				
				int rank = -1;
				
				if(lower != upper && lower >= 1 && upper >= 1 && !playersRankChecked.contains(pl)){			
					rank = Utils.getOsuPlayerRank(pl.getName(), match.getTournament().getMode());
					
					if(rank != -1 && (rank < lower || rank > upper)){
						sendMessage(pl.getName() + "'s rank is out of range. His rank is " + Utils.veryLongNumberDisplay(rank) + 
								   " while the range is " + Utils.veryLongNumberDisplay(lower) + " to " + Utils.veryLongNumberDisplay(upper) + "!");
						sendMessage("!mp kick " + pl.getIRCTag());
						playersInRoom.remove(player);
						
						pmUser(player, "You were kicked because your rank is out of range. You are #" + Utils.veryLongNumberDisplay(rank) +
									   " while the range is " + Utils.veryLongNumberDisplay(lower) + " to " + Utils.veryLongNumberDisplay(upper) + "!");
						
						advanceQueue(player.replaceAll(" ", "_"), hijackers);
						pl.setSlot(-1);
						return false;
					}
					
					playersRankChecked.add(pl);
				}else if(match.getTournament().isUsingMapStats()){
					rank = Utils.getOsuPlayerRank(pl.getName(), match.getTournament().getMode());
				}
				
				if(rank > 0){
					pl.setRank(rank);
				}
				
				if(isCaptain(player.replaceAll(" ", "_")))
					waitingForCaptains--;
				
				if(waitingForCaptains == 0 && playersInRoom.size() < 2)
					waitingForCaptains++;
				else if(waitingForCaptains == 0){
					advanceQueue(player.replaceAll(" ", "_"), hijackers);
					mapSelection(1);
					return true;
				}
				
				advanceQueue(player.replaceAll(" ", "_"), hijackers);
				return true;
			}
		}
		
		advanceQueue(player.replaceAll(" ", "_"), hijackers);
		return false;
	}
}