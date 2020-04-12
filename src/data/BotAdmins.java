package data;

import net.dv8tion.jda.api.entities.User;

/**
 * This enum lists all users having administrative powers over the bot.
 * 
 * @author Smc
 */
public enum BotAdmins {
	
	Smc("91302128328392704"),
	Auto("91184384442384384");
	
	private String m_discordId;
	
	private BotAdmins(String p_discordId) {
		m_discordId = p_discordId;
	}

	public static boolean isAdmin(User p_user) {
		for(BotAdmins admin : BotAdmins.values())
			if(admin.m_discordId.equals(p_user.getId()))
				return true;
		
		return false;
	}
}
