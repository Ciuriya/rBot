package me.smc.sb.tracking;

import java.util.ArrayList;
import java.util.List;

public class OsuTrackScheduler implements Runnable{

	// go through player list dynamically, not just a for loop in here, use a counter or whatever?
	// or just detect if list changes every run of the loop (static boolean playerAdded in TrackedPlayer?) and adjust loop
	
	private List<TrackedPlayer> playersToRefresh;
	
	public OsuTrackScheduler(){
		playersToRefresh = new ArrayList<>();
	}
	
	@Override
	public void run(){
		if(TrackedPlayer.changeOccured){
			TrackedPlayer.changeOccured = false;
			playersToRefresh = new ArrayList<>(TrackedPlayer.registeredPlayers);
		}
		
		TrackedPlayer player = playersToRefresh.get(0);
		playersToRefresh.remove(player);
		
		// this needs to run in a new thread?
		List<TrackedPlay> plays = player.fetchLatestPlays();
		
		if(plays.size() > 0){
			for(TrackingGuild guild : player.getTrackers()){
				// post in guild using their own format
			}
		}
	}
	
	public void stop(){
		// idk do something here
		// Thread.currentThread().stop();
	}
}
