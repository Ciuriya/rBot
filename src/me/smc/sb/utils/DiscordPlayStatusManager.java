package me.smc.sb.utils;

import java.util.Timer;
import java.util.TimerTask;

import me.smc.sb.main.Main;
import net.dv8tion.jda.core.entities.Game;

public class DiscordPlayStatusManager{

	public static final int STATUS_ROTATION_INTERVAL = 15; // seconds
	private int currentIndex;
	private Timer rotationTimer;
	
	public DiscordPlayStatusManager(){
		currentIndex = 0;
		rotationTimer = null;
		
		start();
	}
	
	public void start(){
		if(rotationTimer != null){
			rotationTimer.cancel();
			rotationTimer = null;
		}
		
		rotationTimer = new Timer();
		rotationTimer.scheduleAtFixedRate(new TimerTask(){
			public void run(){
				if(currentIndex == DiscordStatuses.values().length)
					currentIndex = 0;
				
				if(!DiscordStatuses.values()[currentIndex].canParse()){
					while(!DiscordStatuses.values()[currentIndex].canParse()){
						incrementCurrentIndex();
					}
					
					if(!DiscordStatuses.values()[currentIndex].canParse())
						incrementCurrentIndex();
				}
				
				Main.api.getPresence().setGame(Game.of(DiscordStatuses.values()[currentIndex].getStatus()));
				
				currentIndex++;
			}
		}, 0, STATUS_ROTATION_INTERVAL * 1000);
	}
	
	public void incrementCurrentIndex(){
		if(currentIndex == DiscordStatuses.values().length) currentIndex = 0;
		else currentIndex++;
	}
}
