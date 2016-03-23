package me.smc.sb.multi;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import me.smc.sb.utils.Configuration;
import me.smc.sb.utils.Utils;

public class Tournament{

	//MAKE SURE YOU USE UTC
	
	private String name;
	public static List<Tournament> tournaments;
	private List<Team> teams;
	private List<Match> matches;
	private List<MapPool> pools;
	private List<Long> matchDates;
	private boolean scoreV2;
	private int pickWaitTime;
	
	public Tournament(String name){
		this(name, true);
	}
	
	public Tournament(String name, boolean append){
		this.name = name;
		teams = new ArrayList<>();
		matches = new ArrayList<>();
		pools = new ArrayList<>();
		matchDates = new ArrayList<>();
		
		scoreV2 = getConfig().getBoolean("scoreV2");
		pickWaitTime = getConfig().getInt("pickWaitTime");
		
		save(append);
		tournaments.add(this);
	}
	
	public String getName(){
		return name;
	}
	
	public boolean isScoreV2(){
		return scoreV2;
	}
	
	public void setScoreV2(boolean scoreV2){
		this.scoreV2 = scoreV2;
	}
	
	public Configuration getConfig(){
		return new Configuration(new File("tournament-" + name + ".txt"));
	}
	
	public List<Team> getTeams(){
		return teams;
	}
	
	public List<Match> getMatches(){
		return matches;
	}
	
	public List<MapPool> getMapPools(){
		return pools;
	}
	
	public List<Long> getMatchDates(){
		return matchDates;
	}
	
	public void addPool(MapPool pool){
		pools.add(pool);
	}
	
	public void addTeam(Team team){
		teams.add(team);
	}
	
	public void addMatch(Match match){
		matches.add(match);
	}
	
	public void addMatchDate(long date){
		matchDates.add(date);
	}
	
	public Match getMatch(int matchNum){
		if(!matches.isEmpty())
			for(Match match : matches)
				if(match.getMatchNum() == matchNum)
					return match;
		return null;
	}
	
	public Team getTeam(String teamName){
		if(!teams.isEmpty())
			for(Team team : teams)
				if(team.getTeamName().equalsIgnoreCase(teamName))
					return team;
		return null;
	}
	
	public MapPool getPool(int poolNum){
		if(!pools.isEmpty())
			for(MapPool pool : pools)
				if(pool.getPoolNum() == poolNum)
					return pool;
		return null;
	}
	
	public boolean removeMatch(int matchNum){
		if(!matches.isEmpty())
			for(Match match : new ArrayList<Match>(matches))
				if(match.getMatchNum() == matchNum){
					matches.remove(match);
					
					if(match.getTime() != 0)
						matchDates.remove(match.getTime());
					
					match.delete();
					return true;
				}
		return false;
	}
	
	public boolean removeTeam(String teamName){
		if(!teams.isEmpty())
			for(Team team : new ArrayList<Team>(teams))
				if(team.getTeamName().equalsIgnoreCase(teamName)){
					teams.remove(team);
					team.delete();
					return true;
				}
		return false;
	}
	
	public boolean removePool(int poolNum){
		if(!pools.isEmpty())
			for(MapPool pool : new ArrayList<MapPool>(pools))
				if(pool.getPoolNum() == poolNum){
					pools.remove(pool);
					pool.delete();
					return true;
				}
		return false;
	}
	
	public void save(boolean append){
		if(append) new Configuration(new File("tournaments.txt")).appendToStringList("tournaments", name, true);
		
		getConfig().writeValue("scoreV2", scoreV2);
		getConfig().writeValue("pickWaitTime", pickWaitTime);
	}
	
	public void delete(){
		Configuration config = new Configuration(new File("tournaments.txt"));
		ArrayList<String> savedTournaments = config.getStringList("tournaments");
		
		if(!savedTournaments.isEmpty()){
			savedTournaments.remove(name);
			config.writeStringList("tournaments", savedTournaments, true);
		}
		
		getConfig().delete();
	}
	
	public static Tournament getTournament(String name){
		if(!tournaments.isEmpty())
			for(Tournament t : tournaments)
				if(t.getName().equalsIgnoreCase(name))
					return t;
		return null;
	}
	
	public static void loadTournaments(){
		tournaments = new ArrayList<>();
		List<String> savedTournaments = new Configuration(new File("tournaments.txt")).getStringList("tournaments");
		
		if(!savedTournaments.isEmpty())
			for(String sTournament : savedTournaments){
				Tournament tournament = new Tournament(sTournament, false);
				
				tournament.loadPools();
				tournament.loadTeams();
				tournament.loadMatches();
			}	
	}
	
	private void loadMatches(){
		Configuration config = getConfig();
		List<String> savedMatches = config.getStringList("matches");
		
		if(!savedMatches.isEmpty())
			for(String matchNum : savedMatches){
				Match match = new Match(this, Utils.stringToInt(matchNum), config.getInt("match-" + matchNum + "-players"), false);
				
				if(config.getValue("match-" + matchNum + "-team1") != "" && config.getValue("match-" + matchNum + "-team2") != "")
					match.setTeams(getTeam(config.getValue("match-" + matchNum + "-team1")), getTeam(config.getValue("match-" + matchNum + "-team2")));
				
				if(config.getInt("match-" + matchNum + "-pool") != 0)
					match.setMapPool(getPool(config.getInt("match-" + matchNum + "-pool")));
				
				if(config.getInt("match-" + matchNum + "-bestof") != 0)
					match.setBestOf(config.getInt("match-" + matchNum + "-bestof"));
				
				if(config.getLong("match-" + matchNum + "-date") != 0)
					match.setTime(config.getLong("match-" + matchNum + "-date"));
				
				if(!config.getStringList("match-" + matchNum + "-admins").isEmpty())
					match.setMatchAdmins(config.getStringList("match-" + matchNum + "-admins"));
			}
	}
	
	protected int incrementMatchCount(){
		Configuration config = getConfig();
		int lastMatch = config.getInt("last-match");
		config.writeValue("last-match", lastMatch + 1);
		return lastMatch + 1;
	}
	
	private void loadTeams(){
		Configuration config = getConfig();
		List<String> savedTeams = config.getStringList("teams");
		
		if(!savedTeams.isEmpty())
			for(String teamName : savedTeams){
				Team team = new Team(this, teamName, false);
				
				List<String> players = config.getStringList("team-" + teamName);
				if(players.size() != 0){
					LinkedList<Player> lPlayers = new LinkedList<>();
					for(String name : players)
						lPlayers.add(new Player(name));
					
					team.setPlayers(lPlayers);
				}
			}
	}
	
	private void loadPools(){
		Configuration config = getConfig();
		List<String> savedPools = config.getStringList("pools");
		
		if(!savedPools.isEmpty())
			for(String poolNum : savedPools){
				MapPool pool = new MapPool(this, Utils.stringToInt(poolNum), false);
				
				List<String> maps = config.getStringList("pool-" + poolNum + "-maps");
				if(maps.size() != 0)
					for(String map : maps)
						pool.addMap(new Map(map));
				
				if(config.getValue("pool-" + poolNum + "-sheet") != "") 
					pool.setSheetUrl(config.getValue("pool-" + poolNum + "-sheet"));
			}
	}
	
	protected int incrementPoolCount(){
		Configuration config = getConfig();
		int lastPool = config.getInt("last-pool");
		config.writeValue("last-pool", lastPool + 1);
		return lastPool + 1;
	}
	
}
