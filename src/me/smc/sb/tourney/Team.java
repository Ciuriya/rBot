package me.smc.sb.tourney;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import me.smc.sb.utils.Configuration;

public class Team{

	private String teamName;
	private LinkedList<Player> players;
	private Tournament tournament;
	private int serverTeamID;
	public static List<Team> teams = new ArrayList<>();
	
	public Team(Tournament t, String teamName){
		this(t, teamName, true);
	}
	
	public Team(Tournament t, String teamName, boolean append){
		this.teamName = teamName;
		this.tournament = t;
		serverTeamID = 0;
		this.players = new LinkedList<>();
		
		save(append);
		
		teams.add(this);
	}
	
	public String getTeamName(){
		return teamName;
	}
	
	public LinkedList<Player> getPlayers(){
		return players;
	}
	
	public Tournament getTournament(){
		return tournament;
	}
	
	public int getServerTeamID(){
		return serverTeamID;
	}
	
	public boolean has(Player player){
		return players.contains(player);
	}
	
	public boolean has(String playerName){
		return players.stream().anyMatch(p -> p.getName().replaceAll(" ", "_").equalsIgnoreCase(playerName.replaceAll(" ", "_")));
	}
	
	public void setServerTeamID(int serverTeamID){
		this.serverTeamID = serverTeamID;
	}
	
	public void setPlayers(LinkedList<Player> players){
		this.players = players;
	}
	
	public static Team getTeam(Tournament t, String teamName){
		if(!teams.isEmpty())
			for(Team team : teams)
				if(team.getTeamName().equalsIgnoreCase(teamName) && team.getTournament().get("name").equalsIgnoreCase(t.get("name")))
					return team;
		
		return null;
	}
	
	public static Team getTeam(Tournament t, int serverID){
		if(!teams.isEmpty())
			for(Team team : teams)
				if(team.getServerTeamID() == serverID && team.getTournament().get("name").equalsIgnoreCase(t.get("name")))
					return team;
		
		return null;
	}
	
	public static List<Team> getTeams(Tournament tournament){
		return teams.stream().filter(t -> t.tournament.get("name").equalsIgnoreCase(tournament.get("name"))).collect(Collectors.toList());
	}
	
	public static void removeTeam(Tournament t, String teamName){
		Team team = getTeam(t, teamName);
		
		teams.remove(team);
		team.delete();
	}
	
	public void save(boolean append){
		Configuration config = tournament.getConfig();
		
		if(append) config.appendToStringList("conf-teams", "" + teamName, true);
		
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
		
		config.removeFromStringList("conf-teams", "" + teamName, true);
		config.deleteKey("team-" + teamName);
		config.deleteKey("team-" + teamName + "-serverID");
	}
	
	public static void loadTeams(Tournament t){
		ArrayList<String> teams = t.getStringList("teams");
		
		if(!teams.isEmpty()){
			Configuration config = t.getConfig();
			
			for(String teamName : teams){
				Team team = new Team(t, teamName, false);
				
				List<String> players = config.getStringList("team-" + teamName);
				
				if(players.size() != 0){
					LinkedList<Player> lPlayers = new LinkedList<>();
					
					for(String name : players)
						lPlayers.add(new Player(name));
					
					team.setPlayers(lPlayers);
				}
				
				if(config.getInt("team-" + teamName + "-serverID") != 0)
					team.setServerTeamID(config.getInt("team-" + teamName + "-serverID"));
			}
		}
	}
}
