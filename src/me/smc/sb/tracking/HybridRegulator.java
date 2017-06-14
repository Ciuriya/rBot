package me.smc.sb.tracking;

import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;

import me.smc.sb.utils.Log;
import me.smc.sb.utils.Utils;

public class HybridRegulator{

	private static final int[] FIBONACCI = new int[]{ 1, 1, 2, 3, 5, 8, 13, 21 };
	public static LinkedList<OsuRequest> requests;
	private static int apiPerMinute = 600;
	private static int htmlPerMinute = 120;
	private static int apiRequestsCompleted = 0;
	private static int htmlRequestsCompleted = 0;
	private static int lastRefreshApiRequestsCount = 0;
	private static int lastRefreshHtmlRequestsCount = 0;
	private static long lastLoadRefresh = 0;
	public static double apiLoad = 0;
	public static double htmlLoad = 0;
	private Timer requestTimer;
	private Timer loadTimer;
	private boolean apiStalled;
	private boolean htmlStalled;
	
	public HybridRegulator(){
		requests = new LinkedList<>();
		lastLoadRefresh = System.currentTimeMillis();
		apiStalled = false;
		htmlStalled = false;
		startLoadTimer();
		startRequestTimer();
	}
	
	private void startRequestTimer(){
		if(requestTimer != null) requestTimer.cancel();
		
		requestTimer = new Timer();
		long delay = (long) (60000.0 / (double) (apiPerMinute + htmlPerMinute));
		
		requestTimer.scheduleAtFixedRate(new TimerTask(){
			public void run(){
				OsuRequest toProcess = null;
				boolean useApi = false; // false means using html
				
				if(!requests.isEmpty()){
					OsuRequest request = requests.getFirst();
					
					if(request.getType().equals(RequestTypes.HYBRID)){
						if((apiLoad < htmlLoad || htmlStalled) && !apiStalled && apiLoad < 1)
							useApi = true;
						else if((htmlLoad < apiLoad || apiStalled) && !htmlStalled && htmlLoad < 1)
							useApi = false;
						else if(apiLoad == htmlLoad && apiLoad < 1 && !apiStalled && htmlStalled)
							useApi = true;
						else if(apiLoad == htmlLoad && apiLoad < 1 && apiStalled && !htmlStalled)
							useApi = false;
						else if(apiLoad == htmlLoad && apiLoad < 1 && !apiStalled && !htmlStalled)
							useApi = true;
						else return;
						
						toProcess = request;
					}else if(request.getType().equals(RequestTypes.API) && !apiStalled && apiLoad < 1){
						toProcess = request;
						useApi = true;
					}else if(request.getType().equals(RequestTypes.HTML) && !htmlStalled && htmlLoad < 1)
						toProcess = request;
				}
				
				if(toProcess != null){
					requests.remove(toProcess);
					toProcess.setRequestType(useApi ? RequestTypes.API : RequestTypes.HTML);
					
					final OsuRequest request = toProcess;
					final boolean api = useApi;
					
					new Thread(new Runnable(){
						public void run(){
							if(api) apiStalled = true;
							else htmlStalled = true;
							
							executeRequest(request, api, 0);
							
							if(api){
								apiRequestsCompleted++;
								apiStalled = false;
							}else{
								htmlRequestsCompleted++;
								htmlStalled = false;
							}
						}
					}).start();
				}
			}
		}, delay, delay);
	}
	
	private void startLoadTimer(){
		if(loadTimer != null) loadTimer.cancel();
		
		loadTimer = new Timer();
		
		loadTimer.scheduleAtFixedRate(new TimerTask(){
			public void run(){
				calculateLoads();
			}
		}, 500, 500);
	}
	
	private void executeRequest(OsuRequest request, boolean api, int attempt){
		try{
			request.send(api);
		}catch(Exception e){
			if(attempt + 1 >= FIBONACCI.length) return;
			
			int nextAttemptDelay = FIBONACCI[attempt + 1];
			Log.logger.log(Level.WARNING, "Retrying osu!" + (api ? "api" : "html") + " request in " + nextAttemptDelay + " seconds!\n" +
										  "Request: " + request.getName() + " Ex: " + e.getMessage());
			Utils.sleep(nextAttemptDelay * 1000);
			
			executeRequest(request, api, attempt + 1);
		}
	}
	
	public Object sendRequest(OsuRequest request){
		return sendRequest(request, 30000, false);
	}
	
	public Object sendRequest(OsuRequest request, boolean priority){
		return sendRequest(request, 30000, priority);
	}
	
	public Object sendRequest(OsuRequest request, int timeout, boolean priority){
		if(priority) requests.addFirst(request);
		else requests.add(request);
		
		int timeElapsed = 0;
		
		while(!request.isDone()){
			if(timeElapsed >= timeout && timeout > 0){
				requests.remove(request);
				
				return "Request: " + request.getName() + " timed out.";
			}
			
			Utils.sleep(10);
			timeElapsed += 10;
		}
		
		return request.getAnswer();
	}
	
	private void calculateLoads(){
		long delay = System.currentTimeMillis() - lastLoadRefresh;
		
		if(delay >= 60000){
			lastLoadRefresh = System.currentTimeMillis();
			lastRefreshApiRequestsCount = apiRequestsCompleted;
			lastRefreshHtmlRequestsCount = htmlRequestsCompleted;
		}else{
			int apiRequests = apiRequestsCompleted - lastRefreshApiRequestsCount;
			int htmlRequests = htmlRequestsCompleted - lastRefreshHtmlRequestsCount;
			double timeSliceMult = 60000.0 / (double) delay;
			
			apiLoad = ((double) apiRequests * timeSliceMult) / (double) apiPerMinute;
			htmlLoad = ((double) htmlRequests * timeSliceMult) / (double) htmlPerMinute;
		}
	}
}

