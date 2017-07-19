package me.smc.sb.tourney;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class PlayingTeam{
	
	private Match match;
	private Team team;
	private int points;
	private int rematchesLeft;
	private List<Player> currentPlayers;
	private List<Map> bans;
	private LinkedList<PickedMap> mapsPicked;

	public PlayingTeam(Team team, Match match){
		this.match = match;
		this.team = team;
		
		this.currentPlayers = new ArrayList<>();
		this.bans = new ArrayList<>();
		this.mapsPicked = new LinkedList<>();
	}
	
	public Match getMatch(){
		return match;
	}
	
	public Team getTeam(){
		return team;
	}
	
	public int getPoints(){
		return points;
	}
	
	public boolean canRematch(){
		return rematchesLeft != 0;
	}
	
	public List<Player> getCurrentPlayers(){
		return currentPlayers;
	}
	
	public List<Map> getBans(){
		return bans;
	}
	
	public LinkedList<PickedMap> getPicks(){
		return mapsPicked;
	}
	
	public boolean useRematch(){
		boolean rematch = canRematch();
		
		if(rematch) rematchesLeft--;
		
		return rematch;
	}
	
	public boolean addPlayer(Player player){
		if(currentPlayers.size() < match.getMatchSize() / 2 && team.has(player) &&
			!currentPlayers.contains(player)){
			return currentPlayers.add(player);
		}
		
		return false;
	}
	
	public boolean removePlayer(Player player){
		if(currentPlayers.contains(player))
			return currentPlayers.remove(player);
		
		return false;
	}
}
