package osu.api;

import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.Timer;
import java.util.TimerTask;

import managers.ApplicationStats;
import managers.ThreadingManager;
import utils.Constants;
import utils.GeneralUtils;

/**
 * This class manages osu! api/html requests.
 * It ensures they don't overload osu!'s api/website and are prioritized properly.
 * 
 * @author Smc
 */
public class OsuRequestRegulator {
	
	private static OsuRequestRegulator instance;
	private LinkedList<OsuRequest> m_apiRequests;
	private LinkedList<OsuRequest> m_htmlRequests;
	private boolean m_apiStalled;
	private boolean m_htmlStalled;
	
	public static OsuRequestRegulator getInstance() {
		if(instance == null) instance = new OsuRequestRegulator();
		
		return instance;
	}
	
	public OsuRequestRegulator() {
		m_apiRequests = new LinkedList<>();
		m_htmlRequests = new LinkedList<>();
		
		startRequestTimer(true);
		startRequestTimer(false);
	}
	
	// this is for both api and html, so p_isApi is just to distinguish
	// and use the proper linked list
	// they're separate so they can have their own timer on their own speed
	// based on the amount of requests they can do using their request type
	private void startRequestTimer(boolean p_isApi) {
		long delay = (long) (60000.0 / (float) (p_isApi ? Constants.OSU_API_REQUESTS_PER_MINUTE :
														  Constants.OSU_HTML_REQUESTS_PER_MINUTE));
		
		new Timer().scheduleAtFixedRate(new TimerTask() {
			public void run() {
				// if we have a request that can't send, stall the timer until it works out
				if((p_isApi && m_apiStalled) || (!p_isApi && m_htmlStalled)) return;
				
				OsuRequest toProcess = null;
				
				try {
					toProcess = p_isApi ? m_apiRequests.getFirst() : m_htmlRequests.getFirst();
					
					if(p_isApi) m_apiRequests.remove();
					else m_htmlRequests.remove();
				} catch(NoSuchElementException e) { }
				
				if(toProcess != null) {
					final OsuRequest request = toProcess;
					
					ThreadingManager.getInstance().executeAsync(new Runnable() {
						public void run() {
							int attempts = 0;
							
							while(attempts < Constants.FIBONACCI.length) {
								try {
									request.send();
									break;
								} catch(Exception e) {
									setStalled(p_isApi, true);
									
									GeneralUtils.sleep(Constants.FIBONACCI[attempts] * 1000);
								}
								
								attempts++;
							}
							
							if(p_isApi) ApplicationStats.getInstance().addOsuApiRequestSent();
							else ApplicationStats.getInstance().addOsuHtmlRequestSent();
							
							setStalled(p_isApi, false);
						}
					}, (int) (request.getTimeout() - (System.currentTimeMillis() - request.getTimeSent())), true);
				}
			}
		}, delay, delay);
	}
	
	private void setStalled(boolean p_isApi, boolean p_stalled) {
		if(p_isApi) m_apiStalled = p_stalled;
		else m_htmlStalled = p_stalled;
	}
	
	public Object sendRequestSync(OsuRequest p_request, int p_timeout, boolean p_priority) {
		p_request.setTimeout(p_timeout > 0 ? p_timeout : 30000);
		
		// assign a type in advance
		if(p_request.getType() == RequestTypes.BOTH) {
			RequestTypes type = RequestTypes.API;
			
			// if we're stalled anywhere, use the other
			if(m_apiStalled && !m_htmlStalled) type = RequestTypes.HTML;
			else {
				// if html is literally empty, use it
				if(m_htmlRequests.size() == 0) type = RequestTypes.HTML;
				else {
					// at this point just use whatever's faster
					float apiMinutesDelay = m_apiRequests.size() / Constants.OSU_API_REQUESTS_PER_MINUTE;
					float htmlMinutesDelay = m_htmlRequests.size() / Constants.OSU_HTML_REQUESTS_PER_MINUTE;
					
					if(apiMinutesDelay > htmlMinutesDelay) type = RequestTypes.HTML;
				}
			}
			
			p_request.setType(type);
		}
		
		p_request.setSentTime();
		
		if(p_request.getType() == RequestTypes.API) {
			if(p_priority) m_apiRequests.addFirst(p_request);
			else m_apiRequests.add(p_request);
		} else {
			if(p_priority) m_htmlRequests.addFirst(p_request);
			else m_htmlRequests.add(p_request);
		}
		
		int timeElapsed = 0;
		
		while(p_request.getAnswer() == null) {
			if(timeElapsed >= p_request.getTimeout()) {
				if(p_request.getType() == RequestTypes.API) {
					m_apiRequests.remove(p_request);
					ApplicationStats.getInstance().addOsuApiRequestFailed();
				} else {
					m_htmlRequests.remove(p_request);
					ApplicationStats.getInstance().addOsuHtmlRequestFailed();
				}
				
				// should something be logged here or nah?
			}
			
			GeneralUtils.sleep(25);
			timeElapsed += 25;
		}
		
		return p_request.getAnswer();
	}
}