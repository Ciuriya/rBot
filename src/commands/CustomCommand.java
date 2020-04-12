package commands;

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
		m_instruction = p_instruction; // do not forget to sanitize these
	}

}
