package me.smc.sb.deadpool;

import net.dv8tion.jda.core.entities.User;

public class DeadpoolUser{

	private User discordUser;
	private int points;
	private String currentVote;
	
	public DeadpoolUser(User user){
		discordUser = user;
	}
	
	public User getUser(){
		return discordUser;
	}
	
	public String getCurrentVote(){
		return currentVote;
	}
	
	public int getPoints(){
		return points;
	}
	
	public void addPoints(int amount){
		points += amount;
	}
	
	public void setVote(String vote){
		currentVote = vote;
	}
	
	public void clearVote(){
		currentVote = "";
	}
}
