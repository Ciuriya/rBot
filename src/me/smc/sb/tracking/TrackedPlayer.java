package me.smc.sb.tracking;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import me.smc.sb.discordcommands.OsuStatsCommand;
import me.smc.sb.main.Main;
import me.smc.sb.utils.Utils;

public class TrackedPlayer{

	public static List<TrackedPlayer> registeredPlayers;
	public static boolean changeOccured = false;
	public static final int API_FETCH_PLAY_LIMIT = 10;
	
	private String username;
	private int userId;
	private int mode;
	private double pp;
	private int rank;
	private int countryRank;
	private String lastUpdateMessage;
	private CustomDate lastUpdate;
	private List<TrackingGuild> currentlyTracking;
	
	public TrackedPlayer(int userId, int mode){
		this(Utils.getOsuPlayerName(userId), userId, mode);
	}
	
	public TrackedPlayer(String username, int userId, int mode){
		this.username = username;
		this.userId = userId;
		this.mode = mode;
		pp = 0;
		rank = 0;
		countryRank = 0;
		lastUpdate = new CustomDate();
		
		registeredPlayers.add(this);
		changeOccured = true;
	}
	
	public List<TrackedPlay> fetchLatestPlays(){
		List<TrackedPlay> plays = new ArrayList<>();
		
		if(TrackingUtils.playerHasRecentPlays(userId, mode, lastUpdate)){
			// fetching plays straight away, otherwise we'd be wasting efficiency in scraping/api stuff
			String post = Main.osuRequestManager.sendRequest("https://osu.ppy.sh/api/", "get_user_recent?k=" + OsuStatsCommand.apiKey + 
                    	  "&u=" + userId + "&m=" + mode + "&limit=" + API_FETCH_PLAY_LIMIT + "&type=string&event_days=1");
			
			if(post == "" || !post.contains("{")) return plays;
			
			JSONArray jsonResponse = new JSONArray(post);
			
			String[] pageGeneral = Utils.getHTMLCode("https://osu.ppy.sh/pages/include/profile-general.php?u=" + userId + "&m=" + mode);
			
			// this fixes the username if it changed
			List<String> userLine = Utils.getNextLineCodeFromLink(pageGeneral, 0, "&find=");
			
			if(userLine.size() > 0)
				username = userLine.get(0).split("&find=")[1].split("&")[0];
			
			// to compare fetched plays with these to find out if it the fetched play got a map leaderboard spot
			List<RecentPlay> recentPlays = TrackingUtils.fetchPlayerRecentPlays(pageGeneral, lastUpdate);
			
			// the reason this is done without using the general page is to get decimals on the pp value
			String updatedStats = Utils.getOsuPlayerPPAndRank(String.valueOf(userId), mode);
			double updatedPP = Utils.stringToDouble(updatedStats.split("&r=")[0]);
			int updatedRank = Utils.stringToInt(updatedStats.split("&r=")[1].split("&cr=")[0]);
			int updatedCountryRank = Utils.stringToInt(updatedStats.split("&cr=")[1]);
			
			for(int i = jsonResponse.length() - 1; i >= 0; i--){		
				JSONObject jsonObj = jsonResponse.getJSONObject(i);
				TrackedPlay play = new TrackedPlay();
				CustomDate playDate = new CustomDate(TrackingUtils.osuDateToCurrentDate(jsonObj.getString("date")));
				
				if(playDate.after(lastUpdate)){
					if(jsonObj.getString("rank").equalsIgnoreCase("F")) continue;
					
					Utils.sleep(2500);
					
					// needs more work...
				}
			}
		}
		
		return plays;
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
		double temp = Utils.stringToDouble(statString.split("&r=")[0]);
		if(temp != -1) pp = temp;
		
		temp = Utils.stringToInt(statString.split("&r=")[1].split("&cr=")[0]);
		if(temp != -1) rank = (int) temp;
		
		temp = Utils.stringToInt(statString.split("&cr=")[1]);
		if(temp != -1) countryRank = (int) temp;
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
