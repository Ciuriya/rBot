package me.smc.sb.utils;

import java.util.HashMap;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;

import me.smc.sb.main.Main;

// this is not going to be used everywhere, only in tracking due to conflict issues
public class HTMLRegulator{

	private static HashMap<Integer, HTMLRequest> requests;
	private static int requestNumber = 0;
	private static int requestsPerMinute = 120;
	private Timer requestTimer;
	
	public HTMLRegulator(){
		requests = new HashMap<>();
		startRequestTimer();
	}
	
	private void startRequestTimer(){
		if(requestTimer != null) requestTimer.cancel();
		
		requestTimer = new Timer();
		long delay = (long) (60000.0 / (double) requestsPerMinute);
		
		requestTimer.scheduleAtFixedRate(new TimerTask(){
			public void run(){
				if(!requests.isEmpty()){
					Optional<Integer> optRequest = requests.keySet().stream().findFirst();
					
					if(optRequest.isPresent()){
						int requestNum = optRequest.get();
						
						new Thread(new Runnable(){
							@SuppressWarnings("deprecation")
							public void run(){
								requests.get(requestNum).send();
								requests.remove(requestNum);
								
								Thread.currentThread().stop();
							}
						}).start();
					}
				}
			}
		}, delay, delay);
	}
	
	public String[] sendRequest(String link){
		requestNumber++;
		int number = requestNumber;
		
		HTMLRequest request = new HTMLRequest(link);
		requests.put(number, request);
		
		int timeElapsed = 0;
		
		while(request.getAnswer().length == 0){
			if(timeElapsed >= 30000){
				requests.remove(number);
				
				return new String[]{};
			}
			
			Utils.sleep(100);
			timeElapsed += 100;
		}
		
		requests.remove(number);
		
		return request.getAnswer();
	}
	
	class HTMLRequest{
		
		private String link;
		private String[] answer;
		
		public HTMLRequest(String link){
			this.link = link;
			this.answer = new String[]{};
		}
		
		public String[] getAnswer(){
			return answer;
		}
		
		public void send(){
			answer = Utils.getHTMLCode(link);
			Main.requestsSent++;
		}
	}
}
