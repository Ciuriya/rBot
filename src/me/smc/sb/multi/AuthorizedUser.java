package me.smc.sb.multi;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import com.jcabi.jdbc.JdbcSession;
import com.jcabi.jdbc.Outcome;
import com.jcabi.jdbc.SingleOutcome;

import me.smc.sb.main.Main;
import me.smc.sb.utils.Log;

public class AuthorizedUser{

	private int userId;
	private String discordId, osuName;
	private List<Integer> tournamentsOwned;
	private List<Integer> tournamentsAllowed;
	
	public AuthorizedUser(String discordId, String osuName){
		this.discordId = discordId;
		this.osuName = osuName;
		
		save(true);
	}
	
	public AuthorizedUser(int userId, String discordId, String osuName){
		this.userId = userId;
		this.discordId = discordId;
		this.osuName = osuName;
		tournamentsOwned = new ArrayList<Integer>();
		tournamentsAllowed = new ArrayList<Integer>();
		
		loadAuthorizations();
	}
	
	public String getDiscordId(){
		return discordId;
	}
	
	public String getOsuName(){
		return osuName;
	}
	
	public void setDiscordId(String discordId){
		this.discordId = discordId;
		save(false);
	}
	
	public void setOsuName(String osuName){
		this.osuName = osuName;
		save(false);
	}
	
	public boolean isAllowed(int id){
		for(int tournament : tournamentsAllowed)
			if(tournament == id)
				return true;
		return false;
	}
	
	public boolean ownsTournament(int id){
		for(int tournament : tournamentsOwned)
			if(tournament == id)
				return true;
		return false;
	}
	
	//relation, false = allowed, true = owned
	public void addAuthorization(int tournamentId, boolean relation){
		if((relation && ownsTournament(tournamentId)) ||
		   (!relation && isAllowed(tournamentId)))
			return;
		
		try{
			new JdbcSession(Main.sqlConnection)
			.sql("INSERT INTO Authorization (id_user, id_tournament, relation) " +
			     "VALUES (?, ?, ?)")
			.set(userId)
			.set(tournamentId)
			.set(relation ? 1 : 0)
			.insert(Outcome.VOID);
			
			 if(relation)
				 tournamentsOwned.add(tournamentId);
			 else tournamentsAllowed.add(tournamentId);
		}catch(Exception e){
			Log.logger.log(Level.SEVERE, e.getMessage(), e);
		}
	}
	
	public void removeAuthorization(int tournamentId, boolean relation){
		if((relation && !ownsTournament(tournamentId)) ||
		   (!relation && !isAllowed(tournamentId)))
			return;
		
		try{
			new JdbcSession(Main.sqlConnection)
			.sql("DELETE FROM Authorization " +
			     "WHERE id_user='?' AND id_tournament='?' AND relation='?'")
			.set(userId)
			.set(tournamentId)
			.set(relation ? 1 : 0)
			.update(Outcome.VOID);
			
			 if(relation)
				 tournamentsOwned.remove(tournamentId);
			 else tournamentsAllowed.remove(tournamentId);
		}catch(Exception e){
			Log.logger.log(Level.SEVERE, e.getMessage(), e);
		}
	}
	
	public void loadAuthorizations(){
		try{
			new JdbcSession(Main.sqlConnection)
			.sql("SELECT id_tournament, relation FROM Authorization WHERE id_user = ?")
			.set(userId)
			.select(new Outcome<List<String>>(){
		    	 @Override public List<String> handle(ResultSet rset, Statement stmt) throws SQLException{
		    		 while(rset.next())
		    			 if(rset.getBoolean(1))
		    				 tournamentsOwned.add(rset.getInt(0));
		    			 else tournamentsAllowed.add(rset.getInt(0));
		    		 
		    		 return new ArrayList<String>();
		    	 }
		     });
		}catch(Exception e){
			Log.logger.log(Level.SEVERE, e.getMessage(), e);
		}
	}
	
	public static void load(){
		try{
			new JdbcSession(Main.sqlConnection)
			.sql("SELECT id_user, id_discord, osu_user FROM AuthorizedUser")
			.select(new Outcome<List<String>>(){
		    	 @Override public List<String> handle(ResultSet rset, Statement stmt) throws SQLException{
		    		 while(rset.next())
		    			 new AuthorizedUser(rset.getInt(0), rset.getString(1), rset.getString(2));
		    		 
		    		 return new ArrayList<String>();
		    	 }
		     });
		}catch(Exception e){
			Log.logger.log(Level.SEVERE, e.getMessage(), e);
		}
	}
	
	public void save(boolean add){
		try{
			if(add){
				userId = new JdbcSession(Main.sqlConnection)
				.sql("INSERT INTO AuthorizedUser (id_discord, osu_user) " +
				     "VALUES (?, ?)")
				.set(discordId)
				.set(osuName)
				.insert(new SingleOutcome<Integer>(Integer.class));
			}else{
				new JdbcSession(Main.sqlConnection)
				.sql("UPDATE AuthorizedUser " +
					 "SET id_discord='?', osu_user='?' " +
					 "WHERE id_user='?'")
				.set(discordId)
				.set(osuName)
				.set(userId)
				.update(Outcome.VOID);
			}
		}catch(Exception e){
			Log.logger.log(Level.SEVERE, e.getMessage(), e);
		}
	}
	
	public void delete(){
		try{
			if(!tournamentsOwned.isEmpty()) 
				for(int tournamentId : new ArrayList<Integer>(tournamentsOwned))
					removeAuthorization(tournamentId, true);
			
			if(!tournamentsAllowed.isEmpty()) 
				for(int tournamentId : new ArrayList<Integer>(tournamentsAllowed))
					removeAuthorization(tournamentId, false);
			
			new JdbcSession(Main.sqlConnection)
			.sql("DELETE FROM AuthorizedUser " +
			     "WHERE id_user='?'")
			.set(userId)
			.update(Outcome.VOID);
		}catch(Exception e){
			Log.logger.log(Level.SEVERE, e.getMessage(), e);
		}
	}
	
}