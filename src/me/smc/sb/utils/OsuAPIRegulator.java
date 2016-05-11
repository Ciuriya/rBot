package me.smc.sb.utils;

import java.util.HashMap;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;

public class OsuAPIRegulator{

	private static HashMap<Integer, APIRequest> requests;
	private static int requestNumber = 0;
	private static int requestsPerMinute = 30;
	private Timer requestTimer;
	
	public OsuAPIRegulator(){
		requests = new HashMap<>();
		startRequestTimer();
	}
	
	private void startRequestTimer(){
		if(requestTimer != null) requestTimer.cancel();
		
		requestTimer = new Timer();
		long delay = (long) (60000 / requestsPerMinute);
		
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
	
	public String sendRequest(String urlString, String urlParameters){
		requestNumber++;
		int number = requestNumber;
		
		APIRequest request = new APIRequest(urlString, urlParameters);
		requests.put(number, request);
		
		int timeElapsed = 0;
		
		while(request.getAnswer().equals("empty")){
			if(timeElapsed >= 30000){
				requests.remove(number);
				return "";
			}
			
			Utils.sleep(100);
			timeElapsed += 100;
		}
		
		requests.remove(number);
		
		return request.getAnswer();
	}
	
	class APIRequest{
		
		private String urlString;
		private String urlParameters;
		private String answer;
		
		public APIRequest(String urlString, String urlParameters){
			this.urlString = urlString;
			this.urlParameters = urlParameters;
			this.answer = "empty";
		}
		
		public String getAnswer(){
			return answer;
		}
		
		public void send(){
			answer = Utils.sendPost(urlString, urlParameters);
		}
	}
}
