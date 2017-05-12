package me.smc.sb.polls;

import java.util.ArrayList;
import java.util.List;

public class Option{

	private String name;
	private List<String> votes;
	
	public Option(String name){
		this.name = name;
		votes = new ArrayList<>();
	}
	
	public String getName(){
		return name;
	}
	
	public List<String> getVotes(){
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
