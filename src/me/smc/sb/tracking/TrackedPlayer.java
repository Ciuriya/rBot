package me.smc.sb.tracking;

import java.util.List;

public class TrackedPlayer{

	public static List<TrackedPlayer> registeredPlayers;
	
	public String username;
	public int userId;
	public int mode;
	public CustomDate lastUpdate;
	public List<TrackingGuild> currentlyTracking;
	
	public TrackedPlayer(){
		
	}
	
}
