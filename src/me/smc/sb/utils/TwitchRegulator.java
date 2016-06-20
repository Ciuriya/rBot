package me.smc.sb.utils;

import java.util.HashMap;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;

import me.smc.sb.main.Main;

public class TwitchRegulator{

	private int period = 30;
	private int maximumMessages = 20;
	private static int requestNumber = 0;
	private static HashMap<Integer, String> requests;
	private Timer requestTimer;
	
	public TwitchRegulator(){
		requests = new HashMap<>();
		startRequestTimer();
	}
	
	private void startRequestTimer(){
		if(requestTimer != null) requestTimer.cancel();
		
		requestTimer = new Timer();
		long delay = (long) (period * 1000 / maximumMessages);
		
		requestTimer.scheduleAtFixedRate(new TimerTask(){
			public void run(){
				if(!requests.isEmpty()){
					Optional<Integer> optRequest = requests.keySet().stream().findFirst();
					
					if(optRequest.isPresent()){
						int requestNum = optRequest.get();
						
						new Thread(new Runnable(){
							public void run(){
								String full = requests.get(requestNum);
								requests.remove(requestNum);
								
								String channel = "#" + full.split(" ")[0];
								String message = full.replace(full.split(" ")[0] + " ", "");
								
								try{
									Main.twitchBot.sendIRC().joinChannel(channel);
								}catch(Exception ex){
									Log.logger.log(Level.INFO, "Could not join channel " + channel);
								}
								
								Main.twitchBot.sendIRC().message(channel, message);
								Log.logger.log(Level.INFO, "{Twitch message sent in channel " + channel + "} " + message);
								
								try{
									Main.twitchBot.sendIRC().joinChannel(channel);
								}catch(Exception ex){
									Log.logger.log(Level.INFO, "Could not join channel " + channel);
								}
							}
						}).start();
					}
				}
			}
		}, delay, delay);
	}
	
	public void sendMessage(String channel, String message){
		requestNumber++;
		int number = requestNumber;

		requests.put(number, channel + " " + message);
	}
	
}
