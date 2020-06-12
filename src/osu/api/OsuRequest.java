package osu.api;

/**
 * This class represents a request to be made to the osu! servers via api or html scraping.
 * 
 * @author Smc
 */
public abstract class OsuRequest {
	
	private String m_name;
	private RequestTypes m_type;
	private long m_sentTime;
	private int m_timeout;
	private Object m_answer; // this also stores the error message if it errors out
	
	public OsuRequest(String p_name, RequestTypes p_type) {
		m_name = p_name;
		m_type = p_type;
	}
	
	public String getName() {
		return m_name;
	}
	
	public RequestTypes getType() {
		return m_type;
	}
	
	public long getTimeSent() {
		return m_sentTime;
	}
	
	public int getTimeout() {
		return m_timeout;
	}
	
	public Object getAnswer() {
		return m_answer;
	}
	
	public void setType(RequestTypes p_type) {
		m_type = p_type;
	}
	
	public void setSentTime() {
		m_sentTime = System.currentTimeMillis();
	}
	
	public void setTimeout(int p_timeout) {
		m_timeout = p_timeout;
	}
	
	public void setAnswer(Object p_answer) {
		m_answer = p_answer;
	}

	public abstract void send() throws Exception;
}

enum RequestTypes {
	API, HTML, BOTH;
}