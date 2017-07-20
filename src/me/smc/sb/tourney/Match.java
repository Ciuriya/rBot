package me.smc.sb.tourney;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.stream.Collectors;

import me.smc.sb.utils.Configuration;
import me.smc.sb.utils.Log;
import me.smc.sb.utils.Utils;

public class Match{

	private int players, matchNum, bestOf, streamPriority;
	private String serverID;
	private Team fTeam, sTeam;
	private Tournament tournament;
	private long scheduledDate;
	private MapPool pool;
	private Game game;
	private ArrayList<String> matchAdmins;
	private Timer scheduledTime;
	public static List<Match> matches = new ArrayList<>();
	public static int runningMatches = 0;
	public static List<Long> matchTimes = new ArrayList<>();
	
	public Match(Tournament t){
		this(t, t.incrementMatchCount(), true);
	}
	
	public Match(Tournament t, int matchNum, boolean append){
		players = t.getInt("matchSize");
		
		this.matchNum = matchNum;
		this.tournament = t;
		this.scheduledDate = 0;
		this.bestOf = 5;
		this.serverID = "";
		this.fTeam = null;
		this.sTeam = null;
		this.pool = null;
		this.game = null;
		this.matchAdmins = new ArrayList<>();
		streamPriority = 0;
		
		save(append);
		
		matches.add(this);
	}
	
	public int getMatchSize(){
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
	
	public Tournament getTournament(){
		return tournament;
	}
	
	public int getBestOf(){
		return bestOf;
	}
	
	public String getLobbyName(){
		if(fTeam == null || sTeam == null) return "";
		
		return tournament.get("displayName") + ": (" + fTeam.getTeamName() + ") vs (" + sTeam.getTeamName() + ")";
	}
	
	public Game getGame(){
		return game;
	}
	
	public long getTime(){
		return scheduledDate;
	}
	
	public int getStreamPriority(){
		return streamPriority;
	}
	
	public String getServerID(){
		return serverID;
	}
	
	public void setPlayers(int players){
		this.players = players;
	}
	
	public void setStreamPriority(int priority){
		streamPriority = priority;
	}
	
	public void setMatchAdmins(ArrayList<String> admins){
		matchAdmins = admins;
	}
	
	public void addMatchAdmin(String admin){
		if(!matchAdmins.contains(admin))
			matchAdmins.add(admin);
	}
	
	public void removeMatchAdmin(String admin){
		if(matchAdmins.contains(admin))
			matchAdmins.remove(admin);
	}
	
	public boolean isMatchAdmin(String admin){
		if(admin == null) return true;
		
		if(matchAdmins.size() > 0)
			for(String matchAdmin : matchAdmins){
				if(matchAdmin.replaceAll(" ", "_").equalsIgnoreCase(admin))
					return true;
			}
		
		ArrayList<String> tourneyAdmins = tournament.getStringList("tournament-admins");
		
		if(tourneyAdmins.size() > 0)
			for(String matchAdmin : tourneyAdmins){
				if(matchAdmin.replaceAll(" ", "_").equalsIgnoreCase(admin))
					return true;
			}
		
		return false;
	}
	
	public ArrayList<String> getMatchAdmins(){
		return matchAdmins;
	}
	
	public void setServerID(String serverID){
		this.serverID = serverID;
	}
	
	public void start(){
		if(scheduledTime != null) scheduledTime.cancel();
		
		runningMatches++;
		game = new Game(this);
	}
	
	public void resize(int players){
		this.players = players;
		
		if(game != null && tournament.getInt("type") == 0)
			game.lobbyManager.resize();
	}
	
	public void setGame(Game game){
		this.game = game;
		
		if(game == null)
			runningMatches--;
	}
	
	public void setTeams(Team fTeam, Team sTeam){
		this.fTeam = fTeam;
		this.sTeam = sTeam;
	}
	
	public void setTime(long time){
		if(matchTimes.contains(time)){
			setTime(time + 10000);
			
			return;
		}
		
		scheduledDate = time;
		
		if(scheduledDate < Utils.getCurrentTimeUTC() && scheduledDate != 0){
			removeMatch(tournament, matchNum);
			
			return;
		}
		
		if(scheduledDate == 0) return;
		
		matchTimes.add(time);
		
		if(scheduledTime != null) scheduledTime.cancel();
		
		scheduledTime = new Timer();
		scheduledTime.schedule(new TimerTask(){
			public void run(){
				start();
			}
		}, scheduledDate - Utils.getCurrentTimeUTC());
	}
	
	public void setBestOf(int bestOf){
		this.bestOf = bestOf;
	}
	
	public void setMapPool(MapPool pool){
		this.pool = pool;
	}
	
	public static Match getMatch(Tournament t, int matchNum){
		if(!matches.isEmpty())
			for(Match match : matches)
				if(match.getMatchNum() == matchNum || match.getServerID() == String.valueOf(matchNum) && 
					match.getTournament().get("name").equalsIgnoreCase(t.get("name")))
					return match;
		
		return null;
	}
	
	public static List<Match> getMatches(Tournament t){
		return matches.stream().filter(m -> m.getTournament().get("name").equalsIgnoreCase(t.get("name"))).collect(Collectors.toList());
	}
	
	public static void removeMatch(Tournament t, int matchNum){
		Match match = getMatch(t, matchNum);
		
		if(match.getTime() != 0)
			matchTimes.remove(match.getTime());
		
		matches.remove(match);
		match.delete();
	}
	
	public void save(boolean append){
		Configuration config = tournament.getConfig();
		
		if(append) tournament.appendToStringList("matches", "" + matchNum);
		
		config.writeValue("match-" + matchNum + "-players", players);
		
		if(streamPriority != 0) config.writeValue("match-" + matchNum + "-priority", streamPriority);
		if(fTeam != null) config.writeValue("match-" + matchNum + "-team1", fTeam.getTeamName());
		if(sTeam != null) config.writeValue("match-" + matchNum + "-team2", sTeam.getTeamName());
		if(scheduledDate != 0) config.writeValue("match-" + matchNum + "-date", scheduledDate);
		if(pool != null) config.writeValue("match-" + matchNum + "-pool", pool.getPoolNum());
		if(serverID != null && serverID.length() > 0) config.writeValue("match-" + matchNum + "-serverid", serverID);
		if(bestOf != 5) config.writeValue("match-" + matchNum + "-bestof", bestOf);
		if(matchAdmins.size() > 0) config.writeStringList("match-" + matchNum + "-admins", matchAdmins, true);
	}
	
	public void delete(){
		Configuration config = tournament.getConfig();

		tournament.removeFromStringList("matches", "" + matchNum);
		
		config.deleteKey("match-" + matchNum + "-players");
		config.deleteKey("match-" + matchNum + "-priority");
		config.deleteKey("match-" + matchNum + "-team1");
		config.deleteKey("match-" + matchNum + "-team2");
		config.deleteKey("match-" + matchNum + "-date");
		config.deleteKey("match-" + matchNum + "-pool");
		config.deleteKey("match-" + matchNum + "-bestof");
		config.deleteKey("match-" + matchNum + "-admins");
		config.deleteKey("match-" + matchNum + "-serverid");
		
		this.fTeam = null;
		this.sTeam = null;
		this.pool = null;
		
		matchAdmins.clear();
		
		if(scheduledTime != null) scheduledTime.cancel();
	}
	
	public static void loadMatches(Tournament t){
		ArrayList<String> matches = t.getStringList("matches");
		
		if(!matches.isEmpty()){
			Configuration config = t.getConfig();
			
			for(String matchNum : matches){
				Match match = new Match(t, Utils.stringToInt(matchNum), false);
				
				if(config.getInt("match-" + matchNum + "-players") != t.getInt("matchSize"))
					match.setPlayers(config.getInt("match-" + matchNum + "-players"));
				
				if(config.getValue("match-" + matchNum + "-team1") != "" || config.getValue("match-" + matchNum + "-team2") != ""){
					Team team1 = Team.getTeam(t, config.getValue("match-" + matchNum + "-team1"));
					Team team2 = Team.getTeam(t, config.getValue("match-" + matchNum + "-team2"));
					
					if(team1 == null || team2 == null){
						if(team1 == null && isMatchConditional(config.getValue("match-" + matchNum + "-team1"))){
							t.conditionalTeams.put(config.getValue("match-" + matchNum + "-team1") + " 1", match);
							Log.logger.log(Level.INFO, "Match #" + matchNum + " loaded conditional " + config.getValue("match-" + matchNum + "-team1") + " 1");
						}
						
						if(team2 == null && isMatchConditional(config.getValue("match-" + matchNum + "-team2"))){
							t.conditionalTeams.put(config.getValue("match-" + matchNum + "-team2") + " 2", match);
							Log.logger.log(Level.INFO, "Match #" + matchNum + " loaded conditional " + config.getValue("match-" + matchNum + "-team2") + " 2");
						}
					}
					
					match.setTeams(team1, team2);
				}
				
				if(config.getInt("match-" + matchNum + "-pool") != 0)
					match.setMapPool(MapPool.getPool(t, config.getInt("match-" + matchNum + "-pool")));
				
				if(config.getInt("match-" + matchNum + "-bestof") != 0)
					match.setBestOf(config.getInt("match-" + matchNum + "-bestof"));
				
				if(config.getLong("match-" + matchNum + "-date") != 0)
					match.setTime(config.getLong("match-" + matchNum + "-date"));
				
				if(!config.getStringList("match-" + matchNum + "-admins").isEmpty())
					match.setMatchAdmins(config.getStringList("match-" + matchNum + "-admins"));
				
				if(config.getValue("match-" + matchNum + "-serverid") != "")
					match.setServerID(config.getValue("match-" + matchNum + "-serverid"));
				
				if(config.getInt("match-" + matchNum + "-priority") == 0)
					match.setStreamPriority(1);
				else match.setStreamPriority(config.getInt("match-" + matchNum + "-priority"));
			}
		}
	}
	
	public static boolean isMatchConditional(String conditionalStr){
		if(conditionalStr.split(" ")[0].equalsIgnoreCase("loser") || conditionalStr.split(" ")[0].equalsIgnoreCase("winner")){
			if(Utils.stringToInt(conditionalStr.split(" ")[1]) != -1 && conditionalStr.split(" ").length == 2)
				return true;
		}
		
		return false;
	}
}
