package me.smc.sb.utils;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import com.jcabi.jdbc.JdbcSession;
import com.jcabi.jdbc.Outcome;

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
	
	public static boolean isConfirmed(int userId, int tourneyId) throws Exception{
		Connection confirmationSQL = connect();
		
		final FinalInt confirmed = new FinalInt(0);
		
		new JdbcSession(confirmationSQL)
		.sql("SELECT confirmed FROM `teammembers` tm " +
			 "JOIN `teams` t ON tm.`teams.id` = t.`id`" +
			 "WHERE tm.`player_data.user_id`=? AND " +
			 "t.`tournaments_id`=?")
		.set(userId)
		.set(tourneyId)
		.select(new Outcome<List<String>>(){
			@Override public List<String> handle(ResultSet rset, Statement stmt) throws SQLException{
				if(rset.next()) confirmed.set(rset.getBoolean(1) ? 1 : 0);
				
				return new ArrayList<String>();
			}
		});
		
		confirmationSQL.close();
		
		return confirmed.get() == 1;
	}
}
