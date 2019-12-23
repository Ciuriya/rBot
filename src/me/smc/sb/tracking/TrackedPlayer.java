package me.smc.sb.tracking;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import me.smc.sb.main.Main;
import me.smc.sb.utils.Utils;

public class TrackedPlayer{

	public static List<TrackedPlayer> registeredPlayers = new ArrayList<>();
	public static List<TrackedPlayer> secondCyclePlayers = new ArrayList<>();
	public static List<TrackedPlayer> inactivePlayers = new ArrayList<>();
	public static boolean changeOccured = false;
	public static boolean inactivityRefreshDone = false;
	public static boolean secondCycleLoop = false;
	public static boolean refreshing = false;
	public static int currentLoopCount = -1;
	public static final int API_FETCH_PLAY_LIMIT = 25;
	public static final long INACTIVITY_CUTOFF = 1577880000; // seconds (was 259200)
	public static final int LOOP_SKIPS_FOR_SECOND_CYCLE = 10;
	public static final long SECOND_CYCLE_CUTOFF = 1577870000; // seconds (was 3600)
	
	private String username;
	private int userId;
	private int mode;
	private CustomDate lastRankUpdate;
	private double oldPP;
	private double pp;
	private int rank;
	private int oldRank;
	private int countryRank;
	private String country;
	private boolean updatingPlayer;
	private boolean active;
	private boolean banned;
	private String lastUpdateMessage;
	private CustomDate lastUpdate;
	private CustomDate lastActive;
	private List<TrackingGuild> currentlyTracking;
	private List<TrackingGuild> leaderboardTracking;
	private LinkedList<TrackedPlay> lastPostedPlays;
	private boolean normalTrack;
	private boolean leaderboardTrack;
	
	public TrackedPlayer(int userId, int mode){
		this(Utils.getOsuPlayerName(userId, true), userId, mode);
	}
	
	public TrackedPlayer(String username, int userId, int mode){
		this.username = username;
		this.userId = userId;
		this.mode = mode;
		this.normalTrack = false;
		this.leaderboardTrack = false;
		this.active = true;
		banned = false;
		pp = 0;
		rank = 0;
		countryRank = 0;
		lastUpdate = new CustomDate();
		lastRankUpdate = new CustomDate();
		updatingPlayer = false;
		currentlyTracking = new ArrayList<>();
		leaderboardTracking = new ArrayList<>();
		lastPostedPlays = new LinkedList<>();
		country = "A2";
		lastActive = new CustomDate(Utils.toDate(0, "yyyy-MM-dd HH:mm:ss"));
		
		registeredPlayers.add(this);
		changeOccured = true;
	}
	
	public static boolean updateRegisteredPlayers(boolean subsequentRestart){
		if(refreshing) return false;
		
		boolean change = false;
		
		if(inactivityRefreshDone){
			List<TrackedPlayer> scanned = new ArrayList<>();
			
			for(TrackedPlayer player : new ArrayList<>(inactivePlayers)){
				if(!scanned.contains(player) && !player.isActive() && Utils.getCurrentTimeUTC() - player.getLastActive().getTime() < INACTIVITY_CUTOFF * 1000){
					for(TrackedPlayer found : find(player.getUserId())){
						found.setActive(true);
						inactivePlayers.remove(found);
						scanned.add(found);
						
						change = true;
					}
				}
			}

			inactivityRefreshDone = false;
		}
		
		for(TrackedPlayer player : new ArrayList<>(registeredPlayers)){
			if(player.getTrackers().size() == 0 && player.getLeaderboardTrackers().size() == 0){
				registeredPlayers.remove(player);
				change = true;
			}
			
			// never active
			if(player.getLastActive().getTime() <= 0 && !player.isBanned()){
				List<TrackedPlayer> foundModes = TrackedPlayer.find(player.getUserId());
				
				for(TrackedPlayer found : foundModes){
					if(found.getLastActive().getTime() > 0){
						player.setLastActive(found.getLastActive());
						
						for(TrackingGuild tracker : found.getTrackers())
							tracker.setPlayerInfo(found);
						
						break;
					}
				}
				
				if(player.getLastActive().getTime() <= 0){
					boolean success = false;
					int checksFailed = 0;
					
					while(!success){
						OsuRequest lastActiveRequest = new OsuLastActiveRequest("" + player.getUserId());
						String sRequest = (String) Main.hybridRegulator.sendRequest(lastActiveRequest);
						CustomDate uncheckedLastActive = new CustomDate(sRequest);
						
						if(sRequest.equalsIgnoreCase("failed")){
							checksFailed++;
							
							if(checksFailed > 3){
								player.setBanned(true);
								break;
							}
							
							continue;
						}
						
						if(uncheckedLastActive.getTime() > 0)
							for(TrackedPlayer found : foundModes){
								found.setLastActive(uncheckedLastActive);
								
								for(TrackingGuild tracker : found.getTrackers())
									tracker.setPlayerInfo(found);
							}
						
						success = true;
					}
				}
			}
			
			if(player.isActive() && Utils.getCurrentTimeUTC() - player.getLastActive().getTime() >= INACTIVITY_CUTOFF * 1000){
				player.setActive(false);
				inactivePlayers.add(player);
				
				change = true;
			}else if(player.isActive() && !secondCyclePlayers.contains(player) &&
					 Utils.getCurrentTimeUTC() - player.getLastActive().getTime() >= SECOND_CYCLE_CUTOFF * 1000){
				secondCyclePlayers.add(player);
			}else if(player.isActive() && secondCyclePlayers.contains(player) &&
					 Utils.getCurrentTimeUTC() - player.getLastActive().getTime() <= SECOND_CYCLE_CUTOFF * 1000){
				secondCyclePlayers.remove(player);
			}
		}
		
		if(!subsequentRestart){
			currentLoopCount++;
			
			if(secondCycleLoop){
				secondCycleLoop = false;
				currentLoopCount = 0;
				change = true;
			}
			
			if(currentLoopCount == LOOP_SKIPS_FOR_SECOND_CYCLE){
				secondCycleLoop = true;
				change = true;
			}
		}
		
		return change;
	}
	
	public static List<TrackedPlayer> getActivePlayers(){
		List<TrackedPlayer> list = new ArrayList<>();
		List<TrackedPlayer> full = new ArrayList<>(registeredPlayers);
		
		if(!secondCycleLoop) full.removeAll(secondCyclePlayers);
		
		for(TrackedPlayer player : full)
			if(player.isActive()) list.add(player);
		
		return list;
	}
	
	public static List<TrackedPlayer> getInactivePlayers(){
		List<TrackedPlayer> list = new ArrayList<>();
		List<Integer> userIds = new ArrayList<>();
		
		for(TrackedPlayer inactive : inactivePlayers)
			if(!inactive.isActive() && !userIds.contains(inactive.getUserId())){
				userIds.add(inactive.getUserId());
				list.add(inactive);
			}
		
		return list;
	}
	
	public List<TrackedPlay> fetchLatestPlays(){
		List<TrackedPlay> plays = new ArrayList<>();
		
		JSONObject jsonUser = null;
		
		if((pp <= 0 && rank <= 0 && countryRank <= 0) || leaderboardTrack){
			OsuRequest userRequest = new OsuUserRequest(RequestTypes.API, "" + userId, "" + mode);
			Object userObj = Main.hybridRegulator.sendRequest(userRequest);
			
			if(userObj != null && userObj instanceof JSONObject){
				jsonUser = (JSONObject) userObj;
				
				String stats = Utils.getOsuPlayerPPAndRank(jsonUser);
				
				pp = Utils.stringToDouble(stats.split("&r=")[0]);
				rank = Utils.stringToInt(stats.split("&r=")[1].split("&cr=")[0]);
				countryRank = Utils.stringToInt(stats.split("&cr=")[1]);
				country = jsonUser.getString("country");
				username = jsonUser.getString("username");
				
				lastRankUpdate = new CustomDate();
			}
		}

		if(leaderboardTrack && !normalTrack){
			lastUpdate = new CustomDate();
			saveInfo();
			
			return null;
		}
		
		if(!normalTrack) return plays;

		OsuRequest recentPlaysRequest = new OsuRecentPlaysRequest("" + userId, "" + mode);
		Object recentPlaysObj = Main.hybridRegulator.sendRequest(recentPlaysRequest);
		
		if(recentPlaysObj != null && recentPlaysObj instanceof JSONArray && TrackingUtils.playerHasRecentPlays(this, (JSONArray) recentPlaysObj, lastUpdate)){
			JSONArray jsonResponse = (JSONArray) recentPlaysObj;
			
			if(recentPlaysRequest.getType().equals(RequestTypes.HTML)){
				recentPlaysRequest = new OsuRecentPlaysRequest("" + userId, "" + mode);
				recentPlaysRequest.setRequestType(RequestTypes.API);
				recentPlaysObj = Main.hybridRegulator.sendRequest(recentPlaysRequest);
				
				if(recentPlaysObj != null && recentPlaysObj instanceof JSONArray)
					jsonResponse = (JSONArray) recentPlaysObj;
				else return plays;
			}
			
			if(jsonUser == null) Utils.sleep(5000);
			
			if(jsonUser == null){
				OsuRequest userRequest = new OsuUserRequest(RequestTypes.API, "" + userId, "" + mode);
				Object userObj = Main.hybridRegulator.sendRequest(userRequest);
				
				if(!(userObj instanceof JSONObject)){
					System.out.println(userObj.toString());
					return plays;
				}
				
				if(userObj != null && userObj instanceof JSONObject)
					jsonUser = (JSONObject) userObj;
			}
			
			username = jsonUser.getString("username");
			country = jsonUser.getString("country");

			// to compare fetched plays with these to find out if it the fetched play got a map leaderboard spot
			List<RecentPlay> recentPlays = TrackingUtils.fetchPlayerRecentPlays(jsonUser.getJSONArray("events"), lastUpdate);
			if(recentPlays.size() > 0) Collections.reverse(recentPlays);
			
			String updatedStats = Utils.getOsuPlayerPPAndRank(jsonUser);
			double updatedPP = Utils.df(Utils.stringToDouble(updatedStats.split("&r=")[0]));
			int updatedRank = Utils.stringToInt(updatedStats.split("&r=")[1].split("&cr=")[0]);
			int updatedCountryRank = Utils.stringToInt(updatedStats.split("&cr=")[1]);
			
			// this is not accurate if there's more than 1 play queued, please look into verifying if they are all in tops
			// and how much they are weighted to give an appropriate estimate.
			double ppDiff = updatedPP - pp;	
			int rankDiff = updatedRank - rank;
			int countryDiff = updatedCountryRank - countryRank;
			
			if(updatedPP <= 0){
				ppDiff = 0;
				rankDiff = 0;
			}
			
			if(pp != updatedPP || rank != updatedRank || countryRank != updatedCountryRank){
				if(pp != updatedPP || rank != updatedRank) {
					oldPP = pp;
					oldRank = rank;
					lastRankUpdate = new CustomDate();
				}
				
				pp = updatedPP;
				rank = updatedRank;
				countryRank = updatedCountryRank;
			}
			
			OsuRequest topPlaysRequest = new OsuTopPlaysRequest("" + userId, "" + mode);
			Object topPlaysObj = Main.hybridRegulator.sendRequest(topPlaysRequest);
			JSONArray tpJsonResponse = null;
			
			if(topPlaysObj != null && topPlaysObj instanceof JSONArray)
				tpJsonResponse = (JSONArray) topPlaysObj;

			for(int i = jsonResponse.length() - 1; i >= 0; i--){
				JSONObject jsonObj = jsonResponse.getJSONObject(i);
				TrackedPlay play = new TrackedPlay(jsonObj, mode);
				
				if(hasPostedPlayRecently(play)) continue;
				
				if(play.getDate().after(lastActive))
					setLastActive(play.getDate());
				
				if(play.getDate().after(lastUpdate)){
					if(jsonObj.getString("rank").equalsIgnoreCase("F")) continue;
					
					play.loadMap();
					play.setPPChange(ppDiff);
					play.setRankChange(rankDiff);
					play.setCountryRankChange(countryDiff);
					play.setCountry(country);
					
					if(mode == 0) play.loadPP();
					
					int mapRank = 0;
					RecentPlay recent = null;
					
					for(RecentPlay rPlay : recentPlays)
						if(rPlay.getBeatmapId() == play.getBeatmapId() && (recent == null || rPlay.getDate().after(recent.getDate()))){
							mapRank = rPlay.getRank();
							recent = rPlay;
						}
					
					if(recent != null) recentPlays.remove(recent);
					
					play.setMapRank(mapRank);
					
					int personalBest = 0;

					if(Math.abs(ppDiff) > 0 && tpJsonResponse != null){
						for(int j = 0; j < tpJsonResponse.length(); j++){
							JSONObject topPlay = tpJsonResponse.getJSONObject(j);
							
							if(topPlay.getInt("beatmap_id") == play.getBeatmapId() &&
							   topPlay.getInt("enabled_mods") == play.getRawMods() &&
							   topPlay.getLong("score") == play.getRawScore() &&
							   TrackingUtils.getAccuracy(topPlay, mode) - play.getAccuracy() <= 0.01){
								personalBest = j + 1;
								
								play.getPPInfo().setPP(topPlay.getDouble("pp"));
								play.setPersonalBestCount(personalBest);
								
								break;
							}		
						}
					}
					
					plays.add(play);
				}
			}
		}
		
		if(recentPlaysObj != null && recentPlaysObj instanceof JSONArray){
			lastUpdate = new CustomDate();
			saveInfo();
		}
		
		if(!plays.isEmpty())
			for(TrackedPlay play : plays)
				addRecentPlayPost(play);
		
		return plays;
	}
	
	private void addRecentPlayPost(TrackedPlay play){
		lastPostedPlays.add(play);
		
		if(lastPostedPlays.size() > 15) lastPostedPlays.removeFirst();
	}
	
	private boolean hasPostedPlayRecently(TrackedPlay play){
		for(TrackedPlay recent : lastPostedPlays) 
			if(recent.compare(play)) return true;
		
		return false;
	}
	
	public boolean isUpdating(){
		return updatingPlayer;
	}
	
	public void setUpdating(boolean updating){
		updatingPlayer = updating;
	}
	
	public List<TrackingGuild> getTrackers(){
		return currentlyTracking;
	}
	
	public List<TrackingGuild> getLeaderboardTrackers(){
		return leaderboardTracking;
	}
	
	public boolean isTracked(TrackingGuild guild, boolean leaderboard){
		return (currentlyTracking.contains(guild) && !leaderboard) || (leaderboardTracking.contains(guild) && leaderboard);
	}
	
	public boolean isLeaderboardTrack(){
		return leaderboardTrack;
	}
	
	public boolean isNormalTrack(){
		return normalTrack;
	}
	
	public void setLeaderboardTrack(boolean leaderboard){
		this.leaderboardTrack = leaderboard;
	}
	
	public void setNormalTrack(boolean normal){
		this.normalTrack = normal;
	}
	
	public boolean trackPlayer(TrackingGuild guild, boolean leaderboard){
		if(leaderboard && !leaderboardTracking.contains(guild)){
			leaderboardTracking.add(guild);
			
			return true;
		}else if(!leaderboard && !currentlyTracking.contains(guild)){
			currentlyTracking.add(guild);
			
			return true;
		}

		return false;
	}
	
	public void untrackPlayer(TrackingGuild guild, boolean leaderboard){
		if(leaderboard){
			leaderboardTracking.remove(guild);
			
			if(leaderboardTracking.size() == 0)
				leaderboardTrack = false;
		}else{
			currentlyTracking.remove(guild);
			
			if(currentlyTracking.size() == 0)
				normalTrack = false;
		}
	}
	
	public void setStats(String statString){
		double temp = Utils.df(Utils.stringToDouble(statString.split("&r=")[0]));
		if(temp != -1){
			if(temp != pp){
				oldPP = pp;
				lastRankUpdate = new CustomDate();
				
				if(oldPP == 0) oldPP = temp;
			}
			
			pp = temp;
		}
		
		temp = Utils.stringToInt(statString.split("&r=")[1].split("&cr=")[0]);
		if(temp != -1){
			if((int) temp != rank) {
				oldRank = rank;
				lastRankUpdate = new CustomDate();
				
				if(oldRank == 0) oldRank = (int) temp;
			}
			
			rank = (int) temp;
		}
		
		temp = Utils.stringToInt(statString.split("&cr=")[1]);
		if(temp != -1) countryRank = (int) temp;
	}
	
	public void saveInfo(){
		if(currentlyTracking.size() != 0)
			for(TrackingGuild tracker : currentlyTracking)
				tracker.setPlayerInfo(this);
	}
	
	public String getUsername(){
		return username;
	}
	
	public int getUserId(){
		return userId;
	}
	
	public int getMode(){
		return mode;
	}
	
	public double getOldPP(){
		return oldPP;
	}
	
	public double getPP(){
		return pp;
	}
	
	public int getOldRank(){
		return oldRank;
	}
	
	public int getRank(){
		return rank;
	}
	
	public int getCountryRank(){
		return countryRank;
	}
	
	public String getCountry(){
		return country;
	}
	
	public String getLastUpdateMessage(){
		return lastUpdateMessage;
	}
	
	public CustomDate getLastUpdate(){
		return lastUpdate;
	}
	
	public void setLastUpdate(CustomDate lastUpdate){
		this.lastUpdate = lastUpdate;
	}
	
	public CustomDate getLastActive(){
		return lastActive;
	}
	
	public void setLastActive(CustomDate lastActive){
		this.lastActive = lastActive;
	}
	
	public CustomDate getLastRankUpdate(){
		return lastRankUpdate;
	}
	
	public void setLastRankUpdate(CustomDate lastRankUpdate){
		this.lastRankUpdate = lastRankUpdate;
	}
	
	public boolean isActive(){
		return active;
	}
	
	public void setActive(boolean active){
		this.active = active;
	}
	
	public boolean isBanned(){
		return banned;
	}
	
	public void setBanned(boolean banned){
		this.banned = banned;
	}
	
	public static TrackedPlayer get(int userId, int mode){
		for(TrackedPlayer registered : new ArrayList<>(registeredPlayers)){
			if(registered.getUserId() == userId &&
			   registered.getMode() == mode){
				return registered;
			}
		}
		
		return null;
	}
	
	public static List<TrackedPlayer> find(int userId){
		List<TrackedPlayer> found = new ArrayList<>();
		
		for(TrackedPlayer registered : new ArrayList<>(registeredPlayers))
			if(registered.getUserId() == userId)
				found.add(registered);
		
		return found;
	}
	
	public static TrackedPlayer get(String username, int mode){
		for(TrackedPlayer registered : new ArrayList<>(registeredPlayers)){
			if(registered.getUsername().equalsIgnoreCase(username) &&
			   registered.getMode() == mode){
				return registered;
			}
		}
		
		return null;
	}
	
	public static List<TrackedPlayer> get(TrackingGuild guild, boolean leaderboard){
		List<TrackedPlayer> list = new ArrayList<>();
		
		for(TrackedPlayer registered : new ArrayList<>(registeredPlayers))
			if(registered.isTracked(guild, leaderboard)) list.add(registered);
		
		return list;
	}
}
