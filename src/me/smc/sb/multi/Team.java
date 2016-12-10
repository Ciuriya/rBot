package me.smc.sb.multi;

import java.util.ArrayList;
import java.util.LinkedList;

import me.smc.sb.utils.Configuration;

public class Team{

	private String teamName;
	private LinkedList<Player> players;
	private Tournament tournament;
	private int serverTeamID;
	
	public Team(Tournament t, String teamName){
		this(t, teamName, true);
	}
	
	public Team(Tournament t, String teamName, boolean append){
		this.teamName = teamName;
		this.tournament = t;
		serverTeamID = 0;
		this.players = new LinkedList<>();
		
		save(append);
		t.addTeam(this);
	}
	
	public String getTeamName(){
		return teamName;
	}
	
	public LinkedList<Player> getPlayers(){
		return players;
	}
	
	public int getServerTeamID(){
		return serverTeamID;
	}
	
	public void setServerTeamID(int serverTeamID){
		this.serverTeamID = serverTeamID;
	}
	
	public void setPlayers(LinkedList<Player> players){
		this.players = players;
	}
	
	public void save(boolean append){
		Configuration config = tournament.getConfig();
		
		if(append) config.appendToStringList("teams", teamName, true);
		
		if(players.size() != 0){
			ArrayList<String> convertedPlayers = new ArrayList<String>();
			for(Player player : players)
				convertedPlayers.add(player.getName());
			
			config.writeStringList("team-" + teamName, convertedPlayers, false);		
		}
		
		if(serverTeamID != 0) config.writeValue("team-" + teamName + "-serverID", serverTeamID);
	}
	
	public void delete(){
		Configuration config = tournament.getConfig();
		ArrayList<String> savedTeams = config.getStringList("teams");
		
		if(!savedTeams.isEmpty()){
			savedTeams.remove(teamName);
			config.writeStringList("teams", savedTeams, true);
		}
		
		config.deleteKey("team-" + teamName);
	}
	
}
