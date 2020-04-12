package utils;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
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
			Statement st = conn.createStatement();
			
			st.executeUpdate("INSERT IGNORE INTO `discord-guild` SET `id`=\"" + p_guildId + "\"");
			st.close();
		} catch(Exception e) {
			Log.log(Level.SEVERE, "Could not setup guild (" + p_guildId + ") SQL", e);
		}
	}

	public static String getGuildSetting(String p_guildId, String p_key, String p_channelId) {
		String setting = "";
		
		try(Connection conn = DatabaseManager.getInstance().get("discord").getConnection()) {
			Statement st = conn.createStatement();
			ResultSet rs = st.executeQuery(
							"SELECT field FROM `guild-settings` " + 
							"WHERE `guild-id`=\"" + p_guildId + 
							"\" AND `key`=\"" + p_key + "\" AND " + 
							"(`channel-id` IS NULL OR `channel-id`=\"" + 
							p_channelId + "\") ORDER BY CASE WHEN " + // this should make it so nulls are last
							"`channel-id` IS NULL THEN 1 ELSE 0 END, `channel-id`");
	
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
