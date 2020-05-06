package data;

import java.util.concurrent.Callable;

import managers.ApplicationStats;
import managers.ThreadingManager;
import utils.Constants;
import utils.TimeUtils;

/**
 * This enum holds all possible discord activities as runnables 
 * that fetch their values synchronously when needed.
 * 
 * @author Smc
 */
public enum DiscordActivities {
	
	UPTIME(new Callable<String>() {
		public String call() {
			return "for " + TimeUtils.toDuration(ApplicationStats.getInstance().getUptime(), false);
		}
	}),
	SERVERS(new Callable<String>() {
		public String call() {
			return "in " + ApplicationStats.getInstance().getServerCount() + " servers | " + Constants.DEFAULT_PREFIX + "help";
		}
	}),
	SUPPORT(new Callable<String>() {
		public String call() {
			return Constants.SUPPORT_SERVER_LINK;
		}
	});
	
	private Callable<String> m_activityFetchingCallable;
	
	private DiscordActivities(Callable<String> p_activityFetchingCallable) {
		m_activityFetchingCallable = p_activityFetchingCallable;
	}

	public String getActivity() {
		String activity = ThreadingManager.getInstance().executeSync(m_activityFetchingCallable, 5000);
		
		return activity == null ? "" : activity;
	}
}
