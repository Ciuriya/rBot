package me.smc.sb.deadpool;

import net.dv8tion.jda.core.entities.User;

public class DeadpoolUser{

	private User discordUser;
	private int points;
	private String vote;
	
	public DeadpoolUser(User user){
		discordUser = user;
		vote = "";
	}
	
	public String getVote(){
		return vote;
	}
	
	public User getUser(){
		return discordUser;
	}
	
	public int getPoints(){
		return points;
	}
	
	public void addPoints(int amount){
		points += amount;
	}
	
	public void setVote(String vote){
		this.vote = vote;
	}
	
	public void clearVote(){
		setVote("");
	}
}
