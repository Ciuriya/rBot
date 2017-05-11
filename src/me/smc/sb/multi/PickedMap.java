package me.smc.sb.multi;

public class PickedMap{

	private Map map;
	private Team team;
	private Match match;
	private boolean warmup;
	
	public PickedMap(Map map, Team team, Match match, boolean warmup){
		this.map = map;
		this.team = team;
		this.match = match;
		this.warmup = warmup;
	}
	
	public Map getMap(){
		return map;
	}
	
	public Team getTeam(){
		return team;
	}
	
	public Match getMatch(){
		return match;
	}
	
	public boolean isWarmup(){
		return warmup;
	}
}
