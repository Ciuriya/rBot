package me.smc.sb.tracking;

import me.smc.sb.main.Main;

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
	
	public void setRequestType(RequestTypes type){
		this.type = type;
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
	
	protected void setDone(boolean api){
		done = true;
		
		if(api) Main.requestsSent++;
		else Main.requestHtmlSent++;
	}
}
