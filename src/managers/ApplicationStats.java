package managers;

/**
 * This class collects stats for the current application.
 * 
 * @author Smc
 */
public class ApplicationStats {

	private static ApplicationStats instance;
	private long m_bootTime;
	private long m_timerStart;
	private int m_serverCount;
	private int m_messagesReceived;
	private int m_messagesSent;
	private int m_commandsUsed;
	
	public static ApplicationStats getInstance() {
		if(instance == null) instance = new ApplicationStats();
		
		return instance;
	}
	
	public ApplicationStats() {
		m_bootTime = System.currentTimeMillis();
	}
	
	public long getUptime() {
		return System.currentTimeMillis() - m_bootTime;
	}
	
	public int getServerCount() {
		return m_serverCount;
	}
	
	public int getMessagesReceived() {
		return m_messagesReceived;
	}
	
	public int getMessagesSent() {
		return m_messagesSent;
	}
	
	public int getCommandsUsed() {
		return m_commandsUsed;
	}
	
	public void addServerCount(int p_servers) {
		m_serverCount += p_servers;
	}
	
	public void addMessageReceived() {
		m_messagesReceived++;
	}
	
	public void addMessageSent() {
		m_messagesSent++;
	}
	
	public void addCommandUsed() {
		m_commandsUsed++;
	}
	
	// a timer to count the time something takes in code
	// simply start when needed and stop when needed
	// start will tell you if it started and stop will give you the time it took
	public boolean startTimer() {
		if(m_timerStart > 0) return false;
		
		m_timerStart = System.currentTimeMillis();
		
		return true;
	}
	
	public long stopTimer() {
		if(m_timerStart == 0) return 0;
		
		long time = System.currentTimeMillis() - m_timerStart;
		
		m_timerStart = 0;
		
		return time;
	}
}
