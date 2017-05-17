package me.smc.sb.utils;

import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;

import me.smc.sb.main.Main;

public class OsuAPIRegulator{

	private static final int[] FIBONACCI = new int[]{ 1, 1, 2, 3, 5, 8, 13, 21 };
	private static LinkedList<APIRequest> requests;
	private static int requestNumber = 0;
	private static int requestsPerMinute = 900;
	private Timer requestTimer;
	private boolean stalled;
	
	public OsuAPIRegulator(){
		requests = new LinkedList<>();
		stalled = false;
		startRequestTimer();
	}
	
	private void startRequestTimer(){
		if(requestTimer != null) requestTimer.cancel();
		
		requestTimer = new Timer();
		long delay = (long) (60000.0 / (double) requestsPerMinute);
		
		requestTimer.scheduleAtFixedRate(new TimerTask(){
			public void run(){
				if(!stalled && !requests.isEmpty()){
					APIRequest request = requests.getFirst();
					requests.remove(request);
					
					stalled = true; // stall the request loop while we're requesting 
									// so that they don't pile up if osu! dies
					
					executeRequest(request, 0);
					
					stalled = false;
				}
			}
		}, delay, delay);
	}
	
	private void executeRequest(APIRequest request, int attempt){
		try{
			request.send();
		}catch(Exception e){
			if(attempt + 1 >= FIBONACCI.length) return;
			
			int nextAttemptDelay = FIBONACCI[attempt + 1];
			Log.logger.log(Level.WARNING, "Retrying osu!api request in " + nextAttemptDelay + " seconds!");
			Utils.sleep(nextAttemptDelay * 1000);
			
			executeRequest(request, attempt + 1);
		}
	}
	
	public String sendRequest(String urlString, String urlParameters){
		return sendRequest(urlString, urlParameters, 30000, false);
	}
	
	public String sendRequest(String urlString, String urlParameters, boolean priority){
		return sendRequest(urlString, urlParameters, 30000, priority);
	}
	
	public String sendRequest(String urlString, String urlParameters, int timeout, boolean priority){
		requestNumber++;
		
		APIRequest request = new APIRequest(requestNumber, urlString, urlParameters);
		
		if(priority) requests.addFirst(request);
		else requests.add(request);
		
		int timeElapsed = 0;
		
		while(!request.isDone()){
			if(timeElapsed >= timeout && timeout > 0){
				return "";
			}
			
			Utils.sleep(100);
			timeElapsed += 100;
		}
		
		return request.getAnswer();
	}
	
	class APIRequest{
		private int requestNumber;
		private String urlString;
		private String urlParameters;
		private String answer;
		private boolean done;
		
		public APIRequest(int requestNumber, String urlString, String urlParameters){
			this.requestNumber = requestNumber;
			this.urlString = urlString;
			this.urlParameters = urlParameters;
			this.answer = "";
			this.done = false;
		}
		
		public String getAnswer(){
			return answer;
		}
		
		public int getRequestNumber(){
			return requestNumber;
		}
		
		public boolean isDone(){
			return done;
		}
		
		public void send() throws Exception{
			answer = Utils.sendPost(urlString, urlParameters);
			done = true;
			Main.requestsSent++;
		}
	}
}
