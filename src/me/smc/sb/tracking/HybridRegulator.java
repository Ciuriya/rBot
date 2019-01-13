package me.smc.sb.tracking;

import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;

import me.smc.sb.main.Main;
import me.smc.sb.utils.Log;
import me.smc.sb.utils.Utils;

public class HybridRegulator{

	private static final int[] FIBONACCI = new int[]{ 1, 1, 2, 3, 5, 8, 13, 21 };
	public static LinkedList<OsuRequest> requests;
	public static long timeExecutingRequests = 0;
	public static int requestsSent = 0;
	private static int apiPerMinute = 150;
	private static int htmlPerMinute = 50;
	private static int apiRequestsSent = 0;
	private static int htmlRequestsSent = 0;
	private static int lastRefreshApiRequestsCount = 0;
	private static int lastRefreshHtmlRequestsCount = 0;
	private static long lastLoadRefresh = 0;
	public static double apiLoad = 0;
	public static double htmlLoad = 0;
	private Timer requestTimer;
	private Timer loadTimer;
	private boolean apiStalled;
	private boolean htmlStalled;
	private ExecutorService apiPool;
	private ExecutorService htmlPool;
	
	public HybridRegulator(){
		requests = new LinkedList<>();
		lastLoadRefresh = System.currentTimeMillis();
		apiStalled = false;
		htmlStalled = false;
		htmlPool = Executors.newSingleThreadExecutor(); //was cached thing
		apiPool = Executors.newFixedThreadPool(7);
		
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
					OsuRequest request = requests.stream().filter(r -> r.isPriority()).findAny().orElse(requests.getFirst());
					
					if(request.getType().equals(RequestTypes.HYBRID)){
						if(!apiStalled && apiLoad < 1)
							useApi = true;
						else if(!htmlStalled && htmlLoad < 1)
							useApi = false;
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
					ExecutorService service = api ? apiPool : htmlPool;
					
					service.execute(new Runnable(){
						public void run(){
							executeRequest(request, api, 0, false);
						}
					});
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
		}, 100, 100);
	}
	
	private void executeRequest(OsuRequest request, boolean api, int attempt, boolean staller){
		try{
			if(api) apiRequestsSent++;
			else htmlRequestsSent++;
			
			request.send(api);
		}catch(Exception e){
			if(attempt + 1 >= FIBONACCI.length) return;
			
			boolean stalled = false;
			
			while(!staller && ((api && apiStalled) || (!api && htmlStalled))){
				stalled = true;
				Utils.sleep(10);
			}
			
			if(stalled){
				executeRequest(request, api, attempt + 1, false);
				
				return;
			}
			
			if(api) apiStalled = true;
			else htmlStalled = true;
			
			int nextAttemptDelay = FIBONACCI[attempt + 1];
			Log.logger.log(Level.WARNING, "Retrying osu!" + (api ? "api" : "html") + " request in " + nextAttemptDelay + " seconds!\n" +
										  "Request: " + request.getName() + " Ex: " + e.getMessage());

			Utils.sleep(nextAttemptDelay * 1000);
			
			executeRequest(request, api, attempt + 1, true);
			
			if(api) apiStalled = false;
			else htmlStalled = false;
		}
	}
	
	public Object sendRequest(OsuRequest request){
		return sendRequest(request, 30000, false);
	}
	
	public Object sendRequest(OsuRequest request, boolean priority){
		return sendRequest(request, 30000, priority);
	}
	
	public Object sendRequest(OsuRequest request, int timeout, boolean priority){
		long time = System.currentTimeMillis();
		
		if(priority) request.setPrioritary();
		
		requests.add(request);
		
		int timeElapsed = 0;
		
		while(!request.isDone()){
			if(timeElapsed >= timeout && timeout > 0){
				Main.failedRequests++;
				
				requests.remove(request);
				
				return "Request: " + request.getName() + " timed out.";
			}
			
			Utils.sleep(10);
			timeElapsed += 10;
		}
		
		HybridRegulator.timeExecutingRequests += System.currentTimeMillis() - time;
		HybridRegulator.requestsSent++;
		
		return request.getAnswer();
	}
	
	private void calculateLoads(){
		long delay = System.currentTimeMillis() - lastLoadRefresh;
		
		if(delay >= 60000){
			lastLoadRefresh = System.currentTimeMillis();
			lastRefreshApiRequestsCount = apiRequestsSent;
			lastRefreshHtmlRequestsCount = htmlRequestsSent;
		}else{
			int apiRequests = apiRequestsSent - lastRefreshApiRequestsCount;
			int htmlRequests = htmlRequestsSent - lastRefreshHtmlRequestsCount;
			double timeSliceMult = 60000.0 / (double) delay;
			
			apiLoad = ((double) apiRequests * timeSliceMult) / (double) apiPerMinute;
			htmlLoad = ((double) htmlRequests * timeSliceMult) / (double) htmlPerMinute;
		}
	}
}

