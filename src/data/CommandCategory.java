package data;

import java.awt.Color;

import utils.Constants;

/**
 * This represents the categories a command can fall under.
 * Displayed in the help command!
 * 
 * @author Smc
 */
public enum CommandCategory {
	
	GENERAL("General", Constants.DEFAULT_EMBED_COLOR),
	ADMIN("Admin", Color.GRAY);
	
	private String m_displayName;
	private Color m_color;
	
	private CommandCategory(String p_displayName, Color p_color) {
		m_displayName = p_displayName;
		m_color = p_color;
	}

	public String getName() {
		return m_displayName;
	}
	
	public Color getColor() {
		return m_color;
	}
}
