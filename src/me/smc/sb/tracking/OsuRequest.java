package me.smc.sb.tracking;

public abstract class OsuRequest{

	private String name;
	private RequestTypes type;
	protected Object answer;
	protected boolean done;
	protected String[] specifics;
	
	public OsuRequest(String name, RequestTypes type, String...specifics){
		this.name = name;
		this.type = type;
		this.answer = null;
		this.done = false;
		this.specifics = specifics;
	}
	
	public String getName(){
		return name;
	}
	
	public RequestTypes getType(){
		return type;
	}
	
	public Object getAnswer(){
		return answer;
	}
	
	public boolean isDone(){
		return done;
	}
	
	public String[] getSpecifics(){
		return specifics;
	}
	
	public abstract void send(boolean api) throws Exception;
}
