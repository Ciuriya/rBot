package utils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.logging.Level;

import data.Log;
import managers.DatabaseManager;

/**
 * An utility class containing functions with common calls in them.
 * 
 * @author Smc
 */
public class SQLUtils {
	
	public static void setupGuildSQL(String p_guildId) {
		try(Connection conn = DatabaseManager.getInstance().get("discord").getConnection()) {
			PreparedStatement st = conn.prepareStatement(
								   "INSERT IGNORE INTO `discord-guild` SET `id`=?");
			
			st.setString(1, p_guildId);
			
			st.executeUpdate();
			st.close();
		} catch(Exception e) {
			Log.log(Level.SEVERE, "Could not setup guild (" + p_guildId + ") SQL", e);
		}
	}

	public static String getGuildSetting(String p_guildId, String p_key, String p_channelId) {
		String setting = "";
		
		try(Connection conn = DatabaseManager.getInstance().get("discord").getConnection()) {
			PreparedStatement st = conn.prepareStatement(
								   "SELECT field FROM `guild-settings` WHERE `guild-id`=? " +
								   "AND `key`=? AND (`channel-id` IS NULL OR `channel-id`=?) " +
								   " ORDER BY CASE WHEN `channel-id` IS NULL THEN 1 ELSE 0 END, " +
								   "`channel-id`"); // this should make it so nulls are last
			
			st.setString(1, p_guildId);
			st.setString(2, p_key);
			st.setString(3, p_channelId);
			
			ResultSet rs = st.executeQuery();
	
			if(rs.next()) setting = rs.getString(1);
			
			rs.close();
			st.close();
		} catch(Exception e) {
			Log.log(Level.SEVERE, "Could not fetch guild setting: " + p_key + 
								  " for guild " + p_guildId + " in " + p_channelId, e);
		}
		
		return setting;
	}
}
