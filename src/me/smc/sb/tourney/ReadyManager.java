package me.smc.sb.tourney;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.stream.Collectors;

import me.smc.sb.irccommands.ContestCommand;
import me.smc.sb.irccommands.SelectMapCommand;
import me.smc.sb.irccommands.SkipRematchCommand;
import me.smc.sb.tourney.PickedMap;
import me.smc.sb.tourney.GameState;
import me.smc.sb.tourney.Player;
import me.smc.sb.tracking.Mods;
import me.smc.sb.utils.Log;
import me.smc.sb.utils.Utils;

public class ReadyManager{
	
	private Game game;
	protected long startTime;
	
	public ReadyManager(Game game){
		this.game = game;
	}

	public void startReadyWait(){
		if(game.state.eq(GameState.PAUSED)) return;
		
		game.state = GameState.READYING;
		game.selectionManager.clearPickTimer();
		String message = "Waiting for players to ready up...";
		
		if(game.match.getTournament().getBool("scoreV2")) message = "Waiting for players to ready up, fallback scores will not count!";
		
		game.selectionManager.selectionStartTime = System.currentTimeMillis();
		game.selectionManager.pickTimer(game.match.getTournament().getInt("readyWaitTime"));
		game.messageUpdater(0, game.match.getTournament().getInt("readyWaitTime"), 
							message, "Force starting after the timer. You may change map if needed.");
	}
	
	public void playersReady(){
		if(!game.state.eq(GameState.READYING)) return;
		
		game.messageUpdater.cancel();
		game.selectionManager.clearPickTimer();
		boolean full = game.lobbyManager.getCurrentPlayers().size() == game.match.getMatchSize();
		
		// if lobby is full and map is selected normally, we verify lobby
		if(full && game.selectionManager.map != null){
			// in case the selected map somehow wasn't set into the lobby
			if(game.selectionManager.lobbyMap == null ||
			  !game.selectionManager.lobbyMap.getURL().equalsIgnoreCase(game.selectionManager.map.getURL())){
				game.selectionManager.changeMap(game.selectionManager.map);
				Utils.sleep(2500);
				
				return;
			}
			
			if(game.selectionManager.lobbyUpdater != null) game.selectionManager.lobbyUpdater.cancel();
			if(game.messageUpdater != null) game.messageUpdater.cancel();
			
			SelectMapCommand.pickingTeams.remove(game.nextTeam);
			
			verify();
		}else if(game.selectionManager.map != null){
			// should check lobby and make sure it's up to date
		}
	}
	
	public void finalModCheck(){
		if(game.state.eq(GameState.VERIFYING) && game.lobbyManager.getCurrentPlayers().size() == game.match.getMatchSize()){
			if(game.selectionManager.map.getCategory() == 1 && game.selectionManager.warmupsLeft <= 0){
				int fTeamModCount = 0, sTeamModCount = 0;
				
				for(Player player : game.firstTeam.getCurrentPlayers())
					if(player.hasMod()) fTeamModCount++;
				
				for(Player player : game.secondTeam.getCurrentPlayers())
					if(player.hasMod()) sTeamModCount++;
				
				int countNeeded = (int) Math.ceil((double) game.match.getMatchSize() / 4.0);
				
				if(fTeamModCount < countNeeded || sTeamModCount < countNeeded){
					if(game.match.getTournament().getInt("type") == 0)
						game.banchoHandle.sendMessage("You need " + countNeeded + " mod user" + 
													  (countNeeded <= 1 ? "" : "s") + " per team!", false);
					else game.banchoHandle.sendMessage("You both need to have a mod!", false);
					
					game.banchoHandle.sendMessage("!mp map " + game.selectionManager.map.getBeatmapID() + " " + 
												  game.match.getTournament().getInt("mode"), false);
					startReadyWait();
				}else startMatch(0, false);
			}else startMatch(5, false);
		}else startReadyWait();
	}
	
	public void startMatch(int delay, boolean backupCall){	
		if(game.state.eq(GameState.PAUSED)) return;
		
		if(!backupCall){
			Timer t = new Timer();
			t.schedule(new TimerTask(){
				public void run(){
					if(game.lobbyManager.getCurrentPlayers().size() == game.match.getMatchSize() && 
					   !game.state.eq(GameState.PLAYING) && !game.state.eq(GameState.PRESTART))
						startMatch(0, true);
				}
			}, delay * 1000 + 3000);	
		}
		
		if(game.roomSize > game.match.getMatchSize()){
			game.roomSize = game.match.getMatchSize();
			game.banchoHandle.sendMessage("!mp size " + game.roomSize, false);
		}
		
		onMatchStart();
		
		if(delay != 0){
			game.state = GameState.PRESTART;
			game.banchoHandle.sendMessage("!mp start " + delay, false);
		}else{
			game.state = GameState.PLAYING;
			game.banchoHandle.sendMessage("!mp start", false);
		}
		
		game.feed.updateDiscord();
		game.feed.updateTwitch("The match has begun!");
	}
	
	public void onMatchStart(){
		game.selectionManager.clearPickTimer();
		SkipRematchCommand.gamesAllowedToSkip.remove(game);
		ContestCommand.gamesAllowedToContest.remove(game);
		game.resultManager.skipRematchState = 0;
		game.resultManager.contestState = 0;
		game.selectionManager.selectionStartTime = 0;
		
		if(game.messageUpdater != null) game.messageUpdater.cancel();
		if(game.selectionManager.lobbyUpdater != null) game.selectionManager.lobbyUpdater.cancel();
		
		try{
			PickedMap picked = new PickedMap(game.selectionManager.map, game.nextTeam.getTeam(), game.match, game.selectionManager.warmupsLeft != 0);
			
			if(!game.selectionManager.getPicks().stream().anyMatch(p -> p != null && p.getMap() != null && p.getMap().export() != null &&
																		p.getMap().export().equals(picked.getMap().export())))
				game.getNextTeam().addPick(picked);
		}catch(Exception e){
			Log.logger.log(Level.SEVERE, e.getMessage(), e);
		}
		
		switchPlaying(true, false);
		
		game.resultManager.results.clear();
		game.resultManager.rematching = false;
	}
	
	public void matchStarted(){
		startTime = System.currentTimeMillis();
		
		if(!game.state.eq(GameState.PLAYING)){
			if(!game.state.eq(GameState.PRESTART))
				game.banchoHandle.sendMessage("!mp settings", false);
			
			game.state = GameState.PLAYING;
		}
	}
	
	public void verify(){
		game.state = GameState.VERIFYING;
		
		if(game.selectionManager.map.getCategory() == 1 && game.selectionManager.warmupsLeft <= 0){
			LinkedList<Player> tempList = new LinkedList<>(game.firstTeam.getTeam().getPlayers());
			tempList.addAll(game.secondTeam.getTeam().getPlayers());
			
			for(Player pl : tempList){
				pl.setHasMod(false);
				pl.setModMultiplier(1);
				pl.clearMods();
				pl.setPlaying(false);
				pl.setVerified(false);
				pl.setSubmitted(false);
			}
		}
		
		game.banchoHandle.sendMessage("!mp settings", true);
		
		Timer t = new Timer();
		t.schedule(new TimerTask(){
			public void run(){
				if(game.state.eq(GameState.VERIFYING) && getVerifiedPlayers().size() == 0)
					game.banchoHandle.sendMessage("!mp settings", true);
			}
		}, 2500);
		
		return;
	}
	
	public void verifyPlayer(String message){
		int slot = Utils.stringToInt(message.split(" ")[1]);
		boolean hasMod = false;
		double modMultiplier = 1;
		List<Mods> playerMods = new ArrayList<>();
		Map map = game.selectionManager.map;
		String[] sBracketSplit = message.split("\\[");
		String fullEndSection = sBracketSplit[sBracketSplit.length - 1].split("\\]")[0];
		String teamColor = fullEndSection.split("Team ")[1].split(" ")[0];
		
		if(fullEndSection.startsWith("Host \\/")) fullEndSection = fullEndSection.replace("Host / ", "");
		
		String mods = fullEndSection.contains("/") ? fullEndSection.split("\\/")[1].substring(1) : "";
		
		// if we need to make sure players are using correct mods
		if(mods.length() > 0 && (map.getCategory() == 1 || map.getCategory() == 5) && game.selectionManager.warmupsLeft <= 0){
			boolean validMod = true;
			List<String> validFreemodMods = game.match.getTournament().getStringList("allowedFreemodMods");
			String displayAllowedMods = "";
			
			for(String allowed : validFreemodMods)
				displayAllowedMods += ", " + allowed;
			
			displayAllowedMods = displayAllowedMods.substring(2);
			
			for(String mod : mods.split(", ")){
				if(!validFreemodMods.contains(mod)){
					validMod = false;
					break;
				}
				
				modMultiplier *= getModMultiplier(mod, game.match.getTournament().getBool("scoreV2"));
				
				try{
					playerMods.add(Mods.getMod(Mods.getMods(mod)));
				}catch(Exception ex){}
			}
			
			if(!validMod){					
				game.banchoHandle.sendMessage("You can only use " + displayAllowedMods, false);
				game.banchoHandle.sendMessage("!mp map " + map.getBeatmapID() + " " + game.match.getTournament().getInt("mode"), false);
				startReadyWait();
				
				return;
			}else if(validMod) hasMod = true;
		}
		
		message = Utils.removeExcessiveSpaces(message);
		
		String[] spaceSplit = message.split(" ");
		String beforeLink = "";
		String playerName = "";
		int count = 0;
		
		for(String arg : spaceSplit){
			if(arg.contains("osu.ppy.sh")) break;
			
			beforeLink += arg + " ";
			count++;
		}
		
		if(beforeLink.toLowerCase().contains("no map")){
			game.banchoHandle.sendMessage("!mp map " + map.getBeatmapID() + " " + game.match.getTournament().getInt("mode"), false);
			startReadyWait();
			
			return;
		}

		for(int i = count + 1; i < spaceSplit.length; i++){
			if(spaceSplit[i].equalsIgnoreCase("[Team") || spaceSplit[i].equalsIgnoreCase("[Host"))
				break;
			
			playerName += spaceSplit[i] + "_";
		}
		
		playerName = playerName.substring(0, playerName.length() - 1);
		Player player = game.lobbyManager.findPlayer(playerName);
		PlayingTeam team = game.lobbyManager.findTeam(player);
		
		if(player == null || team == null){
			game.banchoHandle.kickPlayer(playerName, playerName + " is not on a team!");
			game.banchoHandle.sendMessage("!mp map " + map.getBeatmapID() + " " + game.match.getTournament().getInt("mode"), false);
			startReadyWait();
			
			return;
		}
		
		if(map.getCategory() > 1 && map.getCategory() != 5)
			modMultiplier = getModMultiplier(game.selectionManager.getMod(map), game.match.getTournament().getBool("scoreV2"));
		
		if(map.getCategory() == 2) playerMods.add(Mods.Hidden);
		else if(map.getCategory() == 3) playerMods.add(Mods.HardRock);
		else if(map.getCategory() == 4) playerMods.add(Mods.DoubleTime);
		
		player.setHasMod(hasMod);
		player.setModMultiplier(modMultiplier);
		player.setMods(playerMods);
		player.setSlot(slot);
		player.setVerified(true);
		
		boolean fTeam = game.lobbyManager.isOnFirstTeam(player);
		
		if(fTeam && (!teamColor.equalsIgnoreCase("Blue") && !teamColor.contains("Blue")))
			game.banchoHandle.sendMessage("!mp team " + player.getIRCTag() + " blue", false);
		else if(!fTeam && (!teamColor.equalsIgnoreCase("Red") && !teamColor.contains("Red")))
			game.banchoHandle.sendMessage("!mp team " + player.getIRCTag() + " red", false);

		if(getVerifiedPlayers().size() >= game.match.getMatchSize()){
			if(lobbySwapFixing()) finalModCheck();
			else{
				game.banchoHandle.sendMessage("!mp map " + map.getBeatmapID() + " " + game.match.getTournament().getInt("mode"), false);
				startReadyWait();
				
				return;
			}
		}
	}
	
	private boolean lobbySwapFixing(){
		List<Player> fTeamList = game.firstTeam.getCurrentPlayers();
		List<Player> sTeamList = game.secondTeam.getCurrentPlayers();
		List<Player> fTeamSwaps = new ArrayList<>();
		List<Player> sTeamSwaps = new ArrayList<>();
		
		// remove excess players
		for(Player player : fTeamList){
			if(fTeamList.size() > game.match.getMatchSize() / 2){
				game.banchoHandle.kickPlayer(player.getIRCTag(), "");
			}else if(player.getSlot() > game.match.getMatchSize() / 2)
				fTeamSwaps.add(player);
		}
		
		for(Player player : sTeamList){
			if(sTeamList.size() > game.match.getMatchSize() / 2){
				game.banchoHandle.kickPlayer(player.getIRCTag(), "");
			}else if(player.getSlot() <= game.match.getMatchSize() / 2)
				sTeamSwaps.add(player);
		}
		
		// swap players around if needed
		if(!fTeamSwaps.isEmpty() || !sTeamSwaps.isEmpty()){
			while(fTeamSwaps.size() + sTeamSwaps.size() > 0){
				Player fPlayer = null;
				Player sPlayer = null;
				
				if(!fTeamSwaps.isEmpty()) fPlayer = fTeamSwaps.get(0);
				if(!sTeamSwaps.isEmpty()) sPlayer = sTeamSwaps.get(0);
				
				if(fPlayer != null && sPlayer == null){
					game.banchoHandle.kickPlayer(fPlayer.getIRCTag(), "");
					
					return false;
				}
				
				if(fPlayer == null && sPlayer != null){
					game.banchoHandle.kickPlayer(sPlayer.getIRCTag(), "");
					
					return false;
				}
				
				game.roomSize += 1;
				int fSlot = sPlayer.getSlot();
				int sSlot = fPlayer.getSlot();
				
				fPlayer.setSlot(fSlot);
				sPlayer.setSlot(sSlot);
				
				game.banchoHandle.sendMessage("!mp size " + game.roomSize, false);
				game.banchoHandle.sendMessage("!mp move " + sPlayer.getIRCTag() + " " + game.roomSize, false);
				game.banchoHandle.sendMessage("!mp move " + fPlayer.getIRCTag() + " " + fSlot, false);
				game.banchoHandle.sendMessage("!mp move " + sPlayer.getIRCTag() + " " + sSlot, false);
				
				game.roomSize -= 1;
				game.banchoHandle.sendMessage("!mp size " + game.roomSize, false);
				
				fTeamSwaps.remove(fPlayer);
				sTeamSwaps.remove(sPlayer);
			}
		}
		
		return true;
	}
	
	public List<Player> getVerifiedPlayers(){
		LinkedList<Player> tempList = new LinkedList<>(game.firstTeam.getTeam().getPlayers());
		tempList.addAll(game.secondTeam.getTeam().getPlayers());
		
		return tempList.stream().filter(p -> p.isVerified()).collect(Collectors.toList());
	}
	
	public void switchPlaying(boolean playing, boolean full){
		if(full){
			LinkedList<Player> tempList = new LinkedList<>(game.firstTeam.getTeam().getPlayers());
			tempList.addAll(game.secondTeam.getTeam().getPlayers());
			
			for(Player player : tempList){
				player.setPlaying(playing);
				player.setVerified(false);
				player.setSubmitted(false);
			}
		}else
			for(Player player : game.lobbyManager.getCurrentPlayers()){
				player.setPlaying(playing);
				player.setVerified(false);
				player.setSubmitted(false);
			}
	}
	
	public static double getModMultiplier(String mod, boolean scorev2){
		switch(mod.toLowerCase()){
			case "hidden": case "hd": return 1.06;
			case "hardrock": case "hr": return scorev2 ? 1.1 : 1.06;
			case "doubletime": case "nightcore": case "dt": case "nc": return scorev2 ? 1.2 : 1.12;
			case "flashlight": case "fl": return 1.12;
			case "easy": case "ez": return 0.5;
			case "halftime": case "ht": return 0.3;
			case "nofail": case "nf": return 0.5;
			case "suddendeath": case "sd": case "perfect": case "pf": case "none": return 1;
			case "spunout": case "so": return 0.9;
			default: return 0;
		}
	}
}
