package me.smc.sb.tourney;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;

import org.json.JSONObject;

import me.smc.sb.irccommands.BanMapCommand;
import me.smc.sb.irccommands.ChangeWarmupModCommand;
import me.smc.sb.irccommands.ContestCommand;
import me.smc.sb.irccommands.SelectMapCommand;
import me.smc.sb.irccommands.SkipRematchCommand;
import me.smc.sb.irccommands.SkipWarmupCommand;
import me.smc.sb.tourney.Game;
import me.smc.sb.tourney.Map;
import me.smc.sb.tourney.GameState;
import me.smc.sb.pickstrategies.ModPickStrategy;
import me.smc.sb.pickstrategies.PickStrategy;
import me.smc.sb.utils.Log;
import me.smc.sb.utils.RemotePatyServerUtils;
import me.smc.sb.utils.Utils;

public class SelectionManager{

	private Game game;
	private PickStrategy strategy;
	protected Map map;
	protected Map lobbyMap;
	protected int warmupsLeft;
	protected int bansLeft;
	protected long selectionStartTime;
	protected String warmupMod;
	protected Timer lobbyUpdater;
	protected Timer pickTimer;
	
	public SelectionManager(Game game){
		this.game = game;
		this.strategy = PickStrategy.findStrategy(game.match.getTournament().get("pickStrategy"));
		this.map = null;
		this.lobbyMap = null;
		this.warmupsLeft = game.match.getTournament().getInt("warmupCount") * 2;
		this.bansLeft = game.match.getTournament().getInt("banCount") * 2;
	}
	
	public void selectWarmups(){
		if(game.state.eq(GameState.PAUSED)) return;
		
		clearPickTimer();
		game.state = GameState.SELECTING;
		
		if(game.match.getTournament().getBool("skipWarmups") || warmupsLeft == 0){
			selectBans();
			
			return;
		}
		
		map = null;
		
		if(!SelectMapCommand.pickingTeams.contains(game.nextTeam))
			SelectMapCommand.pickingTeams.add(game.nextTeam);
		
		if(!ChangeWarmupModCommand.gamesAllowedToChangeMod.contains(game))
			ChangeWarmupModCommand.gamesAllowedToChangeMod.add(game);
		
		if(!SkipWarmupCommand.gamesAllowedToSkip.contains(game))
			SkipWarmupCommand.gamesAllowedToSkip.add(game);
		
		selectionStartTime = 0;
		startLobbyUpdater();
		pickTimer(game.match.getTournament().getInt("pickWaitTime"));
		game.messageUpdater(0, game.match.getTournament().getInt("pickWaitTime"), 
							game.nextTeam.getTeam().getTeamName() + 
							", pick a warmup map using !select <map url> +MOD, mod is optional and only takes DT/HT", 
				  			"Use !warmupskip to skip this warmup.");
	}
	
	public void selectBans(){
		if(game.state.eq(GameState.PAUSED)) return;
		
		SelectMapCommand.pickingTeams.remove(game.nextTeam);
		ChangeWarmupModCommand.gamesAllowedToChangeMod.remove(game);
		SkipWarmupCommand.gamesAllowedToSkip.remove(game);
		
		clearPickTimer();
		
		game.state = GameState.BANNING;
		warmupsLeft = 0;
		map = null;
		selectionStartTime = 0;
		
		if(!game.match.getTournament().getBool("usingBans") || bansLeft == 0 || strategy instanceof ModPickStrategy){
			selectPicks();
			
			return;
		}
		
		if(!BanMapCommand.banningTeams.contains(game.nextTeam))
			BanMapCommand.banningTeams.add(game.nextTeam);
		
		pickTimer(game.match.getTournament().getInt("banWaitTime"));
		
		String poolSheet = "";
		
		try{
			poolSheet = game.match.getMapPool().getSheetUrl().length() > 0 ? " [" + game.match.getMapPool().getSheetUrl() + " You can find the maps here]" : "";
		}catch(Exception e){}
		
		game.messageUpdater(0, game.match.getTournament().getInt("banWaitTime"), 
						    game.nextTeam.getTeam().getTeamName() + ", ban a map using !ban <map url> or !ban <map #>" + poolSheet);
		
		if(getBans().size() > 0) game.getGameFeed().updateDiscord();
	}
	
	public void selectPicks(){
		if(game.state.eq(GameState.PAUSED)) return;
		
		clearPickTimer();
		
		game.state = GameState.SELECTING;
		warmupsLeft = 0;
		bansLeft = 0;
		map = null;
		
		if(!SelectMapCommand.pickingTeams.contains(game.nextTeam))
			SelectMapCommand.pickingTeams.add(game.nextTeam);
		
		selectionStartTime = 0;
		startLobbyUpdater();
		pickTimer(game.match.getTournament().getInt("pickWaitTime"));
		
		String selectUsage = "!select <map url> or !select <map #>";
		
		if(getPickStrategy() instanceof ModPickStrategy) selectUsage = "!select <mod>";
		
		String poolSheet = "";
		
		try{
			poolSheet = game.match.getMapPool().getSheetUrl().length() > 0 ? " [" + game.match.getMapPool().getSheetUrl() + " You can find the maps here]" : "";
		}catch(Exception e){}
		
		game.messageUpdater(0, game.match.getTournament().getInt("pickWaitTime"), 
							game.nextTeam.getTeam().getTeamName() + ", pick a map using " + selectUsage + poolSheet);
	}
	
	public void handleMapSelect(String map, boolean select, String mod){
		if(game.state.eq(GameState.PAUSED)) return;
		if(selectionStartTime != 0 && selectionStartTime + (long) (game.match.getTournament().getInt("readyWaitTime") / 2 * 1000) <= System.currentTimeMillis()) return;
		
		strategy.handleMapSelect(game, map, select, mod);
	}
	
	public boolean forceSelect(boolean revertScore, Map newMap){
		String indicator = game.match.getTournament().getInt("type") == 0 ? "teams" : "players";
		
		if(game.state.eq(GameState.ROLLING) || game.state.eq(GameState.WAITING)){
			game.banchoHandle.sendMessage("You cannot force select whilst " + indicator + " have not finished rolling!", false);
			
			return false;
		}else if(warmupsLeft != 0){
			game.banchoHandle.sendMessage("You cannot force select whilst " + indicator + " have not finished warmups yet!", false);
			
			return false;
		}else if(bansLeft > 0){
			game.banchoHandle.sendMessage("You cannot force select whilst " + indicator + " have not banned yet!", false);
			
			return false;
		}
		
		if(newMap != null) changeMap(newMap);
		else changeMap(game.getOppositeTeam(game.nextTeam).getPicks().getLast().getMap());
		
		game.resultManager.skipRematchState = 0;
		game.resultManager.contestState = 0;
		SkipRematchCommand.gamesAllowedToSkip.remove(game);
		ContestCommand.gamesAllowedToContest.remove(game);
		SelectMapCommand.pickingTeams.remove(game.nextTeam);
		
		if(lobbyUpdater != null) lobbyUpdater.cancel();
		if(game.messageUpdater != null) game.messageUpdater.cancel();
		
		if(revertScore && game.firstTeam.getPoints() + game.secondTeam.getPoints() > 0 && map != null){
			if(game.resultManager.lastWinner) game.firstTeam.removePoint();
			else game.secondTeam.removePoint();
			
			game.switchNextTeam();
			game.resultManager.updateScores(true, true);
			
			int mapId = game.match.getMapPool().getMapId(map);
			
			if(mapId != 0){
				int tourneyId = 0;
				
				try{
					tourneyId = RemotePatyServerUtils.fetchTournamentId(game.match.getTournament().get("name"));
				}catch(Exception e){
					Log.logger.log(Level.SEVERE, "Could not fetch tourney id", e);
				}
				
				if(tourneyId != 0){
					RemotePatyServerUtils.incrementMapValue(mapId, game.match.getMapPool().getPoolNum(), tourneyId, "pickcount", -1);
				}
			}
		}
		
		game.readyManager.switchPlaying(false, true);
		game.readyManager.startReadyWait();
		
		return true;
	}
	
	public void startLobbyUpdater(){
		if(lobbyUpdater != null) lobbyUpdater.cancel();
		
		lobbyUpdater = new Timer();
		lobbyUpdater.scheduleAtFixedRate(new TimerTask(){
			public void run(){
				if(selectionStartTime != 0 && selectionStartTime + (long) (game.match.getTournament().getInt("readyWaitTime") / 2 * 1000) <= System.currentTimeMillis()){
					lobbyUpdater.cancel();
					
					return;
				}
				
				if(map != null){
					if(lobbyMap != null && lobbyMap.getURL().equalsIgnoreCase(map.getURL())) return;
					
					lobbyMap = map;
					changeMap(map);
				}
			}
		}, 2500, 2500);
	}
	
	public void changeMap(Map map){
		game.banchoHandle.sendMessage("!mp map " + map.getBeatmapID() + " " + game.match.getTournament().getInt("mode"), false);
		
		if(warmupsLeft == 0)
			game.banchoHandle.sendMessage("!mp mods " + getMod(map), false);
		
		this.map = map;
		this.lobbyMap = map;
		
		try{
			String bloodcat = map.getBloodcatLink();
			
			if(bloodcat.length() > 0)
				game.banchoHandle.sendMessage("[" + bloodcat + " Bloodcat link for this map.]", false);
		}catch(Exception e){}
		
		game.feed.updateDiscord();
	}
	
	public Map getMap(){
		return map;
	}
	
	public List<Map> getBans(){
		List<Map> bans = new ArrayList<>(game.firstTeam.getBans());
		
		bans.addAll(game.secondTeam.getBans());
		
		return bans;
	}
	
	public List<PickedMap> getPicks(){
		List<PickedMap> picks = new ArrayList<>(game.firstTeam.getPicks());
		
		picks.addAll(game.secondTeam.getPicks());
		
		return picks;
	}
	
	public PickStrategy getPickStrategy(){
		return strategy;
	}
	
	public int getWarmupsLeft(){
		return warmupsLeft;
	}
	
	public int getBansLeft(){
		return bansLeft;
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
	
	public boolean wasPicked(Map map){
		for(PickedMap picked : getPicks())
			if(!picked.isWarmup() && picked.getMap().getURL().equalsIgnoreCase(map.getURL()))
				return true;
		
		return false;
	}
	
	public void skipWarmup(){
		if(warmupsLeft > 0 && game.state.eq(GameState.SELECTING)){
			clearPickTimer();
			warmupsLeft--;
			
			SelectMapCommand.pickingTeams.remove(game.nextTeam);
			ChangeWarmupModCommand.gamesAllowedToChangeMod.remove(game);
			SkipWarmupCommand.gamesAllowedToSkip.remove(game);
			
			game.switchNextTeam();
			game.feed.updateDiscord();
			game.banchoHandle.sendMessage("The warmup has been skipped!", false);
			selectWarmups();
		}
	}
	
	public void setMap(Map map){
		this.map = map;
	}
	
	public void removeBanLeft(){
		bansLeft--;
	}
	
	// in case the map was changed externally
	public void updateMap(String link){
		if(map != null && map.getURL().equalsIgnoreCase(link)) return;
		
		if(game.state.eq(GameState.PLAYING)){
			this.map = game.match.getMapPool().findMap(link);
			this.lobbyMap = map;
			game.readyManager.onMatchStart();
			
			return;
		}
		
		final Map fMap = map;
		this.map = game.match.getMapPool().findMap(link);
		this.lobbyMap = map;
		JSONObject jsMap = Map.getMapInfo(map.getBeatmapID(), game.match.getTournament().getInt("mode"), true);
		
		game.feed.updateTwitch(getMod(map).replace("None", "Nomod") + " pick: " + jsMap.getString("artist") + " - " + 
	    	  	 	 		   jsMap.getString("title") + " [" + jsMap.getString("version") + "] was picked by " + 
	    	  	 	 		   game.nextTeam.getTeam().getTeamName() + "!");
		
		if(fMap == null) game.getReadyManager().startReadyWait();
	}
	
	public void setWarmupMod(String mod){
		if(warmupsLeft > 0 && game.state.eq(GameState.SELECTING)){
			if(isWarmupTooLong(mod)){
				game.banchoHandle.sendMessage("This mod makes the warmup too long! The maximum length is " + 
											  Utils.toDuration(game.match.getTournament().getInt("warmupLength")) + ".", false);
				
				return;
			}
			
			warmupMod = mod;
			game.banchoHandle.sendMessage("!mp mods " + (mod.toUpperCase().equals("NM") ? "" : mod.toUpperCase() + " ") + "Freemod", false);
		}
	}
	
	public boolean isWarmupTooLong(String mod){
		return isWarmupTooLong(mod, map.getLength());
	}
	
	public boolean isWarmupTooLong(String mod, int length){
		double modMultiplier = 1;
		
		if(mod.length() > 0)
			if(mod.equalsIgnoreCase("DT") || mod.equalsIgnoreCase("NC"))
				modMultiplier = 1.5;
			else if(mod.equalsIgnoreCase("HT"))
				modMultiplier = 0.75;
		
		return length / modMultiplier > game.match.getTournament().getInt("warmupLength");
	}
	
	public void pickTimer(int waitTime){
		if(game.state.eq(GameState.PLAYING)) return;
		if(pickTimer != null) pickTimer.cancel();
		
		pickTimer = new Timer();
		pickTimer.schedule(new TimerTask(){
			public void run(){
				if(game != null && !game.state.eq(GameState.PLAYING)){
					game.messageUpdater.cancel();
					
					if(warmupsLeft == 0 && game.state.eq(GameState.BANNING)){
						bansLeft--;
						game.nextTeam.addBan(new Map("https://osu.ppy.sh/b/1", 1, null));
						BanMapCommand.banningTeams.remove(game.nextTeam);
						game.switchNextTeam();
						selectBans();
						
						return;
					}
					
					if(map != null && !game.state.eq(GameState.PRESTART) && !game.state.eq(GameState.PLAYING)){
						game.banchoHandle.sendMessage("Attempting to force start...", false);
						game.readyManager.playersReady();
						
						return;
					}
					
					if(warmupsLeft > 0){
						warmupsLeft--;
						lobbyUpdater.cancel();
						SelectMapCommand.pickingTeams.remove(game.nextTeam);
						ChangeWarmupModCommand.gamesAllowedToChangeMod.remove(game);
						SkipWarmupCommand.gamesAllowedToSkip.remove(game);
						game.banchoHandle.sendMessage(game.nextTeam.getTeam().getTeamName() + 
													  " took too long to pick a warmup, they will not get a warmup!", false);
						game.switchNextTeam();
						selectWarmups();
						
						return;
					}
					
					game.banchoHandle.sendMessage(game.nextTeam.getTeam().getTeamName() + " took too long to select a map!", false);
					SelectMapCommand.pickingTeams.remove(game.nextTeam);
					game.switchNextTeam();
					
					selectPicks();
				}
			}
		}, waitTime * 1000);
	}
	
	public void clearPickTimer(){
		if(pickTimer != null){
			pickTimer.cancel();
			pickTimer = null;
		}
	}
}
