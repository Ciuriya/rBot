package me.smc.sb.deadpool;

import net.dv8tion.jda.core.entities.User;

public class DeadpoolUser{

	private User discordUser;
	private int points;
	private Vote currentVote;
	private Vote previousVote;
	
	public DeadpoolUser(User user){
		discordUser = user;
		currentVote = new Vote("");
		previousVote = new Vote("");
	}
	
	public User getUser(){
		return discordUser;
	}
	
	public String getCurrentVote(){
		return currentVote.get();
	}
	
	public String getPreviousVote(){
		return previousVote.get();
	}
	
	public boolean votedAfterFightStart(long diff){
		if(previousVote.get().length() > 0){
			long voteDiff = System.currentTimeMillis() - currentVote.getTime();
			
			return voteDiff >= diff;
		}
		
		return false;
	}
	
	public int getPoints(){
		return points;
	}
	
	public void addPoints(int amount){
		points += amount;
	}
	
	public void setVote(String vote){
		previousVote = currentVote;
		currentVote = new Vote(vote);
	}
	
	public void clearVote(){
		currentVote = new Vote("");
		previousVote = new Vote("");
	}
}

class Vote{
	
	private String vote;
	private long timestamp;
	
	public Vote(String vote){
		this.vote = vote;
		timestamp = System.currentTimeMillis();
	}
	
	public String get(){
		return vote;
	}
	
	public long getTime(){
		return timestamp;
	}
}
