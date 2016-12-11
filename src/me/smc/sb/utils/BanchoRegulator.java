package me.smc.sb.utils;

import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;

import me.smc.sb.main.Main;

public class BanchoRegulator {

	private int period = 60;
	private int maximumMessages = 60;
	private static int requestNumber = 0, priorityRequestNumber;
	private static HashMap<Integer, String> requests;
	private static HashMap<Integer, String> priorityRequests;
	private Timer requestTimer;
	private Timer priorityTimer;
	
	public BanchoRegulator(){
		requests = new HashMap<>();
		priorityRequests = new HashMap<>();
		
		startRequestTimer();
	}
	
	private void startRequestTimer(){
		if(requestTimer != null) requestTimer.cancel();
		if(priorityTimer != null) priorityTimer.cancel();
		
		requestTimer = new Timer();
		priorityTimer = new Timer();
		long delay = (long) (period * 1000 / maximumMessages);
		
		requestTimer.scheduleAtFixedRate(new TimerTask(){
			public void run(){
				if(!requests.isEmpty()){
					int request = requests.keySet().stream().min(Integer::compare).orElse(-1);
					
					if(request != -1){
						int requestNum = request;
						
						new Thread(new Runnable(){
							public void run(){
								sendRequestToIRC(requestNum, -1);
							}
						}).start();
					}
				}
			}
		}, delay, delay);
		
		priorityTimer.scheduleAtFixedRate(new TimerTask(){
			public void run(){
				if(!priorityRequests.isEmpty()){
					int request = priorityRequests.keySet().stream().min(Integer::compare).orElse(-1);
					
					if(request != -1){
						int requestNum = request;
						
						new Thread(new Runnable(){
							public void run(){
								sendRequestToIRC(-1, requestNum);
							}
						}).start();
					}
				}
			}
		}, delay + (delay / 2), delay);
	}
	
	private void sendRequestToIRC(int requestNum, int priorityRequestNum){
		String full = "";
		
		if(requestNum == -1){
			full = priorityRequests.get(priorityRequestNum);
			priorityRequests.remove(priorityRequestNum);
		}else{
			full = requests.get(requestNum);
			requests.remove(requestNum);
		}
		
		String channel = full.split(" ")[0];
		String message = full.replace(full.split(" ")[0] + " ", "");
		
		Main.ircBot.sendIRC().message(channel, message);
		Log.logger.log(Level.INFO, "IRC/Sent in " + channel + ": " + message);
	}
	
	public void sendPriorityMessage(String channel, String message){
		priorityRequestNumber++;
		int number = priorityRequestNumber;
		
		priorityRequests.put(number, channel + " " + message);
	}
	
	public void sendMessage(String channel, String message){
		requestNumber++;
		int number = requestNumber;

		requests.put(number, channel + " " + message);
	}
}
