package me.smc.sb.multi;

import java.util.ArrayList;

import me.smc.sb.utils.Configuration;

public class Match{ //resize?

	private int players, matchNum;
	private Team fTeam, sTeam;
	private Tournament tournament;
	private long scheduledDate;
	private MapPool pool;
	
	public Match(Tournament t, int players){
		this(t, t.incrementMatchCount(), players, true);
	}
	
	public Match(Tournament t, int matchNum, int players, boolean append){
		this.players = players;
		this.matchNum = matchNum;
		this.tournament = t;
		this.scheduledDate = 0;
		this.fTeam = null;
		this.sTeam = null;
		this.pool = null;
		
		save(append);
		t.addMatch(this);
	}
	
	public int getPlayers(){
		return players;
	}
	
	public int getMatchNum(){
		return matchNum;
	}
	
	public Team getFirstTeam(){
		return fTeam;
	}
	
	public Team getSecondTeam(){
		return sTeam;
	}
	
	public MapPool getMapPool(){
		return pool;
	}
	
	public void setTeams(Team fTeam, Team sTeam){
		this.fTeam = fTeam;
		this.sTeam = sTeam;
	}
	
	public void setTime(long time){
		scheduledDate = time;
	}
	
	public void setMapPool(MapPool pool){
		this.pool = pool;
	}
	
	public void delete(){
		Configuration config = tournament.getConfig();
		ArrayList<String> savedMatches = config.getStringList("matches");
		
		if(!savedMatches.isEmpty()){
			savedMatches.remove(String.valueOf(matchNum));
			config.writeStringList("matches", savedMatches, true);
		}
		
		config.deleteKey("match-" + matchNum + "-players");
		config.deleteKey("match-" + matchNum + "-team1");
		config.deleteKey("match-" + matchNum + "-team2");
		config.deleteKey("match-" + matchNum + "-date");
		config.deleteKey("match-" + matchNum + "-pool");
	}
	
	public void save(boolean append){
		Configuration config = tournament.getConfig();
		
		if(append) config.appendToStringList("matches", String.valueOf(matchNum), true);
		config.writeValue("match-" + matchNum + "-players", players);
		
		if(fTeam != null && sTeam != null){
			config.writeValue("match-" + matchNum + "-team1", fTeam.getTeamName());
			config.writeValue("match-" + matchNum + "-team2", sTeam.getTeamName());
		}
		
		config.writeValue("match-" + matchNum + "-date", scheduledDate);
		
		if(pool != null) config.writeValue("match-" + matchNum + "-pool", pool.getPoolNum());
	}
	
}
