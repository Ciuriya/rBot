package me.smc.sb.multi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;

import org.json.JSONObject;
import org.pircbotx.Channel;

import me.smc.sb.irccommands.BanMapCommand;
import me.smc.sb.irccommands.InvitePlayerCommand;
import me.smc.sb.irccommands.JoinMatchCommand;
import me.smc.sb.irccommands.PassTurnCommand;
import me.smc.sb.irccommands.RandomCommand;
import me.smc.sb.irccommands.SelectMapCommand;
import me.smc.sb.listeners.IRCChatListener;
import me.smc.sb.main.Main;
import me.smc.sb.utils.Log;
import me.smc.sb.utils.Utils;

public class Game{

	private Match match;
	private String multiChannel;
	private String mpLink;
	private int waitingForCaptains = 2;
	private int fTeamPoints = 0, sTeamPoints = 0;
	private int warmupsLeft = 2;
	private int rollsLeft = 2;
	private int previousRoll = 0;
	private int playersChecked = 0;
	private int roomSize = 0;
	private boolean mapSelected = false;
	private boolean fTeamFirst = true;
	private boolean validMods = true;
	private GameState state;
	private int rematchesAllowed = 1;
	private List<String> invitesSent;
	private List<String> playersInRoom;
	private List<String> captains;
	private List<String> joinQueue;
	private java.util.Map<Integer, String> hijackedSlots;
	private List<Map> bans;
	private List<Map> mapsPicked;
	private List<Player> playersSwapped;
	private java.util.Map<Player, Integer> verifyingSlots;
	private LinkedList<String> banchoFeedback;
	private Team selectingTeam;
	private Team banningTeam;
	private Map map;
	private Map previousMap;
	private Timer mapUpdater;
	private Timer messageUpdater;
	
	public Game(Match match){
		this.match = match;
		this.multiChannel = "";
		this.mpLink = "";
		this.invitesSent = new ArrayList<>();
		this.playersInRoom = new ArrayList<>();
		this.banchoFeedback = new LinkedList<>();
		this.bans = new ArrayList<>();
		this.mapsPicked = new ArrayList<>();
		this.captains = new ArrayList<>();
		this.joinQueue = new ArrayList<>();
		this.playersSwapped = new ArrayList<>();
		this.hijackedSlots = new HashMap<>();
		this.verifyingSlots = new HashMap<>();
		this.state = GameState.WAITING;
		this.match.setGame(this);
		
		setupLobby();
	}
	
	public void start(String multiChannel, String mpLink){
		this.multiChannel = multiChannel;
		this.mpLink = mpLink;
		
		IRCChatListener.gameCreatePMs.remove(multiChannel.replace("#mp_", "") + "|" + match.getLobbyName());
		IRCChatListener.gamesListening.put(multiChannel, this);
		
		sendMessage("!mp lock");
		sendMessage("!mp size " + match.getPlayers());
		sendMessage("!mp set 2 " + (match.getTournament().isScoreV2() ? "4" : "0"));
		
		String admins = "";
		
		if(!match.getMatchAdmins().isEmpty()){
			for(String admin : match.getMatchAdmins())
				admins += admin.replaceAll(" ", "_") + ", ";
			admins = admins.substring(0, admins.length() - 2);
			
			sendMessage("!mp addref " + admins);
		}
		
		roomSize = match.getPlayers();
		
		LinkedList<Player> fullList = new LinkedList<Player>(match.getFirstTeam().getPlayers());
		
		fullList.addAll(match.getSecondTeam().getPlayers());
		
		for(Player pl : fullList)
			pl.setSlot(-1);
		
		captains.add(match.getFirstTeam().getPlayers().get(0).getName().replaceAll(" ", "_"));
		captains.add(match.getSecondTeam().getPlayers().get(0).getName().replaceAll(" ", "_"));
		
		for(String player : captains)
			invitePlayer(player);
		
		scheduleNextCaptainInvite();
		
		Timer timeout = new Timer();
		timeout.schedule(new TimerTask(){
			public void run(){
				if(waitingForCaptains > 0){
					if(!playersInRoom.isEmpty())
						if(teamToBoolean(findTeam(playersInRoom.get(0)))){
							fTeamPoints++;
						}else sTeamPoints++;
					
					stop();
				}
			}
		}, 600000);
	}
	
	private void scheduleNextCaptainInvite(){
		Timer captainFallback = new Timer();
		captainFallback.schedule(new TimerTask(){
			public void run(){
				if(!playersInRoom.isEmpty()){
					int fTeamCaptains = 0, sTeamCaptains = 0;
					
					for(String player : playersInRoom)
						if(captains.contains(player.replaceAll(" ", "_"))){
							Team team = findTeam(player);	
							if(teamToBoolean(team)) fTeamCaptains++;
							else sTeamCaptains++;
						}	
					
					Team missingTeam = null;
					
					if(fTeamCaptains == 0 && sTeamCaptains == 0){
						addNextCaptain(match.getFirstTeam());
						addNextCaptain(match.getSecondTeam());
						scheduleNextCaptainInvite();
						return;
					}else if(fTeamCaptains == 0) missingTeam = match.getFirstTeam();
					else if(sTeamCaptains == 0) missingTeam = match.getSecondTeam();
					else return;
					
					addNextCaptain(missingTeam);
					scheduleNextCaptainInvite();
				}
			}
		}, 60000);
	}
	
	private void addNextCaptain(Team team){
		for(Player pl : team.getPlayers())
			if(!captains.contains(pl.getName().replaceAll(" ", "_")) && !playersInRoom.contains(pl.getName().replaceAll(" ", "_"))){
				captains.add(pl.getName().replaceAll(" ", "_"));
				invitePlayer(pl.getName().replaceAll(" ", "_"));
				return;
			}
		
		for(Player pl : team.getPlayers())
			if(captains.contains(pl.getName().replaceAll(" ", "_")) && pl.getSlot() == -1 && waitingForCaptains > 0)
				sendInviteMessages(pl.getName().replaceAll(" ", "_"));
	}
	
	private void messageUpdater(String...messages){
		messageUpdater(0, messages);
	}
	
	private void messageUpdater(int delay, String...messages){
		if(messageUpdater != null) messageUpdater.cancel();
		if(messages == null || messages.length <= 0) return;
		
		messageUpdater = new Timer();
		messageUpdater.scheduleAtFixedRate(new TimerTask(){
			public void run(){
				for(String message : messages)
					sendMessage(message);
			}
		}, delay * 1000, 60000);
	}
	
	public void resize(){
		sendMessage("!mp size " + match.getPlayers());
		//do some shit here I guess?
	}
	
	public void setScores(int fTeamScore, int sTeamScore){
		this.fTeamPoints = fTeamScore;
		this.sTeamPoints = sTeamScore;
	}
	
	private void mapSelection(int part){
		switch(part){
			case 1:
				waitingForCaptains = -1;
				messageUpdater("Use !invite <player name> to invite your teammates or simply invite them through osu!.",
							   "Both captains, please use !random to settle which team goes first.");
				
				InvitePlayerCommand.allowedInviters.put(match.getFirstTeam(), this);
				InvitePlayerCommand.allowedInviters.put(match.getSecondTeam(), this);
				
				RandomCommand.waitingForRolls.put(match.getFirstTeam(), this);
				RandomCommand.waitingForRolls.put(match.getSecondTeam(), this);
				break;
			case 2:
				selectingTeam = findNextTeamToPick();
				map = null;
				
				messageUpdater(selectingTeam.getTeamName() + ", please pick a warmup map using !select <map url>");
				SelectMapCommand.gamesInSelection.add(this);
				
				startMapUpdater();
				break;
			case 3: 
				banningTeam = findNextTeamToBan();
				
				if(bans.size() >= 4) mapSelection(4);
				else{
					BanMapCommand.banningTeams.put(banningTeam, this);
					messageUpdater(banningTeam.getTeamName() + ", please ban a map using !ban <map url> or !ban <map #>" +
							   (match.getMapPool().getSheetUrl().length() > 0 ? " [" + match.getMapPool().getSheetUrl() + 
							   " You can find the maps here]" : ""));
				}
				
				break;
			case 4:
				selectingTeam = findNextTeamToPick();
				map = null;
				
				messageUpdater(selectingTeam.getTeamName() + ", please pick a map using !select <map url> or !select <map #>" +
						   (match.getMapPool().getSheetUrl().length() > 0 ? " [" + match.getMapPool().getSheetUrl() + 
						   " You can find the maps here]" : ""));
				
				SelectMapCommand.gamesInSelection.add(this);
				
				startMapUpdater();
				break;
			default: break;
		}
	}
	
	private void startMapUpdater(){
		previousMap = null;
		
		mapUpdater = new Timer();
		mapUpdater.scheduleAtFixedRate(new TimerTask(){
			public void run(){
				if(map != null){
					if(previousMap != null && previousMap.getURL().equalsIgnoreCase(map.getURL())) return;
					
					previousMap = map;
					changeMap(map);
				}
			}
		}, 10000, 10000);
	}
	
	private void changeMap(Map map){
		sendMessage("!mp map " + map.getBeatmapID() + " 0");
		sendMessage("!mp mods " + getMod(map));
		
		this.map = map;
	}
	
	private String getMod(Map map){
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
	
	public void acceptRoll(String player, int roll){
		rollsLeft--;
		
		if(rollsLeft == 0){
			boolean fTeam = false;
			for(Player p : match.getFirstTeam().getPlayers())
				if(p.getName().replaceAll(" ", "_").equalsIgnoreCase(player)){
					fTeam = true;
					break;
				}
			
			if(roll == previousRoll){
				sendMessage("Rolls were equal! Please reroll using !random.");
				RandomCommand.waitingForRolls.put(match.getFirstTeam(), this);
				RandomCommand.waitingForRolls.put(match.getSecondTeam(), this);
				rollsLeft = 2;
				return;
			}
			
			if(roll > previousRoll) fTeamFirst = fTeam;
			else fTeamFirst = !fTeam;
			
			PassTurnCommand.passingTeams.put(fTeam ? match.getFirstTeam() : match.getSecondTeam(), this);
			
			sendMessage((fTeam ? match.getFirstTeam().getTeamName() : match.getSecondTeam().getTeamName()) + 
					    ", use !pass within the next 10 seconds to let the other team start instead!");
			
			Timer t = new Timer();
			t.schedule(new TimerTask(){
				public void run(){
					sendMessage("This match is reffed by a bot! If you have suggestions or have found a bug, please report in our [http://discord.gg/0f3XpcqmwGkNseMR discord group] or to Smc. Thank you!");
					mapSelection(2);
				}
			}, 10000);
		}else previousRoll = roll;
	}
	
	public void passFirstTurn(){
		fTeamFirst = !fTeamFirst;
	}
	
	public void stop(){
		waitingForCaptains = 0;
		
		for(String player : invitesSent)
			JoinMatchCommand.gameInvites.remove(player);
		
		InvitePlayerCommand.allowedInviters.remove(match.getFirstTeam());
		InvitePlayerCommand.allowedInviters.remove(match.getSecondTeam());
		
		IRCChatListener.gamesListening.remove(multiChannel);
		
		SelectMapCommand.gamesInSelection.remove(this);
		
		sendMessage("!mp close");
		
		String gameEndedMsg = "Game ended: " + match.getFirstTeam().getTeamName() + " (" + fTeamPoints + ") vs (" +
	   						  sTeamPoints + ") " + match.getSecondTeam().getTeamName() + " - " + mpLink;
		
		for(String admin : match.getMatchAdmins())
			pmUser(admin, gameEndedMsg);

		Log.logger.log(Level.INFO, gameEndedMsg);
		
		if(messageUpdater != null) messageUpdater.cancel();
		invitesSent.clear();
		playersInRoom.clear();
		banchoFeedback.clear();
		bans.clear();
		mapsPicked.clear();
		captains.clear();
		joinQueue.clear();
		playersSwapped.clear();
		hijackedSlots.clear();
		verifyingSlots.clear();
		banningTeam = null;
		fTeamFirst = false;
		fTeamPoints = 0;
		map = null;
		mapSelected = false;
		mpLink = null;
		multiChannel = null;
		playersChecked = 0;
		previousMap = null;
		previousRoll = 0;
		rematchesAllowed = 0;
		rollsLeft = 0;
		roomSize = 0;
		selectingTeam = null;
		state = null;
		sTeamPoints = 0;
		validMods = false;
		waitingForCaptains = 0;
		warmupsLeft = 0;
		match.setGame(null);
		match = null;
		
		match.getTournament().removeMatch(match.getMatchNum());
	}
	
	private void pmUser(String user, String message){
		try{
			Main.ircBot.sendIRC().joinChannel(user);
		}catch(Exception ex){
			Log.logger.log(Level.INFO, "Could not talk to " + user + "!");
		}
		
		Main.ircBot.sendIRC().message(user, message);
	}
	
	private void setupLobby(){
		try{
			Main.ircBot.sendIRC().joinChannel("BanchoBot");
		}catch(Exception ex){
			Log.logger.log(Level.INFO, "Could not talk to BanchoBot!");
		}
		
		Main.ircBot.sendIRC().message("BanchoBot", "!mp make " + match.getLobbyName());
		
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
										if(match.getGame() != null && match.getLobbyName().equalsIgnoreCase(gamePM.split("\\|")[1]))
											match.getGame().start("#mp_" + unusedMP, "https://osu.ppy.sh/mp/" + unusedMP);
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
	
	public void handleMapSelect(String map, boolean select){
		Map selected = null;
		
		int num = 1;
		for(Map m : match.getMapPool().getMaps()){
			boolean chosen = false;
			if(num == Utils.stringToInt(map)) chosen = true;
			else if(m.getURL().equalsIgnoreCase(map)) chosen = true;
			
			if(chosen){
				if(select && warmupsLeft == 0){
					if(bans.contains(m)){sendMessage("This map is banned! Please choose something else."); return;}
					if(!checkMap(m)){sendMessage("This map was already picked once! Please choose something else."); return;}
				}
				
				if(m.getCategory() == 5){sendMessage("You cannot " + (select ? "select" : "ban") + " the tiebreaker!"); return;}
				
				selected = m;
				break;
			}
			
			num++;
		}
		
		if(!select && selected != null && !bans.contains(selected)){
			BanMapCommand.banningTeams.remove(banningTeam);
			bans.add(selected);
			
			JSONObject jsMap = Map.getMapInfo(selected.getBeatmapID());
			
			sendMessage(getMod(selected).replace("None", "Nomod") + " pick: " + jsMap.getString("artist") + " - " + 
					    jsMap.getString("title") + " [" + jsMap.getString("version") + "] was banned!");
			
			mapSelection(3);
			return;
		}
		
		if(warmupsLeft > 0 && selected != null && select){
			JSONObject jsMap = Map.getMapInfo(selected.getBeatmapID());
			int length = jsMap.getInt("total_length");
			if(length > 270){
				sendMessage("The warmup selected is too long! The maximum length is 4m30s.");
				return;
			}
		}
		
		if(selected != null && !mapSelected && select){
			mapSelected = true;
			this.map = selected;
			prepareReadyCheck();
			return;
		}else if(mapSelected && select && selected != null) this.map = selected;
		
		if(selected == null || !select) sendMessage("Invalid " + (select ? "selection!" : "ban!"));
	}
	
	private void prepareReadyCheck(){
		String message = "Waiting for all players to ready up...";
		
		if(match.getTournament().isScoreV2()) message = "Waiting for all players to ready up, make sure you are not using fallback as your score will not count!";
		
		messageUpdater(0, message, "If someone is on the wrong side/team, simply ready up and everything will fix itself! Please note that you can select another map if needed.");
	}
	
	private boolean checkMap(Map map){
		return !mapsPicked.contains(map);
	}
	
	public void handleBanchoFeedback(String message){
		if(message.contains("All players are ready")) readyCheck(true);
		else if(message.contains("left the game.")) playerLeft(message);
		else if(message.contains("The match has started!")){
			banchoFeedback.clear();
			state = GameState.PLAYING;
		}else if(message.contains("The match has finished!")) playEnded();
		else if(message.startsWith("Slot ") && state.equals(GameState.FIXING)) fixLobby(message);
		else if(message.startsWith("Slot ") && state.equals(GameState.VERIFYING)) verifyLobby(message);
		else if(message.contains("joined in")) acceptExternalInvite(message);
		else banchoFeedback.add(message);
	}
	
	private void readyCheck(boolean bancho){
		if(state.equals(GameState.CONFIRMING)) return;
		
		if(playersInRoom.size() == match.getPlayers() && mapSelected && state.equals(GameState.WAITING)){ 
			if(previousMap == null || !previousMap.getURL().equalsIgnoreCase(map.getURL())){
				changeMap(map);
				Utils.sleep(2500);
			}
			
			if(mapUpdater != null) mapUpdater.cancel();
			if(messageUpdater != null) messageUpdater.cancel();
			
			SelectMapCommand.gamesInSelection.remove(this);
			
			verifyLobby(null);
		}else if(mapSelected && state.equals(GameState.WAITING)) fixLobby(null);
		else if((state.equals(GameState.VERIFYING) || state.equals(GameState.FIXING)) && playersInRoom.size() == match.getPlayers()){
			finalModCheck();
		}else if(!bancho){
			state = GameState.WAITING;
			prepareReadyCheck();
			banchoFeedback.clear();
		}
	}
	
	private void verifyLobby(String message){
		if(message == null){
			state = GameState.VERIFYING;
			playersSwapped.clear();
			verifyingSlots.clear();
			
			verifyMods();
			
			sendMessage("!mp settings");
			
			Timer t = new Timer();
			t.schedule(new TimerTask(){
				public void run(){
					if(state.equals(GameState.VERIFYING))
						sendMessage("!mp settings");
				}
			}, 2500);
			return;
		}else fixPlayer(message, false);
	}
	
	private void fixLobby(String message){
		if(message == null){
			state = GameState.FIXING;
			messageUpdater.cancel();
			playersSwapped.clear();
			verifyingSlots.clear();

			verifyMods();
			
			sendMessage("!mp settings");
			playersInRoom.clear();
			
			Timer t = new Timer();
			t.schedule(new TimerTask(){
				public void run(){
					if(state.equals(GameState.FIXING))
						sendMessage("!mp settings");
				}
			}, 2500);
			return;
		}else fixPlayer(message, true);
	}
	
	private void fixPlayer(String message, boolean fixing){
		int slot = Utils.stringToInt(message.split(" ")[1]);
		boolean hasMod = false;
		
		if(message.endsWith("]") && map.getCategory() == 1 && warmupsLeft <= 0){
			String[] sBracketSplit = message.split("\\[");
			
			String mods = sBracketSplit[sBracketSplit.length - 1].split("\\]")[0];
			
			if(mods.split(" ").length <= 2){
				playersChecked++; 
			}else{
				boolean validMod = true;
				
				loop: for(String mod : mods.split(", ")){
					if(mod.contains("/") && mod.contains("Team")) mod = mod.split("\\/")[1].substring(1);
					switch(mod.toLowerCase()){
						case "hidden": case "hardrock": case "flashlight": break;
						default: validMod = false; break loop;
					}
				}
				
				if(!validMod){					
					sendMessage("You can only use HD, HR or FL! Make sure you do not have any other mods!");
					sendMessage("!mp map " + map.getBeatmapID() + " 0");
					validMods = false;
				}else hasMod = true;
			}
		}else if(map.getCategory() == 1 && warmupsLeft <= 0) playersChecked++;
		
		message = Utils.removeExcessiveSpaces(message);
		
		String[] spaceSplit = message.split(" ");
		
		int count = 0;
		for(String arg : spaceSplit){
			if(arg.contains("osu.ppy.sh")) break;
			count++;
		}
		
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
			return;
		}
		
		Player p = findPlayer(player);
		
		if(p != null){
			p.setHasMod(hasMod);
			playersChecked++;	
		}
		
		if(fixing) p.setSlot(slot);
		
		teamColor = spaceSplit[count + 1].replaceAll(" ", "");
				
		boolean fTeam = teamToBoolean(team);
		
		if(fTeam && (!teamColor.equalsIgnoreCase("Blue") && !teamColor.contains("Blue")))
			sendMessage("!mp team " + player + " blue");
		else if(!fTeam && (!teamColor.equalsIgnoreCase("Red") && !teamColor.contains("Red")))
			sendMessage("!mp team " + player + " red");
		
		verifyingSlots.put(p, slot);
		
		if(verifyingSlots.size() >= match.getPlayers()){
			lobbySwapFixing();
			readyCheck(false);
		}
	}
	
	private void lobbySwapFixing(){
		for(Player pl : verifyingSlots.keySet()){
			if(playersSwapped.contains(pl)) continue;
			
			Team team = findTeam(pl.getName().replaceAll(" ", "_"));
			
			boolean fTeam = teamToBoolean(team);
			
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
					sendMessage("!mp move " + p.getName().replaceAll(" ", "_") + " " + roomSize);
					sendMessage("!mp move " + pl.getName().replaceAll(" ", "_") + " " + slot);
					sendMessage("!mp move " + p.getName().replaceAll(" ", "_") + " " + newSlot);
					
					roomSize -= 1;
					sendMessage("!mp size " + roomSize);
					break;
				}
			}
		}
	}
	
	private void startMatch(int timer){	
		state = GameState.PLAYING;
		
		if(messageUpdater != null) messageUpdater.cancel();
		
		if(roomSize > match.getPlayers()){
			roomSize = match.getPlayers();
			sendMessage("!mp size " + roomSize);
		}
		
		mapsPicked.add(map);
		
		banchoFeedback.clear();
		
		if(timer != 0) sendMessage("!mp start " + timer);
		else sendMessage("!mp start");
		
		mapSelected = false;
	}
	
	private void verifyMods(){
		if(map.getCategory() == 1 && warmupsLeft <= 0){
			playersChecked = 0;
			LinkedList<Player> tempList = new LinkedList<Player>(match.getFirstTeam().getPlayers());
			tempList.addAll(match.getSecondTeam().getPlayers());
			
			for(Player pl : tempList) pl.setHasMod(false);
		}
	}
	
	private void finalModCheck(){
		if(playersChecked >= match.getPlayers() && validMods && map.getCategory() == 1 && warmupsLeft <= 0){
			playersChecked = 0;
			int fTeamModCount = 0, sTeamModCount = 0;
			
			for(Player pl : match.getFirstTeam().getPlayers())
				if(pl.hasMod()) fTeamModCount++;
			
			for(Player pl : match.getSecondTeam().getPlayers())
				if(pl.hasMod()) sTeamModCount++;
			
			int countNeeded = (int) Math.ceil((double) match.getPlayers() / 4.0);
			
			if(fTeamModCount < countNeeded || sTeamModCount < countNeeded){
				sendMessage("You need to have at least " + countNeeded + " mod users per team!");
				sendMessage("!mp map " + map.getBeatmapID() + " 0");
				state = GameState.PLAYING;
			}else startMatch(0);
		}else if(!validMods){
			validMods = true;
			state = GameState.PLAYING;
		}else startMatch(5);
	}
	
	private void playerLeft(String message){
		String player = message.replace(" left the game.", "").replaceAll(" ", "_");
		joinQueue.remove(player);
		playersInRoom.remove(player);
		
		for(Player pl : findTeam(player).getPlayers())
			if(pl.equals(player)) pl.setSlot(-1);
		
		if(!hijackedSlots.isEmpty()){
			int rSlot = -1;
			for(int slot : hijackedSlots.keySet())
				if(hijackedSlots.get(slot).equalsIgnoreCase(player)){
					rSlot = slot;
					break;
				}
			
			if(rSlot != -1) hijackedSlots.remove(rSlot);
		}
	}
	
	private void playEnded(){
		state = GameState.CONFIRMING;
		
		Timer t = new Timer();
		t.schedule(new TimerTask(){
			public void run(){
				state = GameState.WAITING;
				validMods = true;
				mapSelected = false;
				
				float fTeamScore = 0, sTeamScore = 0;
				List<String> fTeamPlayers = new ArrayList<>();
				List<String> sTeamPlayers = new ArrayList<>();
				
				for(String feedback : banchoFeedback){
					if(feedback.contains("finished playing")){
						String player = feedback.substring(0, feedback.indexOf(" finished"));
						
						if(fTeamPlayers.contains(player) || sTeamPlayers.contains(player)) continue;
						
						boolean fTeam = teamToBoolean(findTeam(player));
						
						if(fTeam) fTeamPlayers.add(player);
						else sTeamPlayers.add(player);
						
						if(feedback.split(",")[1].substring(1).split("\\)")[0].equalsIgnoreCase("PASSED")){
							int score = Utils.stringToInt(feedback.split("Score: ")[1].split(",")[0]);
							
							if(score > 1500000 && match.getTournament().isScoreV2()){
								sendMessage(player + " is on fallback, please use stable! His score is NOT counted for this pick.");
								continue;
							}
							
							if(fTeam) fTeamScore += score;
							else sTeamScore += score;
						}
					}
				}
				
				if(warmupsLeft > 0){
					if(fTeamScore == sTeamScore) sendMessage("Both teams have tied!");
					else
						sendMessage((fTeamScore > sTeamScore ? match.getFirstTeam().getTeamName() :
									match.getSecondTeam().getTeamName()) + " won by " + Math.abs(fTeamScore - sTeamScore) +
									" points!");
					warmupsLeft--;
					
					if(warmupsLeft == 0) mapSelection(3);
					else mapSelection(2);
				}else{
					if((int) Math.abs(fTeamPlayers.size() - sTeamPlayers.size()) != 0){	
						boolean fTeamDC = sTeamPlayers.size() > fTeamPlayers.size();
						
						if((fTeamDC && fTeamScore < sTeamScore) || (!fTeamDC && sTeamScore < fTeamScore)){
							if(rematchesAllowed > 0){
								rematchesAllowed--;
								
								sendMessage("Someone has disconnected, there will be a rematch!");
								mapSelected = true;
								banchoFeedback.clear();
								return;
							}	
						}
					}
					
					if(fTeamScore == sTeamScore){
						sendMessage("Both teams have tied, there will be a rematch!");
						mapSelected = true;
						banchoFeedback.clear();
						rematchesAllowed--;
						return;
					}else rematchesAllowed = 1;
					
					boolean fTeamWon = fTeamScore > sTeamScore;
					
					sendMessage((fTeamScore > sTeamScore ? match.getFirstTeam().getTeamName() :
						match.getSecondTeam().getTeamName()) + " won by " + Math.abs(fTeamScore - sTeamScore) +
						" points!");
					
					if(fTeamWon) fTeamPoints++;
					else sTeamPoints++;
					
					sendMessage(match.getFirstTeam().getTeamName() + " " + fTeamPoints + " | " +
								sTeamPoints + " " + match.getSecondTeam().getTeamName() +
								"      Best of " + match.getBestOf());
					
					if(fTeamPoints + sTeamPoints == match.getBestOf() - 1 && fTeamPoints == sTeamPoints){
						changeMap(match.getMapPool().findTiebreaker());
						
						mapSelected = true;
					}else if(fTeamPoints > Math.floor(match.getBestOf() / 2) || sTeamPoints > Math.floor(match.getBestOf() / 2)){
						sendMessage((fTeamWon ? match.getFirstTeam().getTeamName() : match.getSecondTeam().getTeamName()) +
									" has won this game!");
						sendMessage("The lobby is ending in 30 seconds, thanks for playing!");
						sendMessage("!mp timer");
						
						Timer time = new Timer();
						time.schedule(new TimerTask(){
							public void run(){
								stop();
							}
						}, 30000);
					}else mapSelection(4);
				}
				
				banchoFeedback.clear();
			}
		}, 10000);
	}
	
	private void acceptExternalInvite(String message){
		String player = message.split(" joined in")[0].replaceAll(" ", "_");
		if(playersInRoom.contains(player)) return;
		if(!verifyPlayer(player)){sendMessage("!mp kick " + player); return;}
		if(joinQueue.contains(player)) joinQueue.remove(player);
		
		if(joinQueue.isEmpty()){
			joinQueue.add(player);
			joinRoom(player, true);
		}else{
			int slot = Utils.stringToInt(message.split("joined in slot ")[1].split(" for team")[0]);
			hijackedSlots.put(slot, player);
		}
	}
	
	private void sendMessage(String command){
		Main.ircBot.sendIRC().message(multiChannel, command);
		Log.logger.log(Level.INFO, "IRC/Sent in " + multiChannel + ": " + command);
	}
	
	public void invitePlayer(String player){
		if(invitesSent.contains(player.replaceAll(" ", "_"))) return;
		
		invitesSent.add(player.replaceAll(" ", "_"));
		JoinMatchCommand.gameInvites.put(player.replaceAll(" ", "_"), this);
		
		sendInviteMessages(player.replaceAll(" ", "_"));
	}
	
	private void sendInviteMessages(String player){	
		pmUser(player, "You have been invited to join: " + match.getLobbyName());
		pmUser(player, "Reply with !join to join the game. Please note that this command is reusable.");
		
		Log.logger.log(Level.INFO, "Invited " + player + " to join " + multiChannel);
	}
	
	public void acceptInvite(String player){
		if(playersInRoom.contains(player.replaceAll(" ", "_")) || playersInRoom.size() >= match.getPlayers()) return;
		if(!verifyPlayer(player.replaceAll(" ", "_"))) return;
		
		if(joinQueue.contains(player.replaceAll(" ", "_"))) return;
		else joinQueue.add(player.replaceAll(" ", "_"));
		
		if(joinQueue.size() > 1) return;
		
		joinRoom(player.replaceAll(" ", "_"), false);
	}
	
	private void joinRoom(String player, boolean hijack){
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
					playersInRoom.remove(hijackingPlayer);
					sendMessage("!mp kick " + hijackingPlayer);
				}
			}
		}
	}
	
	private Team findTeam(String player){
		for(Player p : match.getFirstTeam().getPlayers())
			if(p.equals(player)) return match.getFirstTeam();
		
		for(Player p : match.getSecondTeam().getPlayers())
			if(p.equals(player)) return match.getSecondTeam();
		
		return null;
	}

	private boolean teamToBoolean(Team team){
		return team.getTeamName().equalsIgnoreCase(match.getFirstTeam().getTeamName());
	}
	
	private Player findPlayer(String player){
		LinkedList<Player> fullList = match.getFirstTeam().getPlayers();
		fullList.addAll(match.getSecondTeam().getPlayers());
		
		for(Player p : fullList)
			if(p.equals(player)) return p;
		
		return null;
	}
	
	private Team findNextTeamToPick(){
		int totalPicked = fTeamPoints + sTeamPoints;
		
		if(warmupsLeft == 1) totalPicked = 1;
		
		boolean fTeam = true;
		
		fTeam = (totalPicked + 2) % 2 == 0 ? fTeamFirst : !fTeamFirst; 
		
		return fTeam ? match.getFirstTeam() : match.getSecondTeam();
	}
	
	private Team findNextTeamToBan(){
		int totalBanned = bans.size();
		
		boolean fTeam = true;
		
		fTeam = (totalBanned + 2) % 2 == 0 ? fTeamFirst : !fTeamFirst;
		
		return fTeam ? match.getFirstTeam() : match.getSecondTeam();
	}
	
	private boolean verifyPlayer(String player){
		return findTeam(player) != null;
	}
	
	private boolean isCaptain(String player){
		Team playerTeam = null;
		
		for(String pl : captains)
			if(player.replaceAll(" ", "_").equalsIgnoreCase(pl)){
				playerTeam = findTeam(pl);
				break;
			}
		
		if(playerTeam != null){
			for(Player pl : playerTeam.getPlayers())
				if(!pl.equals(player)) return false;
			
			return true;
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
		
		for(Player p : team.getPlayers())
			if(playersInRoom.contains(p.getName().replaceAll(" ", "_")) && p.getSlot() != -1)
				usedSlots.add(p.getSlot());
		
		for(; i < upperBound; i++){
			if(hijackedSlots.containsKey(i)){
				String hijackingPlayer = hijackedSlots.get(i);
				
				if(hijack){
					roomSize++;
					sendMessage("!mp size " + roomSize);
					sendMessage("!mp move " + hijackingPlayer + " " + roomSize);
					hijackedSlots.remove(i);
					hijackers.add(hijackingPlayer);
				}else{
					playersInRoom.add(hijackingPlayer);
					
					hijackedSlots.remove(i);
					if(!assignSlotAndTeam(hijackingPlayer, true)){
						playersInRoom.remove(hijackingPlayer);
						sendMessage("!mp kick " + hijackingPlayer);
					}else{
						usedSlots.clear();
						
						for(Player p : team.getPlayers())
							if(playersInRoom.contains(p.getName().replaceAll(" ", "_")) && p.getSlot() != -1)
								usedSlots.add(p.getSlot());
					}	
				}
			}
			
			if(!usedSlots.contains(i)){
				sendMessage("!mp move " + player.replaceAll(" ", "_") + " " + i);
				sendMessage("!mp team " + player.replaceAll(" ", "_") + " " + color);
				pl.setSlot(i);
				
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