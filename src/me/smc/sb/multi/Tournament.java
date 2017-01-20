package me.smc.sb.multi;

import java.io.File;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;

import com.jcabi.jdbc.JdbcSession;
import com.jcabi.jdbc.Outcome;

import me.smc.sb.main.Main;
import me.smc.sb.utils.Configuration;
import me.smc.sb.utils.Log;
import me.smc.sb.utils.Utils;

public class Tournament{
	
	private int tournamentId;
	private String name;
	private String displayName;
	private String twitchChannel;
	private Game currentlyStreamed; //game switching from twitch channel? idk
	private static java.util.Map<String, LinkedList<Game>> twitchQueue = new HashMap<>();
	public static List<Tournament> tournaments;
	public static int matchesRunning = 0;
	public java.util.Map<String, Match> conditionalTeams = new HashMap<>();
	private List<Team> teams;
	private List<Match> matches;
	private List<MapPool> pools;
	private ArrayList<String> matchAdmins;
	public static List<Long> matchDates = new ArrayList<>();
	private boolean scoreV2;
	private int pickWaitTime;
	private int banWaitTime;
	private int readyWaitTime;
	private final int tournamentType; //0 = team
	private int mode;
	private int lowerRankBound;
	private int upperRankBound;
	private int targetRankLowerBound;
	private int targetRankUpperBound;
	private boolean skipWarmups;
	private boolean usingTourneyServer;
	private boolean usingConfirms;
	private boolean usingMapStats;
	private String resultDiscord;
	private String alertDiscord;
	private String alertMessage;
	private PickStrategy pickStrategy;
	private int rematchesAllowed;
	private long tempLobbyDecayTime;
	public static String tournamentDB = "Tournament_DB";
	
	public Tournament(String name){
		this(name, true);
	}
	
	public Tournament(String name, boolean append){
		this.name = name;
		teams = new ArrayList<>();
		matches = new ArrayList<>();
		pools = new ArrayList<>();
		matchAdmins = new ArrayList<>();
		
		tempLobbyDecayTime = 0;
		
		displayName = getConfig().getValue("displayName");
		twitchChannel = getConfig().getValue("twitchChannel");
		scoreV2 = getConfig().getBoolean("scoreV2");
		pickWaitTime = getConfig().getInt("pickWaitTime");
		banWaitTime = getConfig().getInt("banWaitTime");
		readyWaitTime = getConfig().getInt("readyWaitTime");
		tournamentType = getConfig().getInt("tournamentType");
		mode = getConfig().getInt("mode");
		resultDiscord = getConfig().getValue("resultDiscord");
		alertDiscord = getConfig().getValue("alertDiscord");
		alertMessage = getConfig().getValue("alertMessage");
		lowerRankBound = getConfig().getInt("lowerRankBound");
		upperRankBound = getConfig().getInt("upperRankBound");
		skipWarmups = getConfig().getBoolean("skipWarmups");
		pickStrategy = PickStrategy.findStrategy(getConfig().getValue("pickStrategy"));
		rematchesAllowed = getConfig().getInt("rematchesAllowed");
		usingTourneyServer = getConfig().getBoolean("usingTourneyServer");
		usingConfirms = getConfig().getBoolean("usingConfirms");
		usingMapStats = getConfig().getBoolean("usingMapStats");
		targetRankLowerBound = getConfig().getInt("targetRankLowerBound");
		targetRankUpperBound = getConfig().getInt("targetRankUpperBound");
		
		currentlyStreamed = null;

		if(append) save(append);
		
		//saveSQL(true);
		tournaments.add(this);
	}
	
	public Tournament(String name, String displayName, String twitchChannel, boolean scoreV2, int pickWaitTime, int banWaitTime, 
					  int readyWaitTime, int type, int mode, String resultDiscord, String alertDiscord, String alertMessage,
					  int lowerRankBound, int upperRankBound, boolean skipWarmups, String pickStrategy, int rematchesAllowed,
					  boolean usingTourneyServer, boolean usingConfirms, boolean usingMapStats, int targetRankLowerBound,
					  int targetRankUpperBound){
		this.name = name;
		this.displayName = displayName;
		this.twitchChannel = twitchChannel;
		this.scoreV2 = scoreV2;
		this.pickWaitTime = pickWaitTime;
		this.banWaitTime = banWaitTime;
		this.readyWaitTime = readyWaitTime;
		this.tournamentType = type;
		this.mode = mode;
		this.resultDiscord = resultDiscord;
		this.alertDiscord = alertDiscord;
		this.alertMessage = alertMessage;
		this.lowerRankBound = lowerRankBound;
		this.upperRankBound = upperRankBound;
		this.skipWarmups = skipWarmups;
		this.pickStrategy = PickStrategy.findStrategy(pickStrategy);
		this.rematchesAllowed = rematchesAllowed;
		this.usingTourneyServer = usingTourneyServer;
		this.usingConfirms = usingConfirms;
		this.usingMapStats = usingMapStats;
		this.targetRankLowerBound = targetRankLowerBound;
		this.targetRankUpperBound = targetRankUpperBound;
		
		currentlyStreamed = null;
		
		teams = new ArrayList<>();
		matches = new ArrayList<>();
		pools = new ArrayList<>();
		matchAdmins = new ArrayList<>();
		
		tempLobbyDecayTime = 0;
		
		tournaments.add(this);
	}
	
	public String getName(){
		return name;
	}
	
	public String getDisplayName(){
		if(displayName.length() == 0) return name;
		return displayName;
	}
	
	public void setDisplayName(String displayName){
		this.displayName = displayName;
	}
	
	public boolean isScoreV2(){
		return scoreV2;
	}
	
	public void setScoreV2(boolean scoreV2){
		this.scoreV2 = scoreV2;
	}
	
	public int getTournamentId(){
		return tournamentId;
	}
	
	public void setTournamentId(int tournamentId){
		this.tournamentId = tournamentId;
	}
	
	public int getTournamentType(){
		return tournamentType;
	}
	
	public Configuration getConfig(){
		return new Configuration(new File("tournament-" + name + ".txt"));
	}
	
	public List<Team> getTeams(){
		return teams;
	}
	
	public List<Match> getMatches(){
		return matches;
	}
	
	public List<MapPool> getMapPools(){
		return pools;
	}
	
	public static List<Long> getMatchDates(){
		return matchDates;
	}
	
	public List<String> getMatchAdmins(){
		return matchAdmins;
	}
	
	public void setMatchAdmins(ArrayList<String> admins){
		this.matchAdmins = admins;
	}
	
	public int getPickWaitTime(){
		return pickWaitTime;
	}
	
	public int getBanWaitTime(){
		return banWaitTime;
	}
	
	public int getReadyWaitTime(){
		return readyWaitTime;
	}
	
	public int getMode(){
		return mode;
	}
	
	public String getResultDiscord(){
		return resultDiscord;
	}
	
	public String getAlertDiscord(){
		return alertDiscord;
	}
	
	public String getAlertMessage(){
		return alertMessage;
	}
	
	public int getLowerRankBound(){
		return lowerRankBound;
	}
	
	public int getUpperRankBound(){
		return upperRankBound;
	}
	
	public int getTargetRankLowerBound(){
		return targetRankLowerBound;
	}
	
	public int getTargetRankUpperBound(){
		return targetRankUpperBound;
	}
	
	public boolean isWithinTargetBounds(int rank){
		return rank >= targetRankLowerBound && rank <= targetRankUpperBound;
	}
	
	public PickStrategy getPickStrategy(){
		return pickStrategy;
	}
	
	public void setPickStrategy(String name){
		pickStrategy = PickStrategy.findStrategy(name);
	}
	
	public int getRematchesAllowed(){
		return rematchesAllowed;
	}
	
	public void setRematchesAllowed(int rematches){
		this.rematchesAllowed = rematches;
	}
	
	public boolean isUsingTourneyServer(){
		return usingTourneyServer;
	}
	
	public boolean isUsingConfirms(){
		return usingConfirms;
	}
	
	public boolean isUsingMapStats(){
		return usingMapStats;
	}
	
	public boolean isSkippingWarmups(){
		return skipWarmups;
	}
	
	public void setSkippingWarmups(boolean skipWarmups){
		this.skipWarmups = skipWarmups;
	}
	
	public String getTwitchChannel(){
		return twitchChannel;
	}
	
	public Game getCurrentlyStreamed(){
		return currentlyStreamed;
	}
	
	public long getTempLobbyDecayTime(){
		return tempLobbyDecayTime;
	}
	
	public java.util.Map<String, Match> getConditionalTeams(){
		return conditionalTeams;
	}
	
	public void removeConditionalTeam(String team){
		conditionalTeams.remove(team);
	}
	
	public void setTempLobbyDecayTime(){
		tempLobbyDecayTime = System.currentTimeMillis() + 300000; // 5 minutes
	}
	
	public void setCurrentlyStreamed(Game game){
		this.currentlyStreamed = game;
	}
	
	public boolean isStreaming(){
		return currentlyStreamed != null;
	}
	
	public boolean isStreamed(Game game){
		if(currentlyStreamed == null || game.match == null) return false;
		
		try{
			if(game.match.getMatchNum() == currentlyStreamed.match.getMatchNum())
				return true;
		}catch(Exception e){
			return false;
		}
		
		return false;
	}
	
	public boolean startStreaming(Game game){
		if(isStreamed(game)) return true;
		
		if(isChannelInUse(twitchChannel)){
			Game streamed = getStreamed(twitchChannel);
			
			if(streamed != null && streamed.match != null){
				try{
					if(game.match.getStreamPriority() >= streamed.match.getStreamPriority()){
						addToTwitchQueue(game);
						return false;
					}else
						streamed.match.getTournament().setCurrentlyStreamed(null);
				}catch(Exception e){
					Log.logger.log(Level.INFO, "game null? " + (game == null) + " game match null? " + (game.match == null) +
											   " streamed null? " + (streamed == null) + " streamed match null? " + (streamed.match == null));
					
					return false;
				}
			}
		}
		
		currentlyStreamed = game;
		
		String accessToken = new Configuration(new File("login.txt")).getValue("twitch-access");
		
		String title = name + ":+" + game.match.getFirstTeam().getTeamName() + "+vs+" + game.match.getSecondTeam().getTeamName();
		
		try{
		    ProcessBuilder pb = new ProcessBuilder(
		            "curl",
		            "-H 'Accept: application/vnd.twitchtv.v2+json'",
		            "-H 'Authorization: OAuth " + accessToken + "'",
		            "-d \"channel[status]=" + title.replaceAll(" ", "+") + "\"",
		            "-X PUT https://api.twitch.tv/kraken/channels/" + twitchChannel);
		    
		    Process p = pb.start();
			
			p.waitFor();
		}catch(Exception e){
			Log.logger.log(Level.SEVERE, e.getMessage(), e);
		}
		
		return true;
	}
	
	public void stopStreaming(Game game){
		if(!isStreaming() || !isStreamed(game)){
			removeFromTwitchQueue(game);
			return;
		}
		
		currentlyStreamed = null;
		
		Game next = getNextFromTwitchQueue(1);
		if(next != null)
			next.match.getTournament().setCurrentlyStreamed(next);
	}
	
	public static boolean isChannelInUse(String channel){
		for(Tournament t : tournaments)
			if(t.getTwitchChannel().equalsIgnoreCase(channel) &&
				t.isStreaming())
				return true;
		
		return false;
	}
	
	public static Game getStreamed(String channel){
		for(Tournament t : tournaments)
			if(t.getTwitchChannel().equalsIgnoreCase(channel) &&
				t.isStreaming())
				return t.getCurrentlyStreamed();
		
		return null;
	}
	
	private void addToTwitchQueue(Game game){
		LinkedList<Game> games = new LinkedList<>();
		
		if(twitchQueue.containsKey(twitchChannel)) 
			games = twitchQueue.get(twitchChannel);
		
		if(!games.contains(game)) games.add(game);
		
		twitchQueue.put(twitchChannel, games);
	}
	
	private Game getNextFromTwitchQueue(int priority){
		try{
			if(twitchQueue.containsKey(twitchChannel)){
				Optional<Game> optGame = twitchQueue.get(twitchChannel).stream()
										 .filter(g -> g.match.getStreamPriority() == priority)
										 .findFirst();
				
				if(optGame.isPresent()){
					removeFromTwitchQueue(optGame.get());
					
					return optGame.get();	
				}else if(twitchQueue.get(twitchChannel).stream()
						 .filter(g -> g.match.getStreamPriority() > priority)
						 .count() > 0)
					return getNextFromTwitchQueue(priority + 1);
			}
		}catch(Exception e){}

		return null;
	}
	
	private void removeFromTwitchQueue(Game game){
		if(twitchQueue.containsKey(twitchChannel)){
			LinkedList<Game> games = twitchQueue.get(twitchChannel);
			games.remove(game);
			
			if(games.size() > 0) twitchQueue.put(twitchChannel, games);
			else twitchQueue.remove(twitchChannel);
		}
	}
	
	public static String getCurrentMPLink(String channel){
		String c = channel;
		if(channel.startsWith("#")) c = channel.substring(1);
		
		for(Tournament t : tournaments)
			if(t.getTwitchChannel().equalsIgnoreCase(c) &&
				t.isStreaming())
				return t.currentlyStreamed.mpLink;
			
		return "Unknown";
	}
	
	public static String getCurrentScore(String channel){
		String c = channel;
		if(channel.startsWith("#")) c = channel.substring(1);
		
		for(Tournament t : tournaments)
			if(t.getTwitchChannel().equalsIgnoreCase(c) &&
				t.isStreaming()){
				Game game = t.getCurrentlyStreamed();
				Match match = game.match;
				
				return match.getFirstTeam().getTeamName() + " " + game.fTeamPoints + " | " +
				 	   game.sTeamPoints + " " + match.getSecondTeam().getTeamName() + " BO" + 
				 	   match.getBestOf();
			}
			
		return "Unknown";
	}
	
	public static String getCurrentMap(String channel){
		String c = channel;
		if(channel.startsWith("#")) c = channel.substring(1);
		
		for(Tournament t : tournaments)
			if(t.getTwitchChannel().equalsIgnoreCase(c) &&
				t.isStreaming()){
				Game game = t.getCurrentlyStreamed();
				Map map = game.map;
				
				if(map == null) return "No map chosen yet!";
				
				return "" + map.getBeatmapID();
			}
			
		return "Unknown";
	}
	
	public void setTwitchChannel(String twitchChannel){
		this.twitchChannel = twitchChannel;
	}
	
	public void setPickWaitTime(int pickWaitTime){
		this.pickWaitTime = pickWaitTime;
	}
	
	public void setBanWaitTime(int banWaitTime){
		this.banWaitTime = banWaitTime;
	}
	
	public void setReadyWaitTime(int readyWaitTime){
		this.readyWaitTime = readyWaitTime;
	}
	
	public void setMode(int mode){
		this.mode = mode;
	}
	
	public void setResultDiscord(String resultDiscord){
		this.resultDiscord = resultDiscord;
	}
	
	public void setAlertDiscord(String alertDiscord){
		this.alertDiscord = alertDiscord;
	}
	
	public void setAlertMessage(String alertMessage){
		this.alertMessage = alertMessage;
	}
	
	public void setLowerRankBound(int lowerRankBound){
		this.lowerRankBound = lowerRankBound;
	}
	
	public void setUpperRankBound(int upperRankBound){
		this.upperRankBound = upperRankBound;
	}
	
	public void setTargetRankLowerBound(int targetRankLowerBound){
		this.targetRankLowerBound = targetRankLowerBound;
	}
	
	public void setTargetRankUpperBound(int targetRankUpperBound){
		this.targetRankUpperBound = targetRankUpperBound;
	}
	
	public void addPool(MapPool pool){
		pools.add(pool);
	}
	
	public void addTeam(Team team){
		teams.add(team);
	}
	
	public void addMatch(Match match){
		matches.add(match);
	}
	
	public static void addMatchDate(long date){
		matchDates.add(date);
	}
	
	public Match getMatch(int matchNum){
		if(!matches.isEmpty())
			for(Match match : matches)
				if(match.getMatchNum() == matchNum)
					return match;
		return null;
	}
	
	public Team getTeam(String teamName){
		if(!teams.isEmpty())
			for(Team team : teams)
				if(team.getTeamName().equalsIgnoreCase(teamName))
					return team;
		return null;
	}
	
	public Team getTeam(int serverTeamId){
		if(!teams.isEmpty())
			for(Team team : teams)
				if(team.getServerTeamID() == serverTeamId)
					return team;
		return null;
	}
	
	public MapPool getPool(int poolNum){
		if(!pools.isEmpty())
			for(MapPool pool : pools)
				if(pool.getPoolNum() == poolNum)
					return pool;
		return null;
	}
	
	public boolean removeMatch(int matchNum){
		if(!matches.isEmpty())
			for(Match match : new ArrayList<Match>(matches))
				if(match.getMatchNum() == matchNum){
					matches.remove(match);
					
					if(match.getTime() != 0)
						matchDates.remove(match.getTime());
					
					match.delete();
					return true;
				}
		return false;
	}
	
	public boolean removeTeam(String teamName){
		if(!teams.isEmpty())
			for(Team team : new ArrayList<Team>(teams))
				if(team.getTeamName().equalsIgnoreCase(teamName)){
					teams.remove(team);
					team.delete();
					return true;
				}
		return false;
	}
	
	public boolean removePool(int poolNum){
		if(!pools.isEmpty())
			for(MapPool pool : new ArrayList<MapPool>(pools))
				if(pool.getPoolNum() == poolNum){
					pools.remove(pool);
					pool.delete();
					return true;
				}
		return false;
	}
	
	public void save(boolean append){
		if(append) new Configuration(new File("tournaments.txt")).appendToStringList("tournaments", name, true);
		
		Configuration config = getConfig();
		
		config.writeValue("displayName", displayName);
		config.writeValue("twitchChannel", twitchChannel);
		config.writeValue("scoreV2", scoreV2);
		config.writeValue("pickWaitTime", pickWaitTime);
		config.writeValue("banWaitTime", banWaitTime);
		config.writeValue("readyWaitTime", readyWaitTime);
		config.writeValue("type", tournamentType);
		config.writeValue("mode", mode);
		config.writeValue("resultDiscord", resultDiscord);
		config.writeValue("alertDiscord", alertDiscord);
		config.writeValue("alertMessage", alertMessage);
		config.writeValue("lowerRankBound", lowerRankBound);
		config.writeValue("upperRankBound", upperRankBound);
		config.writeValue("skipWarmups", skipWarmups);
		config.writeValue("pickStrategy", PickStrategy.getStrategyName(pickStrategy));
		config.writeValue("rematchesAllowed", rematchesAllowed);
		config.writeValue("usingTourneyServer", usingTourneyServer);
		config.writeValue("usingConfirms", usingConfirms);
		config.writeValue("usingMapStats", usingMapStats);
		config.writeValue("targetRankLowerBound", targetRankLowerBound);
		config.writeValue("targetRankUpperBound", targetRankUpperBound);
		config.writeStringList("tournament-admins", matchAdmins, true);
	}
	
	public void saveSQL(boolean add){
		try{
			if(add){
				new JdbcSession(Main.tourneySQL)
				.sql("INSERT INTO Tournament (name, display_name, twitch_channel, scoreV2, pick_wait_time, ban_wait_time, ready_wait_time, " +
					 "type, mode, result_discord, alert_discord, alert_message, lower_rank_bound, upper_rank_bound, skip_warmups, pick_strategy, rematches_allowed, "
					 + "using_tourney_server, using_confirms, using_map_stats, target_rank_lower_bound, target_rank_upper_bound) " +
				     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")
				.set(name)
				.set(displayName)
				.set(twitchChannel)
				.set(scoreV2 ? 1 : 0)
				.set(pickWaitTime)
				.set(banWaitTime)
				.set(readyWaitTime)
				.set(tournamentType)
				.set(mode)
				.set(resultDiscord)
				.set(alertDiscord)
				.set(alertMessage)
				.set(lowerRankBound)
				.set(upperRankBound)
				.set(skipWarmups)
				.set(PickStrategy.getStrategyName(pickStrategy))
				.set(rematchesAllowed)
				.set(usingTourneyServer)
				.set(usingConfirms)
				.set(usingMapStats)
				.set(targetRankLowerBound)
				.set(targetRankUpperBound)
				.insert(Outcome.VOID);
			}else{
				new JdbcSession(Main.tourneySQL)
				.sql("UPDATE Tournament " +
					 "SET display_name='?', twitch_channel='?', scoreV2='?', pick_wait_time='?', ban_wait_time='?', ready_wait_time='?', " +
					 "type='?', mode='?', result_discord='?', alert_discord='?', alert_message='?', lower_rank_bound='?', upper_rank_bound='?', " +
					 "skip_warmups='?', pick_strategy='?', rematches_allowed='?', using_tourney_server='?', using_confirms='?', using_map_stats='?', " +
					 "target_rank_lower_bound='?', target_rank_upper_bound='?' WHERE name='?'")
				.set(displayName)
				.set(twitchChannel)
				.set(scoreV2 ? 1 : 0)
				.set(pickWaitTime)
				.set(banWaitTime)
				.set(readyWaitTime)
				.set(tournamentType)
				.set(mode)
				.set(resultDiscord)
				.set(alertDiscord)
				.set(alertMessage)
				.set(lowerRankBound)
				.set(upperRankBound)
				.set(skipWarmups)
				.set(PickStrategy.getStrategyName(pickStrategy))
				.set(rematchesAllowed)
				.set(usingTourneyServer)
				.set(usingConfirms)
				.set(usingMapStats)
				.set(targetRankLowerBound)
				.set(targetRankUpperBound)
				.set(name)
				.update(Outcome.VOID);
			}
		}catch(Exception e){
			Log.logger.log(Level.SEVERE, e.getMessage(), e);
		}
	}
	
	public void delete(){
		Configuration config = new Configuration(new File("tournaments.txt"));
		ArrayList<String> savedTournaments = config.getStringList("tournaments");
		
		if(!savedTournaments.isEmpty()){
			savedTournaments.remove(name);
			config.writeStringList("tournaments", savedTournaments, true);
		}
		
		getConfig().delete();
		
		//deleteSQL();
	}
	
	public void deleteSQL(){
		try{
			new JdbcSession(Main.tourneySQL)
			.sql("DELETE FROM " + tournamentDB +
				 " WHERE name='?'")
			.set(name)
			.update(Outcome.VOID);
		}catch(Exception e){
			Log.logger.log(Level.SEVERE, e.getMessage(), e);
		}
	}
	
	public static Tournament getTournament(String name){
		return getTournament(name, 0);
	}
	
	public static Tournament getTournament(String name, int next){
		int nextSelected = next;
		
		if(!tournaments.isEmpty())
			for(Tournament t : tournaments)
				if(t.getName().equalsIgnoreCase(name)){
					if(nextSelected > 0){
						nextSelected--;
						continue;
					}
					
					return t;
				}
				else if(t.getDisplayName().equalsIgnoreCase(name)){
					if(nextSelected > 0){
						nextSelected--;
						continue;
					}
					
					return t;
				}
		
		return null;
	}
	
	public static void loadTournaments(){
		tournaments = new ArrayList<>();
		List<String> savedTournaments = new Configuration(new File("tournaments.txt")).getStringList("tournaments");

		if(!savedTournaments.isEmpty())
			for(String sTournament : savedTournaments){
				Tournament tournament = new Tournament(sTournament, false);

				tournament.loadPools();
				tournament.loadTeams();
				tournament.loadMatches();
				
				if(!tournament.getConfig().getStringList("tournament-admins").isEmpty()){
					tournament.setMatchAdmins(tournament.getConfig().getStringList("tournament-admins"));
				}
			}	
	}
	
	public static void loadTournamentsSQL(){ //run bot once to save and then change to load as well
		tournaments = new ArrayList<>();
		try{
			new JdbcSession(Main.tourneySQL)
				     .sql("SELECT id_tournament, name, display_name, scoreV2, pick_wait_time, ban_wait_time, ready_wait_time, " +
				     	  "type, mode, result_discord, lower_rank_bound, upper_rank_bound, skip_warmups, pick_strategy, rematches_allowed " +
				    	  "using_tourney_server, using_confirms, using_map_stats FROM Tournament")
				     .select(new Outcome<List<String>>(){
				    	 @Override public List<String> handle(ResultSet rset, Statement stmt) throws SQLException{
				    		 while(rset.next()){
				    			 Tournament t = new Tournament(rset.getString(2), rset.getString(3), rset.getString(4), rset.getBoolean(5), 
				    					 					   rset.getInt(6), rset.getInt(7), rset.getInt(8), rset.getInt(9), rset.getInt(10), 
				    					 					   rset.getString(11), rset.getString(12), rset.getString(13), rset.getInt(14), 
				    					 					   rset.getInt(15), rset.getBoolean(16), rset.getString(17), rset.getInt(18),
				    					 					   rset.getBoolean(19), rset.getBoolean(20), rset.getBoolean(21), rset.getInt(22),
				    					 					   rset.getInt(23));
				    			 
				    			 t.setTournamentId(rset.getInt(1));
				    			 
				    			 t.loadPools(); //finish those 3
				    			 t.loadTeams();
				    			 t.loadMatches();
				    		 }
				    		 
				    		 return new ArrayList<String>();
				    	 }
				     });
		}catch(Exception e){
			Log.logger.log(Level.SEVERE, e.getMessage(), e);
		}
	}
	
	private void loadMatches(){
		Configuration config = getConfig();
		List<String> savedMatches = config.getStringList("matches");
		
		if(!savedMatches.isEmpty())
			for(String matchNum : savedMatches){
				Match match = new Match(this, Utils.stringToInt(matchNum), config.getInt("match-" + matchNum + "-players"), false);
				
				if(config.getValue("match-" + matchNum + "-team1") != "" || config.getValue("match-" + matchNum + "-team2") != ""){
					Team team1 = getTeam(config.getValue("match-" + matchNum + "-team1"));
					Team team2 = getTeam(config.getValue("match-" + matchNum + "-team2"));
					
					if(team1 == null || team2 == null){
						if(team1 == null && isMatchConditional(config.getValue("match-" + matchNum + "-team1"))){
							conditionalTeams.put(config.getValue("match-" + matchNum + "-team1") + " 1", match);
							Log.logger.log(Level.INFO, "Match #" + matchNum + " loaded conditional " + config.getValue("match-" + matchNum + "-team1") + " 1");
						}
						
						if(team2 == null && isMatchConditional(config.getValue("match-" + matchNum + "-team2"))){
							conditionalTeams.put(config.getValue("match-" + matchNum + "-team2") + " 2", match);
							Log.logger.log(Level.INFO, "Match #" + matchNum + " loaded conditional " + config.getValue("match-" + matchNum + "-team2") + " 2");
						}
					}
					
					match.setTeams(team1, team2);
				}
				
				if(config.getInt("match-" + matchNum + "-pool") != 0)
					match.setMapPool(getPool(config.getInt("match-" + matchNum + "-pool")));
				
				if(config.getInt("match-" + matchNum + "-bestof") != 0)
					match.setBestOf(config.getInt("match-" + matchNum + "-bestof"));
				
				if(config.getLong("match-" + matchNum + "-date") != 0)
					match.setTime(config.getLong("match-" + matchNum + "-date"));
				
				if(!config.getStringList("match-" + matchNum + "-admins").isEmpty())
					match.setMatchAdmins(config.getStringList("match-" + matchNum + "-admins"));
				
				if(config.getValue("match-" + matchNum + "-serverid") != "")
					match.setServerID(config.getValue("match-" + matchNum + "-serverid"));
				
				if(config.getInt("match-" + matchNum + "-priority") == 0)
					match.setStreamPriority(1);
				else match.setStreamPriority(config.getInt("match-" + matchNum + "-priority"));
			}
	}
	
	protected boolean isMatchConditional(String conditionalStr){
		if(conditionalStr.split(" ")[0].equalsIgnoreCase("loser") || conditionalStr.split(" ")[0].equalsIgnoreCase("winner")){
			if(Utils.stringToInt(conditionalStr.split(" ")[1]) != -1 && conditionalStr.split(" ").length == 2)
				return true;
		}
		
		return false;
	}
	
	protected int incrementMatchCount(){
		Configuration config = getConfig();
		int lastMatch = config.getInt("last-match");
		config.writeValue("last-match", lastMatch + 1);
		return lastMatch + 1;
	}
	
	private void loadTeams(){
		Configuration config = getConfig();
		List<String> savedTeams = config.getStringList("teams");
		
		if(!savedTeams.isEmpty())
			for(String teamName : savedTeams){
				Team team = new Team(this, teamName, false);
				
				List<String> players = config.getStringList("team-" + teamName);
				if(players.size() != 0){
					LinkedList<Player> lPlayers = new LinkedList<>();
					for(String name : players)
						lPlayers.add(new Player(name));
					
					team.setPlayers(lPlayers);
				}
				
				if(config.getInt("team-" + teamName + "-serverID") != 0)
					team.setServerTeamID(config.getInt("team-" + teamName + "-serverID"));
			}
	}
	
	private void loadPools(){
		Configuration config = getConfig();
		List<String> savedPools = config.getStringList("pools");
		
		if(!savedPools.isEmpty())
			for(String poolNum : savedPools){
				MapPool pool = new MapPool(this, Utils.stringToInt(poolNum), false);
				
				List<String> maps = config.getStringList("pool-" + poolNum + "-maps");
				if(maps.size() != 0)
					for(String map : maps)
						pool.addMap(new Map(map));
				
				if(config.getValue("pool-" + poolNum + "-sheet") != "") 
					pool.setSheetUrl(config.getValue("pool-" + poolNum + "-sheet"));
			}
	}
	
	protected int incrementPoolCount(){
		Configuration config = getConfig();
		int lastPool = config.getInt("last-pool");
		config.writeValue("last-pool", lastPool + 1);
		return lastPool + 1;
	}
	
}
