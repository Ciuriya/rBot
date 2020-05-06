package managers;

import java.util.Timer;
import java.util.TimerTask;

import data.DiscordActivities;
import main.Main;
import net.dv8tion.jda.api.entities.Activity;
import utils.Constants;

/**
 * This class manages the discord bot's current activity status.
 * 
 * @author Smc
 */
public class DiscordActivityManager {
	
	private static DiscordActivityManager instance;
	private int m_currentIndex;
	private Timer m_rotationTimer;
	
	public static DiscordActivityManager getInstance() {
		if(instance == null) instance = new DiscordActivityManager();
		
		return instance;
	}
	
	public DiscordActivityManager() { 
		start();
	}
	
	private void start() {
		m_rotationTimer = new Timer();
		
		m_rotationTimer.scheduleAtFixedRate(new TimerTask() {
			public void run() {
				if(m_currentIndex == DiscordActivities.values().length)
					m_currentIndex = 0;
				
				Main.discordApi.getPresence().setActivity(Activity.playing(DiscordActivities.values()[m_currentIndex].getActivity()));
				
				m_currentIndex++;
			}
		}, 0, Constants.ACTIVITY_ROTATION_INTERVAL * 1000);
	}
}
