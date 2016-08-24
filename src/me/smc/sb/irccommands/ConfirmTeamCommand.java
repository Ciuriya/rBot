package me.smc.sb.irccommands;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.logging.Level;

import org.pircbotx.PircBotX;
import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.PrivateMessageEvent;

import com.jcabi.jdbc.JdbcSession;
import com.jcabi.jdbc.Outcome;

import me.smc.sb.multi.Tournament;
import me.smc.sb.utils.Configuration;
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
		
		for(int i = 0; i < args.length - 2; i++) tournamentName += args[i] + " ";
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
			Connection upSQL = DriverManager.getConnection(login.getValue("WTURL"), 
											  			   login.getValue("WTUser"),
											  			   login.getValue("WTPass"));
			
			new JdbcSession(upSQL)
			.sql("UPDATE wt_teammembers SET confirmed=1 " +
				 "WHERE `wt_teammembers_data.user_id`=? AND " +
				 "`wt_teams.id`=?")
			.set(userID)
			.set(teamID)
			.update(Outcome.VOID);
			
			Connection delSQL = DriverManager.getConnection(login.getValue("WTURL"), 
					  						  				login.getValue("WTUser"),
					  						  				login.getValue("WTPass"));
			
			new JdbcSession(delSQL)
			.sql("DELETE FROM wt_teammembers " +
				 "WHERE `wt_teammembers_data.user_id`=? AND " +
				 "confirmed=0")
			.set(userID)
			.update(Outcome.VOID);
		}catch(Exception ex){
			Log.logger.log(Level.SEVERE, ex.getMessage(), ex);
			return "Could not connect to database!";
		}
		
		Utils.info(e, pe, discord, "Your participation is now confirmed! Thanks for registering!");
		
		return "";
	}
	
}