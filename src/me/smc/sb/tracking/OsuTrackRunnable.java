package me.smc.sb.tracking;

import java.util.ArrayList;
import java.util.List;
import java.util.TimerTask;
import java.util.logging.Level;

import me.smc.sb.discordcommands.OsuTrackCommand;
import me.smc.sb.utils.Log;

public class OsuTrackRunnable extends TimerTask{
	
	private List<TrackedPlayer> playersToRefresh;
	private OsuTrackCommand trackManager;
	public static int trackedTotal = 0;
	public static long totalTimeUsed = 0;
	private List<TrackedPlayer> updating;
	private List<Thread> updateThreads;
	private long stuckTime;
	private boolean subsequentRestart;
	
	public OsuTrackRunnable(OsuTrackCommand trackManager, boolean subsequentRestart){
		playersToRefresh = new ArrayList<>();
		updating = new ArrayList<>();
		updateThreads = new ArrayList<>();
		stuckTime = 0;
		this.subsequentRestart = subsequentRestart;
		this.trackManager = trackManager;
	}
	
	@SuppressWarnings("deprecation")
	@Override
	public void run(){
		// if list changed, restart to get updated timer period
		if(TrackedPlayer.changeOccured){
			TrackedPlayer.changeOccured = false;
			trackManager.startTracker(false);
			stop();
			
			return;
		}
		
		if(playersToRefresh.isEmpty() && updating.isEmpty()){
			boolean change = TrackedPlayer.updateRegisteredPlayers(subsequentRestart);
			
			// again, if change, restart for updated timer period, although this one
			// doesn't really stop normal tracking, it's more for when someone untracks
			// and that player isn't tracked anywhere else. a clean-up of sorts.
			if(change){
				TrackedPlayer.changeOccured = false;
				trackManager.startTracker(true);
				stop();
				
				return;
			}
			
			subsequentRestart = false;
			playersToRefresh = new ArrayList<>(TrackedPlayer.getActivePlayers());
		}else if(updating.size() > 0 && playersToRefresh.isEmpty()){
			if(stuckTime == 0) stuckTime = System.currentTimeMillis();
			else if(System.currentTimeMillis() - stuckTime > 10000){
				if(!updateThreads.isEmpty())
					for(Thread t : updateThreads) t.stop();
				
				for(TrackedPlayer updatePlayer : updating)
					updatePlayer.setUpdating(false);
				
				updateThreads.clear();
				updating.clear();
				
				stuckTime = 0;
			}

			return;
		}
		
		TrackedPlayer player = null;
		
		try{
			player = playersToRefresh.get(0);
		}catch(Exception ex){
			return;
		}
		
		if(player == null) return;

		playersToRefresh.remove(player);
		
		if(player.isUpdating() || (player.getTrackers().isEmpty() && player.getLeaderboardTrackers().isEmpty())) return;
		
		player.setUpdating(true);
		updating.add(player);
		
		final TrackedPlayer fPlayer = player;
		
		Thread t = new Thread(new Runnable(){
			public void run(){
				try{
					long time = System.currentTimeMillis();
					List<TrackedPlay> plays = fPlayer.fetchLatestPlays();
					
					totalTimeUsed += System.currentTimeMillis() - time;
					trackedTotal++;
					
					if(plays != null && plays.size() > 0 && fPlayer.getTrackers().size() > 0){
						for(TrackingGuild guild : fPlayer.getTrackers()){
							boolean onlyBest = guild.onlyTrackingBestPlays();
							int minimumPP = guild.getMinimumPPAmount();
							
							for(TrackedPlay play : plays){
								if((onlyBest && play.isPersonalBest() || !onlyBest) && (play.getPP() >= minimumPP || minimumPP <= 0)){
									guild.getPlayFormat().send(guild, play, fPlayer);
								}
							}
						}
					}
				}catch(Exception e){
					Log.logger.log(Level.SEVERE, "osu!track exception: " + e.getMessage());
					e.printStackTrace();
				}
				
				fPlayer.setUpdating(false);
				updating.remove(fPlayer);
				updateThreads.remove(this);
			}
		});
		
		updateThreads.add(t);
		t.start();
	}
	
	@SuppressWarnings("deprecation")
	public void stop(){
		Thread.currentThread().stop();
	}
}
