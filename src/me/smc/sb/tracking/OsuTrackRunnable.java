package me.smc.sb.tracking;

import java.util.ArrayList;
import java.util.List;
import java.util.TimerTask;
import java.util.logging.Level;

import me.smc.sb.discordcommands.OsuTrackCommand;
import me.smc.sb.utils.Log;

public class OsuTrackRunnable extends TimerTask{
	
	// use querying one day to check and speed this up
	
	private List<TrackedPlayer> playersToRefresh;
	private OsuTrackCommand trackManager;
	
	public OsuTrackRunnable(OsuTrackCommand trackManager){
		playersToRefresh = new ArrayList<>();
		this.trackManager = trackManager;
	}
	
	@Override
	public void run(){	
		// if list changed, restart to get updated timer period
		if(TrackedPlayer.changeOccured){
			TrackedPlayer.changeOccured = false;
			trackManager.startTracker();
			stop();
			
			return;
		}
		
		if(playersToRefresh.isEmpty()){
			boolean change = TrackedPlayer.updateRegisteredPlayers();
			
			// again, if change, restart for updated timer period, although this one
			// doesn't really stop normal tracking, it's more for when someone untracks
			// and that player isn't tracked anywhere else. a clean-up of sorts.
			if(change){
				TrackedPlayer.changeOccured = false;
				trackManager.startTracker();
				stop();
				
				return;
			}
			
			playersToRefresh = new ArrayList<>(TrackedPlayer.registeredPlayers);
		}
		
		TrackedPlayer player = playersToRefresh.get(0);
		playersToRefresh.remove(player);
		
		if(player.isUpdating() || player.getTrackers().isEmpty()) return;
		
		player.setUpdating(true);
		
		new Thread(new Runnable(){
			public void run(){
				try{
					List<TrackedPlay> plays = player.fetchLatestPlays();
					
					if(plays.size() > 0){
						for(TrackingGuild guild : player.getTrackers()){
							boolean onlyBest = guild.onlyTrackingBestPlays();
							int minimumPP = guild.getMinimumPPAmount();
							
							for(TrackedPlay play : plays){
								if((onlyBest && play.isPersonalBest() || !onlyBest) && (play.getPP() >= minimumPP || minimumPP <= 0)){
									guild.getPlayFormat().send(guild, play, player);
								}
							}
						}
					}
				}catch(Exception e){
					Log.logger.log(Level.SEVERE, "osu!track exception: " + e.getMessage());
					e.printStackTrace();
				}
				
				player.setUpdating(false);
			}
		}).start();
	}
	
	@SuppressWarnings("deprecation")
	public void stop(){
		Thread.currentThread().stop();
	}
}
