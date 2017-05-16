package me.smc.sb.polls;

import java.util.ArrayList;

public class Option{

	private String name;
	private ArrayList<String> votes;
	
	public Option(String name){
		this.name = name;
		votes = new ArrayList<>();
	}
	
	public Option(String name, ArrayList<String> votes){
		this.name = name;
		this.votes = votes;
	}
	
	public String getName(){
		return name;
	}
	
	public ArrayList<String> getVotes(){
		return votes;
	}

	public boolean addVote(String userId){
		if(!votes.contains(userId)) 
			return votes.add(userId);
		
		return false;
	}
	
	public boolean hasVoted(String userId){
		return votes.contains(userId);
	}
	
	public boolean removeVote(String userId){
		if(votes.contains(userId))
			return votes.remove(userId);
		
		return false;
	}
}
