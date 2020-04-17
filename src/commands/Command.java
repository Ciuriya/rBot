package commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import data.BotAdmins;
import data.CommandCategory;
import managers.ApplicationStats;
import managers.ThreadingManager;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

/**
 * This class represents a command that can be called from discord.
 * This represents only global commands.
 * 
 * @author Smc
 */
public abstract class Command {
	
	public static List<Command> commands = new ArrayList<>();
	private String[] m_triggers;
	private Permission m_permission;
	private boolean m_adminOnly;
	private boolean m_allowsDm;
	private CommandCategory m_category;
	private String m_shortDescription;
	private String m_description;
	private String[][] m_usages; // contains the usage first and the description of it in second

	public Command(Permission p_perm, boolean p_adminOnly, boolean p_allowsDm, CommandCategory p_category,
				   String[] p_triggers, String p_shortDesc, String p_desc, String[]... p_usages) {
		m_triggers = p_triggers;
		m_permission = p_perm;
		m_adminOnly = p_adminOnly;
		m_allowsDm = p_allowsDm;
		m_category = p_category;
		m_shortDescription = p_shortDesc;
		m_description = p_desc;
		m_usages = p_usages;
		
		commands.add(this);
	}
	
	public String[] getTriggers() {
		return m_triggers;
	}
	
	public boolean allowsDm() {
		return m_allowsDm;
	}
	
	public CommandCategory getCategory() {
		return m_category;
	}
	
	public String getShortDescription() {
		return m_shortDescription;
	}
	
	public String getDescription() {
		return m_description;
	}
	
	public String[][] getUsages() {
		return m_usages;
	}
	
	public boolean canUse(User p_user, MessageChannel p_channel) {
		if(m_permission == null) return true;
		if(BotAdmins.isAdmin(p_user)) return true;
		if(m_adminOnly) return false;
		
		if(p_channel.getType() == ChannelType.PRIVATE) return true;
		else {
			TextChannel text = ((TextChannel) p_channel);
			Member member = text.getGuild().retrieveMember(p_user).complete();
			
			if(member != null)
				return member.hasPermission(text, m_permission);
		}
		
		return false;
	}
	
	public static Command findCommand(String p_trigger) {
		return commands.stream().filter(c -> Arrays.asList(c.m_triggers).stream()
											 .anyMatch(t -> t.equalsIgnoreCase(p_trigger)))
							 	.findFirst().orElse(null);
	}
	
	public static List<Command> findCommandsInCategory(CommandCategory p_category) {
		return commands.stream().filter(c -> c.m_category == p_category).collect(Collectors.toList());
	}
	
	public static boolean handleCommand(MessageReceivedEvent p_event, String p_message) {
		String trigger = p_message.split(" ")[0];
		String[] args = p_message.replace(trigger + " ", "").split(" ");
		Command cmd = findCommand(trigger);
		
		if(cmd != null) {
			if(!cmd.m_allowsDm && !p_event.isFromGuild()) return false;
			
			ApplicationStats.getInstance().addCommandUsed();
			
			ThreadingManager.getInstance().executeAsync(new Runnable() {
				public void run() {
					if(p_message.contains(" ")) cmd.onCommand(p_event, args);
					else cmd.onCommand(p_event, new String[]{});
				}
			}, 30000, true);
			
			return true;
		}
		
		return false;
	}
	
	public abstract void onCommand(MessageReceivedEvent p_event, String[] args);
	
	public static void registerCommands() {
		new EditCustomCommand();
		new HelpCommand();
		new StopCommand();
	}
}
