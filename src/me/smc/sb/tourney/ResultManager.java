package me.smc.sb.tourney;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;

import org.json.JSONArray;
import org.json.JSONObject;

import me.smc.sb.irccommands.ContestCommand;
import me.smc.sb.irccommands.SkipRematchCommand;
import me.smc.sb.main.Main;
import me.smc.sb.scoringstrategies.DefaultScoringStrategy;
import me.smc.sb.scoringstrategies.ScoringStrategy;
import me.smc.sb.tracking.Mods;
import me.smc.sb.tracking.OsuMultiRequest;
import me.smc.sb.tracking.OsuRequest;
import me.smc.sb.tracking.TrackedPlay;
import me.smc.sb.utils.Log;
import me.smc.sb.utils.RemotePatyServerUtils;
import me.smc.sb.utils.Utils;

public class ResultManager{

	private Game game;
	protected List<String> results;
	protected ScoringStrategy strategy;
	protected int skipRematchState; // 0 = no, 1 = first agree, 2 = second agree, 3 = ok
	protected int contestState;     // ^
	protected boolean lastRematch;  // true = first team initiated rematch
	protected boolean lastWinner;   // ^ but for last winner
	protected boolean rematching;
	
	public ResultManager(Game game){
		this.game = game;
		this.results = new ArrayList<>();
		this.strategy = ScoringStrategy.findStrategy(game.match.getTournament().get("scoringStrategy"));
		this.skipRematchState = 0;
		this.contestState = 0;
		this.rematching = false;
	}
	
	public void analyseResults(){
		game.state = GameState.CONFIRMING;
		
		// need to wait for the results to come in
		// make sure you set the map to null
		new Timer().schedule(new TimerTask(){
			public void run(){
				if(rematching) return;
				
				OsuRequest multiRequest = null;
				Object multiMatchObj = null;
				JSONArray multiMatch = null;
				List<Player> fTeamPlayers = new ArrayList<>();
				List<Player> sTeamPlayers = new ArrayList<>();
				float fTeamScore = 0, sTeamScore = 0;
				long totalScore = 0, targetRangeScore = 0;
				long totalPasses = 0, targetRangePasses = 0;
				long totalSubmits = 0;
				PlayingTeam rematchTeam = null;
				boolean rematch = false;
				java.util.Map<Integer, JSONObject> plays = new HashMap<>();
				
				long startTime = System.currentTimeMillis();
				
				while(plays.size() == 0){
					multiRequest = new OsuMultiRequest(game.mpLink.split("mp\\/")[1]);
					multiMatchObj = Main.hybridRegulator.sendRequest(multiRequest, 15000, true);
					
					if(multiMatchObj != null && multiMatchObj instanceof JSONArray){
						multiMatch = (JSONArray) multiMatchObj;
						plays = getPlays(multiMatch, game.selectionManager.getMap().getBeatmapID());
					}
					
					if(System.currentTimeMillis() - startTime > 30000) break;
				}
				
				for(String result : results){
					String playerName = result.substring(0, result.indexOf(" finished"));
					
					if(!fTeamPlayers.contains(playerName) && !sTeamPlayers.contains(playerName)){
						Player player = game.lobbyManager.findPlayer(playerName);
						PlayingTeam team = game.lobbyManager.findTeam(player);
						boolean fTeam = game.lobbyManager.isOnFirstTeam(player);
						
						if(!player.submittedScore()){
							if(fTeam) fTeamPlayers.add(player);
							else sTeamPlayers.add(player);
							
							player.setSubmitted(true);
							totalSubmits++;
							
							long score = Utils.stringToInt(result.split("Score: ")[1].split(",")[0]);
							JSONObject play = null;
							
							if(plays.containsKey(player.getSlot() - 1)){
								play = plays.get(player.getSlot() - 1);
								play.put("enabled_mods", Mods.getMods(player.getMods()));
							}
							
							if(play != null) score = strategy.calculateScore(player.getName(), new TrackedPlay(play, game.match.getTournament().getInt("mode")), 
																							 game.match.getTournament().getBool("scoreV2"), game.banchoHandle);
							
							if(result.split(",")[1].substring(1).split("\\)")[0].equalsIgnoreCase("PASSED")){
								if(score > 1200000 * player.getModMultiplier() && game.match.getTournament().getBool("scoreV2") && strategy instanceof DefaultScoringStrategy){
									rematch = game.selectionManager.warmupsLeft > 0 && team.canRematch();
									rematchTeam = team;
									game.banchoHandle.sendMessage(player.getName() + " is on fallback, please use stable!" + 
																 (rematch ? " There will be a rematch!" : ""), false);
								}
								
								if(fTeam) fTeamScore += score;
								else sTeamScore += score;
								
								totalScore += score;
								totalPasses++;
								
								if(player.getRank() > 0 && isWithinTargetBounds(player.getRank())){
									targetRangeScore += score;
									targetRangePasses++;
								}
							}
						}
					}
				}
				
				results.clear();
				
				if(rematch && rematchTeam != null && game.selectionManager.warmupsLeft == 0){
					game.feed.updateTwitch("A player was using fallback, there will be a rematch!");
					rematch(rematchTeam);
					
					return;
				}
				
				if(game.selectionManager.warmupsLeft > 0){
					String updateMessage = "";
					
					if(fTeamScore == sTeamScore) updateMessage = "Both " + (game.match.getTournament().getInt("type") == 0 ? "teams" : "players") + " have tied!";
					else
						updateMessage = (fTeamScore > sTeamScore ? game.firstTeam.getTeam().getTeamName() :
										 game.secondTeam.getTeam().getTeamName()) + " won by " + 
										 Utils.veryLongNumberDisplay((long) Math.abs(fTeamScore - sTeamScore)) + " points!";
					
					game.selectionManager.warmupsLeft--;
					game.banchoHandle.sendMessage(updateMessage, false);
					game.feed.updateTwitch(updateMessage, 20);
					game.feed.updateDiscord();
					game.switchNextTeam();
					game.selectionManager.selectWarmups();
				}else{
					rematch: if((int) Math.abs(fTeamPlayers.size() - sTeamPlayers.size()) != 0){	
						boolean fTeamDC = sTeamPlayers.size() > fTeamPlayers.size();
						
						if((fTeamDC && fTeamScore < sTeamScore) || (!fTeamDC && sTeamScore < fTeamScore)){
							if(game.match.getTournament().getBool("scoreV2") && strategy instanceof DefaultScoringStrategy){
								float fScore = fTeamScore + calculateMissingScores(true);
								float sScore = sTeamScore + calculateMissingScores(false);
								
								if((fTeamDC && sScore > fScore) || (!fTeamDC && fScore > sScore))
									break rematch;
							}
							
							if((fTeamDC ? game.firstTeam.canRematch() : game.secondTeam.canRematch())){
								game.banchoHandle.sendMessage("Someone has disconnected, there will be a rematch!", false);
								game.feed.updateTwitch("There was a disconnection, the match will be replayed!");
								lastWinner = fTeamScore > sTeamScore;
								rematch(fTeamDC ? game.firstTeam : game.secondTeam);
								
								return;
							}
						}
					}
					
					if(fTeamScore == sTeamScore){
						String message = "Both " + (game.match.getTournament().getInt("type") == 0 ? "teams" : "players") + " have tied, there will be a rematch!";
						game.banchoHandle.sendMessage(message, false);
						game.feed.updateTwitch(message);
						game.readyManager.switchPlaying(false, true);
						game.readyManager.startReadyWait();
						
						return;
					}
					
					if(game.match.getTournament().getBool("usingMapStats")){
						final long tScore = totalScore, trScore = targetRangeScore;
						final long tPasses = totalPasses, trPasses = targetRangePasses;
						final long tSubmits = totalSubmits;
						
						new Thread(new Runnable(){
							public void run(){
								incrementPlayerStats(tScore, trScore, tPasses, trPasses, tSubmits);
							}
						}).start();
					}
					
					boolean fTeamWon = fTeamScore > sTeamScore;
					
					String updateMessage = (fTeamScore > sTeamScore ? game.firstTeam.getTeam().getTeamName() :
						 					game.secondTeam.getTeam().getTeamName()) + " won by " + 
						 					Utils.veryLongNumberDisplay((long) Math.abs(fTeamScore - sTeamScore)) + " points!";
					
					game.banchoHandle.sendMessage(updateMessage, false);
					game.feed.updateTwitch(updateMessage, 20);
					game.switchNextTeam();
					
					if(fTeamWon) game.firstTeam.addPoint();
					else game.secondTeam.addPoint();
					
					lastWinner = fTeamScore > sTeamScore;
					updateScores(false);
				}
				
				game.readyManager.switchPlaying(false, true);
			}
		}, 10000);
	}
	
	public void updateScores(boolean onlyMessages){
		PlayingTeam first = game.firstTeam;
		PlayingTeam second = game.secondTeam;
		
		game.banchoHandle.sendMessage(first.getTeam().getTeamName() + " " + first.getPoints() + " | " +
									  second.getPoints() + " " + second.getTeam().getTeamName() +
									  "      Best of " + game.match.getBestOf(), false);
		game.feed.updateDiscord();
		game.feed.updateTwitch(first.getTeam().getTeamName() + " " + first.getPoints() + " | " +
					 		   second.getPoints() + " " + second.getTeam().getTeamName() + " BO" + game.match.getBestOf(), 20);
	
		if(first.getPoints() + second.getPoints() == game.match.getBestOf() - 1 && first.getPoints() == second.getPoints()){
			game.selectionManager.changeMap(game.match.getMapPool().findTiebreaker());
			contestMessage();
			game.readyManager.startReadyWait();
		}else if(first.getPoints() > Math.floor(game.match.getBestOf() / 2) || second.getPoints() > Math.floor(game.match.getBestOf() / 2)){
			String winningTeam = (first.getPoints() > second.getPoints() ? first.getTeam().getTeamName() : second.getTeam().getTeamName());
			
			game.banchoHandle.sendMessage(winningTeam + " has won this game!", false);
			game.banchoHandle.sendMessage("The lobby is ending in 30 seconds, thanks for playing!", false);
			game.banchoHandle.sendMessage("!mp timer", false);
			game.feed.updateTwitch(winningTeam + " has won this game! " + game.mpLink, 20);
			
			Timer twitchCloseDelay = new Timer();
			twitchCloseDelay.schedule(new TimerTask(){
				public void run(){
					if(first.getPoints() > Math.floor(game.match.getBestOf() / 2) || second.getPoints() > Math.floor(game.match.getBestOf() / 2))
						game.match.getTournament().getTwitchHandler().stopStreaming(game);
				}
			}, 25500);
			
			Timer time = new Timer();
			time.schedule(new TimerTask(){
				public void run(){
					if(first.getPoints() > Math.floor(game.match.getBestOf() / 2) || second.getPoints() > Math.floor(game.match.getBestOf() / 2))
						game.stop();
				}
			}, 30000);
			
			contestMessage();
			
			return;
		}else if(!onlyMessages){
			game.selectionManager.map = null;
			contestMessage();
			game.selectionManager.selectPicks();
		}
	}
	
	public void rematch(PlayingTeam team){
		if(rematching) return;
		
		boolean fTeam = game.firstTeam.getTeam().getTeamName().equalsIgnoreCase(team.getTeam().getTeamName());
		lastRematch = fTeam;
		
		team.useRematch();
		game.banchoHandle.sendMessage("If you do not wish to rematch, both " + (game.match.getTournament().getInt("type") == 0 ? "teams" : "players") + 
									  " need to use !skiprematch", false);
		
		SkipRematchCommand.gamesAllowedToSkip.add(game);
		skipRematchState = 0;
		rematching = true;
		game.readyManager.switchPlaying(false, true);
		game.readyManager.startReadyWait();
	}
	
	public void skipRematch(String userName){
		Player player = game.lobbyManager.findPlayer(userName);
		PlayingTeam team = game.lobbyManager.findTeam(player);
		
		if(team != null){
			boolean fTeam = game.lobbyManager.isOnFirstTeam(player);
			
			switch(skipRematchState){
				case 0: skipRematchState = fTeam ? 1 : 2;
						game.banchoHandle.sendMessage(team.getTeam().getTeamName() + " has voted to skip the rematch!", false);
						break;
				case 1: if(!fTeam) skipRematchState = 3; break;
				case 2: if(fTeam) skipRematchState = 3; break;
				default: return;
			}
			
			if(skipRematchState == 3){
				game.messageUpdater.cancel();
				SkipRematchCommand.gamesAllowedToSkip.remove(game);
				
				game.banchoHandle.sendMessage("The rematch has been skipped.", false);
				skipRematchState = 0;
				
				if(lastRematch) game.firstTeam.addRematch();
				else game.secondTeam.addRematch();
				
				game.selectionManager.clearPickTimer();
				
				if(lastWinner) game.firstTeam.addPoint();
				else game.secondTeam.addPoint();
				
				game.switchNextTeam();
				updateScores(false);
			}
		}
	}
	
	public void contest(String userName){
		Player player = game.lobbyManager.findPlayer(userName);
		PlayingTeam team = game.lobbyManager.findTeam(player);
		
		if(team != null){
			boolean fTeam = game.lobbyManager.isOnFirstTeam(player);
			
			switch(contestState){
				case 0: contestState = fTeam ? 1 : 2;
						game.banchoHandle.sendMessage(team.getTeam().getTeamName() + " has voted to contest the score!", false);
						break;
				case 1: if(!fTeam) contestState = 3; break;
				case 2: if(fTeam) contestState = 3; break;
				default: return;
			}
			
			if(contestState == 3){
				game.banchoHandle.sendMessage("The contest has been accepted.", false);
				contestState = 0;
				game.selectionManager.clearPickTimer();
				
				if(lastWinner){
					game.firstTeam.removePoint();
					game.secondTeam.addPoint();
					lastWinner = false;
				}else{
					game.secondTeam.removePoint();
					game.firstTeam.addPoint();
					lastWinner = true;
				}
				
				updateScores(false);
			}
		}
	}
	
	protected void contestMessage(){
		ContestCommand.gamesAllowedToContest.add(game);
		contestState = 0;
		game.banchoHandle.sendMessage("If you wish to give the other team the point instead, both teams please use !contest", false);
	}
	
	public void addResult(String result){
		for(String r : results)
			if(r.equalsIgnoreCase(result))
				return;
		
		results.add(result);
	}
	
	private void incrementPlayerStats(long totalScore, long targetRangeScore, long totalPasses, long targetRangePasses, long totalSubmits){
		int mapId = game.match.getMapPool().getMapId(game.selectionManager.map);
		
		if(mapId != 0){
			int tourneyId = 0;
			
			try{
				tourneyId = RemotePatyServerUtils.fetchTournamentId(game.match.getTournament().get("name"));
			}catch(Exception e){
				Log.logger.log(Level.SEVERE, "Could not fetch tourney id", e);
			}
			
			if(tourneyId != 0){
				int poolNum = game.match.getMapPool().getPoolNum();
				
				RemotePatyServerUtils.incrementMapValue(mapId, poolNum, tourneyId, "pickcount", 1);
				
				if(totalScore > 0){
					RemotePatyServerUtils.incrementMapValue(mapId, poolNum, tourneyId, "total_score", totalScore);
					RemotePatyServerUtils.incrementMapValue(mapId, poolNum, tourneyId, "target_range_score", targetRangeScore);
				}
				
				RemotePatyServerUtils.incrementMapValue(mapId, poolNum, tourneyId, "target_range_passed", targetRangePasses);
				RemotePatyServerUtils.incrementMapValue(mapId, poolNum, tourneyId, "plays_submitted", totalSubmits);
				RemotePatyServerUtils.incrementMapValue(mapId, poolNum, tourneyId, "plays_passed", totalPasses);
			}
		}
	}
	
	private int calculateMissingScores(boolean fTeam){ // v2 only
		int score = 0;
		Team team = fTeam ? game.firstTeam.getTeam() : game.secondTeam.getTeam();
		LinkedList<Player> tempList = new LinkedList<>(team.getPlayers());
		
		for(Player pl : tempList)
			if(pl.isPlaying() && !pl.submittedScore())
				score += 1000000 * pl.getModMultiplier() + 100000; // max score + spinning bonus
		
		return score;
	}
	
	public boolean isWithinTargetBounds(int rank){
		return rank >= game.match.getTournament().getInt("targetRankLowerBound") && 
			   rank <= game.match.getTournament().getInt("targetRankUpperBound");
	}
	
	private java.util.Map<Integer, JSONObject> getPlays(JSONArray multiMatch, int beatmapId){
		java.util.Map<Integer, JSONObject> plays = new HashMap<>();
		
		JSONArray mapsPlayed = multiMatch.getJSONArray(1);
		JSONObject played = mapsPlayed.getJSONObject(mapsPlayed.length() - 1);
		
		if(played.getInt("beatmap_id") == beatmapId){
			JSONArray scores = played.getJSONArray("scores");
			
			for(int i = 0; i >= scores.length(); i++){	
				JSONObject jsonPlay = scores.getJSONObject(i);
				
				jsonPlay.put("beatmap_id", beatmapId);
				plays.put(i, jsonPlay);
			}
		}
		
		return plays;
	}
}
