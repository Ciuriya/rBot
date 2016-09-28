package me.smc.sb.irccommands;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import org.pircbotx.PircBotX;
import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.PrivateMessageEvent;

import com.jcabi.jdbc.JdbcSession;
import com.jcabi.jdbc.Outcome;

import me.smc.sb.multi.Tournament;
import me.smc.sb.utils.Configuration;
import me.smc.sb.utils.FinalInt;
import me.smc.sb.utils.Log;
import me.smc.sb.utils.Utils;

public class ConfirmTeamCommand extends IRCCommand{
	
	public ConfirmTeamCommand(){
		super("Confirms a player's inscription to a team.",
			  "<tournament name> <team id> ",
			  null,
			  "confirm");
	}
	
	@Override
	public String onCommand(MessageEvent<PircBotX> e, PrivateMessageEvent<PircBotX> pe, String discord, String[] args){
		String argCheck = Utils.checkArguments(args, 2);
		if(argCheck.length() > 0) return argCheck;
		
		String tournamentName = "";
		
		for(int i = 0; i < args.length - 1; i++) tournamentName += args[i] + " ";
		Tournament t = Tournament.getTournament(tournamentName.substring(0, tournamentName.length() - 1));
		
		if(t == null) return "Invalid tournament!";
		
		String user = Utils.toUser(e, pe);
		if(user == null) return "Invalid user!";
		
		int userID = Utils.stringToInt(Utils.getOsuPlayerId(user));
		if(userID == -1) return "Could not fetch user ID, please message a tournament administrator!";
		
		int teamID = Utils.stringToInt(args[args.length - 1]);
		if(teamID == -1) return "Invalid team ID!";
		
		Configuration login = new Configuration(new File("login.txt"));
		
		try{
			Class.forName("com.mysql.jdbc.Driver");
			Connection tourneyCheckSQL = DriverManager.getConnection(login.getValue("WTURL"), 
                    					 							 login.getValue("WTUser"),
                    					 							 login.getValue("WTPass"));
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
			
			if(tourneyId.get() == 0) return "This tournament doesn't exist!";
			tourneyCheckSQL.close();
			
			Connection teamSignupCheckSQL = DriverManager.getConnection(login.getValue("WTURL"), 
					 													login.getValue("WTUser"),
					 													login.getValue("WTPass"));
			
			final FinalInt teamValid = new FinalInt(1);
			
			new JdbcSession(teamSignupCheckSQL)
			.sql("SELECT teamname FROM teams " +
				 "WHERE tournaments_id=? AND id=?")
			.set(tourneyId.get())
			.set(teamID)
			.select(new Outcome<List<String>>(){
				@Override public List<String> handle(ResultSet rset, Statement stmt) throws SQLException{
					if(!rset.next()) teamValid.set(0);
					
					return new ArrayList<String>();
				}
			});
			
			if(teamValid.get() == 0) return "This team is not signed up for " + tournamentName + "!";
			
			Connection upSQL = DriverManager.getConnection(login.getValue("WTURL"), 
                                                           login.getValue("WTUser"),
                                                           login.getValue("WTPass"));
			
			int rowsChanged = new JdbcSession(upSQL)
			.sql("UPDATE teammembers SET confirmed=1 " +
				 "WHERE `player_data.user_id`=? AND " +
				 "`teams.id`=?")
			.set(userID)
			.set(teamID)
			.update(Outcome.UPDATE_COUNT);
			
			if(rowsChanged == 0) return "You are not on that team!";
			
			upSQL.close();
			
			Connection delSQL = DriverManager.getConnection(login.getValue("WTURL"), 
                                                            login.getValue("WTUser"),
                                                            login.getValue("WTPass"));
			
			new JdbcSession(delSQL)
			.sql("DELETE FROM teammembers " +
				 "WHERE `player_data.user_id`=? AND " +
				 "confirmed=0")
			.set(userID)
			.update(Outcome.VOID);
			
			delSQL.close();
		}catch(Exception ex){
			Log.logger.log(Level.SEVERE, ex.getMessage(), ex);
			return "Could not connect to database!";
		}
		
		Utils.info(e, pe, discord, "Your participation is now confirmed! Thanks for registering!");
		
		return "";
	}
	
}