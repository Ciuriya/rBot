package me.smc.sb.tourney;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;

import me.smc.sb.irccommands.ContestCommand;
import me.smc.sb.irccommands.SkipRematchCommand;
import me.smc.sb.tourney.Team;
import me.smc.sb.tourney.Game;
import me.smc.sb.tourney.Player;
import me.smc.sb.tourney.GameState;
import me.smc.sb.utils.Log;
import me.smc.sb.utils.RemotePatyServerUtils;
import me.smc.sb.utils.Utils;

public class ResultManager{

	private Game game;
	private List<String> results;
	protected int skipRematchState; // 0 = no, 1 = first agree, 2 = second agree, 3 = ok
	protected int contestState;     // ^
	protected boolean lastRematch;  // true = first team initiated rematch
	protected boolean lastWinner;   // ^ but for last winner
	
	public ResultManager(Game game){
		this.game = game;
		this.results = new ArrayList<>();
		this.skipRematchState = 0;
		this.contestState = 0;
	}
	
	public void analyseResults(){
		game.state = GameState.CONFIRMING;
		
		// need to wait for the results to come in
		// make sure you set the map to null
		new Timer().schedule(new TimerTask(){
			public void run(){
				List<Player> fTeamPlayers = new ArrayList<>();
				List<Player> sTeamPlayers = new ArrayList<>();
				float fTeamScore = 0, sTeamScore = 0;
				long totalScore = 0, targetRangeScore = 0;
				long totalPasses = 0, targetRangePasses = 0;
				long totalSubmits = 0;
				PlayingTeam rematchTeam = null;
				boolean rematch = false;

				for(String result : results){
					String playerName = result.substring(0, result.indexOf(" finished"));
					
					if(fTeamPlayers.contains(playerName) || sTeamPlayers.contains(playerName)) continue;
					
					Player player = game.lobbyManager.findPlayer(playerName);
					PlayingTeam team = game.lobbyManager.findTeam(player);
					boolean fTeam = game.lobbyManager.isOnFirstTeam(player);
					
					if(fTeam) fTeamPlayers.add(player);
					else sTeamPlayers.add(player);
					
					player.setSubmitted(true);
					totalSubmits++;
					
					if(result.split(",")[1].substring(1).split("\\)")[0].equalsIgnoreCase("PASSED")){
						int score = Utils.stringToInt(result.split("Score: ")[1].split(",")[0]);
						
						if(score > 1200000 * player.getModMultiplier() && game.match.getTournament().getBool("scoreV2")){
							rematch = game.selectionManager.warmupsLeft > 0 && team.canRematch();
							rematchTeam = team;
							game.banchoHandle.sendMessage(player + " is on fallback, please use stable!" + 
														 (rematch ? " There will be a rematch! (" + 
														 (team.getRematchesLeft() - 1) + " remaining)" : ""), false);
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
				
				if(rematch && rematchTeam != null){
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
					game.selectionManager.selectWarmups();
				}else{
					rematch: if((int) Math.abs(fTeamPlayers.size() - sTeamPlayers.size()) != 0){	
						boolean fTeamDC = sTeamPlayers.size() > fTeamPlayers.size();
						
						if((fTeamDC && fTeamScore < sTeamScore) || (!fTeamDC && sTeamScore < fTeamScore)){
							if(game.match.getTournament().getBool("scoreV2")){
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
	
	protected void updateScores(){
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
		}else{
			contestMessage();
			mapSelection(4);
		}
	}
	
	public void rematch(PlayingTeam team){
		boolean fTeam = game.firstTeam.getTeam().getTeamName().equalsIgnoreCase(team.getTeam().getTeamName());
		lastRematch = fTeam;
		
		team.useRematch();
		game.banchoHandle.sendMessage("If you do not wish to rematch, both " + (game.match.getTournament().getInt("type") == 0 ? "teams" : "players") + 
									  " need to use !skiprematch", false);
		
		SkipRematchCommand.gamesAllowedToSkip.add(game);
		skipRematchState = 0;
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
				
				updateScores();
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
			
			game.banchoHandle.sendMessage("The contest has been accepted.", false);
			contestState = 0;
			
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
	
	protected void contestMessage(){
		ContestCommand.gamesAllowedToContest.add(game);
		contestState = 0;
		
		game.banchoHandle.sendMessage("If you wish to give the other team the point instead, both teams please use !contest", false);
	}
	
	public void addResult(String result){
		results.add(result);
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
}
