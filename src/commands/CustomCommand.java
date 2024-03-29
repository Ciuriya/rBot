package commands;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import data.Log;
import managers.ApplicationStats;
import managers.DatabaseManager;
import managers.ThreadingManager;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import utils.DiscordChatUtils;

/**
 * This class represents a user-made command in a guild that can be called from discord.
 * Note: only loaded as required
 * 
 * @author Smc
 */
public class CustomCommand {
	
	private String m_trigger;
	private String m_instruction;
	private String m_guildId;
	
	public CustomCommand(String p_trigger, String p_guildId, String p_instruction) {
		m_trigger = p_trigger;
		m_guildId = p_guildId;
		m_instruction = p_instruction;
	}

	public String getTrigger() {
		return m_trigger;
	}
	
	public String getInstruction() {
		return m_instruction;
	}
	
	public String getGuildId() {
		return m_guildId;
	}
	
	public void execute(SlashCommandEvent p_event) {
		if(!DiscordChatUtils.checkMessagePermissionForChannel(p_event.getMessageChannel())) {
			DiscordChatUtils.sendMessagePermissionCheckFailedMessage(p_event);
			return;
		}
		
		p_event.deferReply().queue();
		
		ThreadingManager.getInstance().executeAsync(new Runnable() {
			public void run() {
				ApplicationStats.getInstance().addCommandUsed();

				// TODO: this implementation of custom commands is obviously very basic
				// it would be nice to match rBot's actual custom commands in the future
				
				p_event.getHook().sendMessage(m_instruction).queue();
			}
		}, 30000, true);
	}
	
	public boolean save() {
		try(Connection conn = DatabaseManager.getInstance().get("discord").getConnection()) {
			PreparedStatement st = conn.prepareStatement(
								   "INSERT INTO `commands` (`guild-id`, `trigger`, `data`) " +
								   "VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE `data`=?");
			
			st.setString(1, m_guildId);
			st.setString(2, m_trigger.toLowerCase());
			st.setString(3, m_instruction);
			st.setString(4, m_instruction);
			
			st.executeUpdate();
			st.close();
			
			return true;
		} catch(Exception e) {
			Log.log(Level.SEVERE, "Could not save custom command " + m_trigger.toLowerCase() + " for guild " + m_guildId +
								  "\nInstruction: " + m_instruction, e);
			return false;
		}
	}
	
	public int delete() {
		try(Connection conn = DatabaseManager.getInstance().get("discord").getConnection()) {
			PreparedStatement st = conn.prepareStatement(
								   "DELETE FROM `commands` WHERE `guild-id`=? AND `trigger`=?");
			
			st.setString(1, m_guildId);
			st.setString(2, m_trigger.toLowerCase());
			
			int rowsDeleted = st.executeUpdate();
			st.close();
			
			return rowsDeleted;
		} catch(Exception e) {
			return 0;
		}
	}
	
	// TODO: PLEASE cache these
	public static CustomCommand getCommand(String p_guildId, String p_trigger) {
		String instruction = "";
		
		try(Connection conn = DatabaseManager.getInstance().get("discord").getConnection()) {
			PreparedStatement st = conn.prepareStatement(
								   "SELECT data FROM `commands` WHERE `guild-id`=? AND " +
								   "`trigger`=?");
			
			st.setString(1, p_guildId);
			st.setString(2, p_trigger.toLowerCase());
			
			ResultSet rs = st.executeQuery();
	
			if(rs.next()) instruction = rs.getString(1);
			
			rs.close();
			st.close();
		} catch(Exception e) { }
		
		if(instruction.length() == 0) return null;
		
		return new CustomCommand(p_trigger, p_guildId, instruction);
	}
	
	public static List<String> getAllCommandTriggers(String p_guildId) {
		List<String> commands = new ArrayList<>();
		
		try(Connection conn = DatabaseManager.getInstance().get("discord").getConnection()) {
			PreparedStatement st = conn.prepareStatement(
								   "SELECT `trigger` FROM `commands` WHERE `guild-id`=?");
			
			st.setString(1, p_guildId);
			
			ResultSet rs = st.executeQuery();
	
			while(rs.next()) commands.add(rs.getString(1));
			
			rs.close();
			st.close();
		} catch(Exception e) { }
		
		return commands;
	}
}
