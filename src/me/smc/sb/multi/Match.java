package me.smc.sb.multi;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import me.smc.sb.utils.Configuration;
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
	
	public Match(Tournament t, int players){
		this(t, t.incrementMatchCount(), players, true);
	}
	
	public Match(Tournament t, int matchNum, int players, boolean append){
		this.players = players;
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
		streamPriority = 1;
		
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
	
	public Tournament getTournament(){
		return tournament;
	}
	
	public int getBestOf(){
		return bestOf;
	}
	
	public String getLobbyName(){
		if(fTeam == null || sTeam == null) return "";
		return tournament.getDisplayName() + ": (" + fTeam.getTeamName() + ") vs (" + sTeam.getTeamName() + ")";
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
		
		if(tournament.getMatchAdmins().size() > 0)
			for(String matchAdmin : tournament.getMatchAdmins()){
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
		
		Tournament.matchesRunning++;
		Game.createGame(this);
	}
	
	public void resize(int players){
		this.players = players;
		
		if(game != null && tournament.getTournamentType() == 0) 
			((TeamGame) game).resize(null);
	}
	
	public void setGame(Game game){
		this.game = game;
		
		if(game == null)
			Tournament.matchesRunning--;
	}
	
	public void setTeams(Team fTeam, Team sTeam){
		this.fTeam = fTeam;
		this.sTeam = sTeam;
	}
	
	public void setTime(long time){
		if(Tournament.getMatchDates().contains(time)){
			setTime(time + 10000);
			return;
		}
		
		scheduledDate = time;
		
		if(scheduledDate < Utils.getCurrentTimeUTC() && scheduledDate != 0){
			tournament.removeMatch(matchNum);
			return;
		}
		
		if(scheduledDate == 0) return;
		
		Tournament.addMatchDate(time);
		
		if(scheduledTime != null) scheduledTime.cancel();
		
		scheduledTime = new Timer();
		scheduledTime.schedule(new TimerTask(){
			public void run(){
				Game.createGame(Match.this);
			}
		}, scheduledDate - Utils.getCurrentTimeUTC());
	}
	
	public void setBestOf(int bestOf){
		this.bestOf = bestOf;
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
	
	public void save(boolean append){
		Configuration config = tournament.getConfig();
		
		if(append) config.appendToStringList("matches", String.valueOf(matchNum), true);
		config.writeValue("match-" + matchNum + "-players", players);
		
		config.writeValue("match-" + matchNum + "-priority", streamPriority);
		
		if(fTeam != null) config.writeValue("match-" + matchNum + "-team1", fTeam.getTeamName());
		if(sTeam != null) config.writeValue("match-" + matchNum + "-team2", sTeam.getTeamName());
		
		if(scheduledDate != 0) config.writeValue("match-" + matchNum + "-date", scheduledDate);
		
		if(pool != null) config.writeValue("match-" + matchNum + "-pool", pool.getPoolNum());
		
		if(serverID != null && serverID.length() > 0) config.writeValue("match-" + matchNum + "-serverid", serverID);
		
		if(bestOf != 5) config.writeValue("match-" + matchNum + "-bestof", bestOf);
		if(matchAdmins.size() > 0){
			config.writeStringList("match-" + matchNum + "-admins", matchAdmins, true);
		}
	}
	
}
