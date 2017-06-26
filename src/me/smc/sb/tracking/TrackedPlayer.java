package me.smc.sb.tracking;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import org.json.JSONArray;
import org.json.JSONObject;

import me.smc.sb.main.Main;
import me.smc.sb.utils.Log;
import me.smc.sb.utils.Utils;

public class TrackedPlayer{

	public static List<TrackedPlayer> registeredPlayers = new ArrayList<>();
	public static boolean changeOccured = false;
	public static final int API_FETCH_PLAY_LIMIT = 10;
	
	private String username;
	private int userId;
	private int mode;
	private double pp;
	private int rank;
	private int countryRank;
	private String country;
	private boolean updatingPlayer;
	private String lastUpdateMessage;
	private CustomDate lastUpdate;
	private List<TrackingGuild> currentlyTracking;
	
	public TrackedPlayer(int userId, int mode){
		this(Utils.getOsuPlayerName(userId, true), userId, mode);
	}
	
	public TrackedPlayer(String username, int userId, int mode){
		this.username = username;
		this.userId = userId;
		this.mode = mode;
		pp = 0;
		rank = 0;
		countryRank = 0;
		lastUpdate = new CustomDate();
		updatingPlayer = false;
		currentlyTracking = new ArrayList<>();
		country = "A2";
		
		registeredPlayers.add(this);
		changeOccured = true;
	}
	
	public static boolean updateRegisteredPlayers(){
		boolean change = false;
		
		for(TrackedPlayer player : new ArrayList<>(registeredPlayers)){
			if(player.getTrackers().size() == 0){
				registeredPlayers.remove(player);
				change = true;
			}
		}
		
		return change;
	}
	
	public List<TrackedPlay> fetchLatestPlays(){
		List<TrackedPlay> plays = new ArrayList<>();
		
		JSONObject jsonUser = null;
		
		if(pp <= 0 && rank <= 0 && countryRank <= 0){
			OsuRequest userRequest = new OsuUserRequest(RequestTypes.API, "" + userId, "" + mode);
			Object userObj = Main.hybridRegulator.sendRequest(userRequest);
			
			if(userObj != null && userObj instanceof JSONObject){
				jsonUser = (JSONObject) userObj;
				
				String stats = Utils.getOsuPlayerPPAndRank(jsonUser);
				
				pp = Utils.stringToDouble(stats.split("&r=")[0]);
				rank = Utils.stringToInt(stats.split("&r=")[1].split("&cr=")[0]);
				countryRank = Utils.stringToInt(stats.split("&cr=")[1]);
			}
		}
		
		OsuRequest recentPlaysRequest = new OsuRecentPlaysRequest("" + userId, "" + mode);
		Object recentPlaysObj = Main.hybridRegulator.sendRequest(recentPlaysRequest);
		
		if(recentPlaysObj != null && recentPlaysObj instanceof JSONArray && TrackingUtils.playerHasRecentPlays((JSONArray) recentPlaysObj, lastUpdate)){
			JSONArray jsonResponse = (JSONArray) recentPlaysObj;
			
			if(recentPlaysRequest.getType().equals(RequestTypes.HTML)){
				recentPlaysRequest = new OsuRecentPlaysRequest("" + userId, "" + mode);
				recentPlaysRequest.setRequestType(RequestTypes.API);
				recentPlaysObj = Main.hybridRegulator.sendRequest(recentPlaysRequest, true);
				
				if(recentPlaysObj != null && recentPlaysObj instanceof JSONArray)
					jsonResponse = (JSONArray) recentPlaysObj;
				else return plays;
			}
			
			if(jsonUser == null) Utils.sleep(5000);
			
			if(jsonUser == null){
				OsuRequest userRequest = new OsuUserRequest(RequestTypes.API, "" + userId, "" + mode);
				Object userObj = Main.hybridRegulator.sendRequest(userRequest, true);
				
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
				pp = updatedPP;
				rank = updatedRank;
				countryRank = updatedCountryRank;
			}
			
			OsuRequest topPlaysRequest = new OsuTopPlaysRequest("" + userId, "" + mode);
			Object topPlaysObj = Main.hybridRegulator.sendRequest(topPlaysRequest, true);
			JSONArray tpJsonResponse = null;
			
			if(topPlaysObj != null && topPlaysObj instanceof JSONArray)
				tpJsonResponse = (JSONArray) topPlaysObj;

			for(int i = jsonResponse.length() - 1; i >= 0; i--){	
				JSONObject jsonObj = jsonResponse.getJSONObject(i);
				TrackedPlay play = new TrackedPlay(jsonObj, mode);
				
				if(play.getDate().after(lastUpdate)){
					if(jsonObj.getString("rank").equalsIgnoreCase("F")) continue;
					
					play.loadMap();
					play.setPPChange(ppDiff);
					play.setRankChange(rankDiff);
					play.setCountryRankChange(countryDiff);
					play.setCountry(country);
					
					PPInfo oppaiPP = new PPInfo(0, 0);
					
					if(mode == 0){
						try{
							int combo = 0;
							
							if(!play.isPerfect()) combo = play.getCombo();
							
							oppaiPP = TrackingUtils.fetchPPFromOppai(play.getBeatmapId(),
																     play.getBeatmapSetId(), 
																     play.getAccuracy(),
																     combo, 
																     play.getModDisplay(), 
																     play.getMisses(),
																     play.getFifties(),
																     play.getHundreds());
						}catch(Exception e){
							Log.logger.log(Level.INFO, "Could not load peppers: " + e.getMessage());
						}
					}
					
					int mapRank = 0;
					RecentPlay recent = null;
					
					for(RecentPlay rPlay : recentPlays)
						if(rPlay.getBeatmapId() == play.getBeatmapId() && rPlay.isDateValid(play.getDate(), 5)){
							mapRank = rPlay.getRank();
							recent = rPlay;
							
							break;
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
							   TrackingUtils.getAccuracy(topPlay, mode) == play.getAccuracy()){
								personalBest = j + 1;
								
								oppaiPP = new PPInfo(topPlay.getDouble("pp"), oppaiPP.getPPForFC());
								play.setPersonalBestCount(personalBest);
								
								break;
							}		
						}
					}
					
					play.setPPInfo(oppaiPP);
					plays.add(play);
				}
			}
		}
		
		lastUpdate = new CustomDate();
		
		saveInfo();
		
		return plays;
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
	
	public boolean isTracked(TrackingGuild guild){
		return currentlyTracking.contains(guild);
	}
	
	public boolean trackPlayer(TrackingGuild guild){
		if(!currentlyTracking.contains(guild)){
			currentlyTracking.add(guild);
			
			return true;
		}
		
		return false;
	}
	
	public void untrackPlayer(TrackingGuild guild){
		currentlyTracking.remove(guild);
	}
	
	public void setStats(String statString){
		double temp = Utils.df(Utils.stringToDouble(statString.split("&r=")[0]));
		if(temp != -1) pp = temp;
		
		temp = Utils.stringToInt(statString.split("&r=")[1].split("&cr=")[0]);
		if(temp != -1) rank = (int) temp;
		
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
	
	public double getPP(){
		return pp;
	}
	
	public int getRank(){
		return rank;
	}
	
	public int getCountryRank(){
		return countryRank;
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
	
	public static TrackedPlayer get(int userId, int mode){
		for(TrackedPlayer registered : new ArrayList<>(registeredPlayers)){
			if(registered.getUserId() == userId &&
			   registered.getMode() == mode){
				return registered;
			}
		}
		
		return null;
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
}
