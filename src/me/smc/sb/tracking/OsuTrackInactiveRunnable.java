package me.smc.sb.tracking;

import java.util.ArrayList;
import java.util.List;
import java.util.TimerTask;
import java.util.logging.Level;

import me.smc.sb.discordcommands.OsuTrackCommand;
import me.smc.sb.main.Main;
import me.smc.sb.utils.Log;

public class OsuTrackInactiveRunnable extends TimerTask{
	
	private List<TrackedPlayer> playersToRefresh;
	private List<TrackedPlayer> refreshing;
	
	public OsuTrackInactiveRunnable(List<TrackedPlayer> inactives){
		playersToRefresh = new ArrayList<>(inactives);
		refreshing = new ArrayList<>();
	}
	
	@Override
	public void run(){
		if(playersToRefresh.isEmpty() && refreshing.isEmpty()){
			TrackedPlayer.inactivityRefreshDone = true;
			OsuTrackCommand.inactiveTimer.cancel();
			stop();
			
			return;
		}
		
		TrackedPlayer player = null;
		
		try{
			player = playersToRefresh.get(0);
		}catch(Exception ex){
			return;
		}
		
		if(player == null) return;
		
		refreshing.add(player);
		playersToRefresh.remove(player);
		
		final TrackedPlayer fPlayer = player;
		
		new Thread(new Runnable(){
			public void run(){
				try{
					OsuRequest lastActiveRequest = new OsuLastActiveRequest("" + fPlayer.getUserId());
					CustomDate uncheckedLastActive = new CustomDate((String) Main.hybridRegulator.sendRequest(lastActiveRequest));
					
					if(uncheckedLastActive.after(fPlayer.getLastActive())){
						for(TrackedPlayer player : TrackedPlayer.find(fPlayer.getUserId())){
							player.setLastActive(uncheckedLastActive);
							
							for(TrackingGuild tracker : player.getTrackers())
								tracker.setPlayerInfo(player);
						}
					}
				}catch(Exception e){
					Log.logger.log(Level.SEVERE, "osu!inactivity exception: " + e.getMessage());
					e.printStackTrace();
				}
				
				refreshing.remove(fPlayer);
			}
		}).start();
	}
	
	@SuppressWarnings("deprecation")
	public void stop(){
		Thread.currentThread().stop();
	}
}
