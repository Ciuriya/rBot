package me.smc.sb.utils;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import com.jcabi.jdbc.JdbcSession;
import com.jcabi.jdbc.Outcome;

import me.smc.sb.multi.Match;
import me.smc.sb.multi.Player;
import me.smc.sb.multi.Team;
import me.smc.sb.multi.Tournament;

public class RemotePatyServerUtils{

	public static Connection connect() throws Exception{
		Configuration login = new Configuration(new File("login.txt"));
		
		Class.forName("com.mysql.jdbc.Driver");
		return DriverManager.getConnection(login.getValue("WTURL"), 
		        					 	   login.getValue("WTUser"),
		        					 	   login.getValue("WTPass"));
	}
	
	public static int fetchTournamentId(String tournamentName) throws Exception{
		Connection tourneyCheckSQL = connect();
		final FinalInt tourneyId = new FinalInt(0);
		
		new JdbcSession(tourneyCheckSQL)
		.sql("SELECT id FROM tournaments " + 
			 "WHERE tourney_name=?")
		.set(tournamentName)
		.select(new Outcome<List<String>>(){
			@Override public List<String> handle(ResultSet rset, Statement stmt) throws SQLException{
				if(rset.next()) tourneyId.set(rset.getInt(1));
				
				return new ArrayList<String>();
			}
		});
		
		int id = tourneyId.get();
		
		tourneyCheckSQL.close();
		
		return id;
	}
	
	public static boolean isConfirmed(String userName, int tourneyId) throws Exception{
		Connection confirmationSQL = connect();
		
		final FinalInt confirmed = new FinalInt(0);
		
		new JdbcSession(confirmationSQL)
		.sql("SELECT tm.`confirmed` FROM `teammembers` tm " +
			 "JOIN `teams` t ON tm.`teams.id` = t.`id` " +
			 "JOIN `player_data` pd ON tm.`player_data.user_id` = pd.`user_id` " +
			 "WHERE t.`tournaments_id`=? AND username=?")
		.set(tourneyId)
		.set(userName)
		.select(new Outcome<List<String>>(){
			@Override public List<String> handle(ResultSet rset, Statement stmt) throws SQLException{
				if(rset.next()) confirmed.set(rset.getBoolean(1) ? 1 : 0);
				
				return new ArrayList<String>();
			}
		});
		
		confirmationSQL.close();
		
		return confirmed.get() == 1;
	}
	
	public static long fetchMapValue(int mapId, int tournamentId, String value) throws Exception{
		Connection fetchSQL = connect();
		
		final FinalLong finalValue = new FinalLong(0);
		
		new JdbcSession(fetchSQL)
		.sql("SELECT " + value + " FROM maps " + 
			 "WHERE map_id=? AND mappool_tournaments_id=?")
		.set(mapId)
		.set(tournamentId)
		.select(new Outcome<List<String>>(){
			@Override public List<String> handle(ResultSet rset, Statement stmt) throws SQLException{
				if(rset.next()) finalValue.set(rset.getLong(1));
				
				return new ArrayList<String>();
			}
		});
		
		fetchSQL.close();
		
		return finalValue.get();
	}
	
	public static void incrementMapValue(int mapId, int tournamentId, String value, long amount){
		if(amount == 0) return;
		
		new Thread(new Runnable(){
			public void run(){
				Connection mapSQL = null;
				
				try{
					long remoteVal = fetchMapValue(mapId, tournamentId, value);
					
					mapSQL = connect();
					
					new JdbcSession(mapSQL)
					.sql("UPDATE `maps` SET " + value + "=? " +
						 "WHERE map_id=? AND mappool_tournaments_id=?")
					.set(remoteVal + amount)
					.set(mapId)
					.set(tournamentId)
					.update(Outcome.VOID);
				}catch(Exception e){
					Log.logger.log(Level.SEVERE, "Could not increment value " + value + " by " + amount + " for map #" + mapId, e);
				}
				
				if(mapSQL != null){
					try{
						mapSQL.close();
					}catch(SQLException e){
						Log.logger.log(Level.SEVERE, "Could not close connection to remote database!", e);
					}
				}
			}
		}).start();
	}
	
	public static void setMPLinkAndWinner(String mpLink, Team winner, String matchID, String tournamentName, int fTeamScore, int sTeamScore){
		Connection mpSQL = null;
		
		try{
			int tourneyId = fetchTournamentId(tournamentName);
			
			mpSQL = connect();
			
			new JdbcSession(mpSQL)
			.sql("UPDATE `schedule` SET mp_link=?, winner=?, " +
				 "team1_score=?, team2_score=? " +
				 "WHERE id=? AND tournaments_id=?")
			.set(mpLink)
			.set(winner.getServerTeamID())
			.set(fTeamScore)
			.set(sTeamScore)
			.set(matchID)
			.set(tourneyId)
			.update(Outcome.VOID);
		}catch(Exception e){
			Log.logger.log(Level.SEVERE, "Could not set mp info!", e);
		}
		
		if(mpSQL != null){
			try{
				mpSQL.close();
			}catch(SQLException e){
				Log.logger.log(Level.SEVERE, "Could not close connection to remote database!", e);
			}
		}
	}
	
	public static void syncTeams(String tournamentName){
		Connection teamSQL = null;
		
		try{
			teamSQL = connect();
			
			Tournament t = Tournament.getTournament(tournamentName);
			
			new JdbcSession(teamSQL)
			.sql("SELECT te.`id`, teamname FROM `teams` te " +
				 "JOIN `tournaments` t ON t.`id` = te.`tournaments_id` " +
				 "WHERE t.`tourney_name`=?")
			.set(tournamentName)
			.select(new Outcome<List<String>>(){
				@Override public List<String> handle(ResultSet rset, Statement stmt) throws SQLException{
					while(rset.next()){
						syncTeam(t, rset.getInt(1), rset.getString(2));
					}
					
					return new ArrayList<String>();
				}
			});
		}catch(Exception e){
			Log.logger.log(Level.SEVERE, "Could not sync teams! ex: " + e.getMessage(), e);
		}
		
		if(teamSQL != null){
			try{
				teamSQL.close();
			}catch(SQLException e){
				Log.logger.log(Level.SEVERE, "Could not close connection to remote database!", e);
			}
		}
	}
	
	private static Team syncTeam(Tournament t, int teamId, String teamName){
		Team team = new Team(t, teamName, true);
		team.setServerTeamID(teamId);
		LinkedList<Player> players = new LinkedList<>();
		
		Connection playersSQL = null;
		
		try{
			playersSQL = connect();
			
			new JdbcSession(playersSQL)
			.sql("SELECT username FROM `teammembers` tm " +
				 "JOIN `player_data` pd ON tm.`player_data.user_id` = pd.`user_id` " +
				 "WHERE tm.`teams.id`=? " +
				 "ORDER BY is_cpt DESC, confirmed DESC, username ASC")
			.set(teamId)
			.select(new Outcome<List<String>>(){
				@Override public List<String> handle(ResultSet rset, Statement stmt) throws SQLException{
					while(rset.next()){
						players.add(new Player(rset.getString(1)));
					}
					
					return new ArrayList<String>();
				}
			});
		}catch(Exception e){
			Log.logger.log(Level.SEVERE, "Could not sync teams! ex: " + e.getMessage(), e);
		}
		
		if(playersSQL != null){
			try{
				playersSQL.close();
			}catch(SQLException e){
				Log.logger.log(Level.SEVERE, "Could not close connection to remote database!", e);
			}
		}
		
		if(players.size() > 0){
			team.setPlayers(players);
			team.save(false);
		}else t.removeTeam(teamName);
		
		return team;
	}
	
	public static void syncMatches(String tournamentName){
		Connection matchesSQL = null;
		
		try{
			matchesSQL = connect();
			
			Tournament t = Tournament.getTournament(tournamentName);
			
			new JdbcSession(matchesSQL)
			.sql("SELECT id, team1, team2, mappool, `date` FROM schedule " +
				 "WHERE tournaments_id=?")
			.set(fetchTournamentId(tournamentName))
			.select(new Outcome<List<String>>(){
				@Override public List<String> handle(ResultSet rset, Statement stmt) throws SQLException{
					while(rset.next()){
						Match match = new Match(t, 8);
						match.setServerID(rset.getString(1));
						match.setTeams(t.getTeam(rset.getInt(2)), t.getTeam(rset.getInt(3)));
						match.setMapPool(t.getPool(Utils.stringToInt(rset.getString(4))));
						
						try{
							Timestamp ts = rset.getTimestamp(5);
							
							@SuppressWarnings("deprecation")
							int timezoneOffset = ts.getTimezoneOffset();
							long time = ts.getTime() - TimeUnit.MINUTES.toMillis(timezoneOffset);
							
							match.setTime(time);
						}catch(Exception e){
							Log.logger.log(Level.SEVERE, "Could not sync time! ex: " + e.getMessage(), e);
						}

						match.save(false);
					}
					
					return new ArrayList<String>();
				}
			});
		}catch(Exception e){
			Log.logger.log(Level.SEVERE, "Could not sync matches! ex: " + e.getMessage(), e);
		}
		
		if(matchesSQL != null){
			try{
				matchesSQL.close();
			}catch(SQLException e){
				Log.logger.log(Level.SEVERE, "Could not close connection to remote database!", e);
			}
		}
	}
}
