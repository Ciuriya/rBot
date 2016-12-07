package me.smc.sb.irccommands;

import java.sql.Connection;
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
import me.smc.sb.utils.FinalInt;
import me.smc.sb.utils.Log;
import me.smc.sb.utils.RemotePatyServerUtils;
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
		
		try{
			int tourneyId = RemotePatyServerUtils.fetchTournamentId(t.getName());
			if(tourneyId == 0) return "This tournament doesn't exist!";
			
			Connection teamSignupCheckSQL = RemotePatyServerUtils.connect();
			
			final FinalInt teamValid = new FinalInt(1);
			
			new JdbcSession(teamSignupCheckSQL)
			.sql("SELECT teamname FROM teams " +
				 "WHERE tournaments_id=? AND id=?")
			.set(tourneyId)
			.set(teamID)
			.select(new Outcome<List<String>>(){
				@Override public List<String> handle(ResultSet rset, Statement stmt) throws SQLException{
					if(!rset.next()) teamValid.set(0);
					
					return new ArrayList<String>();
				}
			});
			
			if(teamValid.get() == 0) return "This team is not signed up for " + tournamentName + "!";
			
			Connection upSQL = RemotePatyServerUtils.connect();
			
			int rowsChanged = new JdbcSession(upSQL)
			.sql("UPDATE teammembers SET confirmed=1 " +
				 "WHERE `player_data.user_id`=? AND " +
				 "`teams.id`=?")
			.set(userID)
			.set(teamID)
			.update(Outcome.UPDATE_COUNT);
			
			if(rowsChanged == 0) return "You are not on that team!";
			
			upSQL.close();
			
			Connection delSQL = RemotePatyServerUtils.connect();
			
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