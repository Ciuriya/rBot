package commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import data.BotAdmins;
import data.CommandCategory;
import main.Main;
import managers.ApplicationStats;
import managers.ThreadingManager;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.GuildChannel;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.ThreadChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction;
import utils.DiscordChatUtils;

/**
 * This class represents a command that can be called from discord.
 * This represents only global commands.
 * 
 * @author Smc
 */
public abstract class Command {
	
	public static List<Command> commands = new ArrayList<>();
	
	private CommandInfo m_commandInfo;

	public Command(CommandInfo p_commandInfo) {
		m_commandInfo = p_commandInfo;
		
		commands.add(this);
	}
	
	public String getTrigger() {
		return m_commandInfo.trigger;
	}
	
	public boolean allowsDm() {
		return m_commandInfo.allowsDm;
	}
	
	public CommandCategory getCategory() {
		return m_commandInfo.category;
	}
	
	public String getDescription() {
		return m_commandInfo.description;
	}
	
	public String[][] getUsages() {
		return m_commandInfo.usages;
	}
	
	public boolean canUse(User p_user, MessageChannel p_channel) {
		if(BotAdmins.isAdmin(p_user)) return true;
		if(m_commandInfo.adminOnly) return false;
		if(m_commandInfo.permission == null) return true;
		
		if(p_channel.getType() == ChannelType.PRIVATE) return true;
		else {
			Guild guild;
			
			if(p_channel instanceof TextChannel)
				guild = ((TextChannel) p_channel).getGuild();
			else if(p_channel instanceof ThreadChannel)
				guild = ((ThreadChannel) p_channel).getGuild();
			else return false;
			
			Member member = guild.retrieveMember(p_user).complete();
			
			if(member != null)
				return member.hasPermission((GuildChannel) p_channel, m_commandInfo.permission);
		}
		
		return false;
	}
	
	protected void sendInvalidArgumentsError(SlashCommandEvent p_event) {
		DiscordChatUtils.message(p_event, "Invalid arguments! Use **__/help " + m_commandInfo.trigger + "__** for info on this command!", false);
	}
	
	public static Command findCommand(String p_trigger) {
		return commands.stream().filter(c -> Arrays.asList(c.getTrigger()).stream()
											 .anyMatch(t -> t.equalsIgnoreCase(p_trigger)))
							 	.findFirst().orElse(null);
	}
	
	public static List<Command> findCommandsInCategory(CommandCategory p_category) {
		return commands.stream().filter(c -> c.getCategory() == p_category).collect(Collectors.toList());
	}
	
	public static boolean handleCommand(SlashCommandEvent p_event, String p_trigger, List<OptionMapping> p_options) {
		Command cmd = findCommand(p_trigger);
		
		if(cmd != null) {
			if(!cmd.allowsDm() && !p_event.isFromGuild()) return false;
			
			ApplicationStats.getInstance().addCommandUsed();
			
			boolean canMessageChannel = cmd.m_commandInfo.bypassMessageSendPermissions || 
										DiscordChatUtils.checkMessagePermissionForChannel(p_event.getMessageChannel());
			
			if(canMessageChannel && cmd.canUse(p_event.getUser(), p_event.getChannel())) {
				ThreadingManager.getInstance().executeAsync(new Runnable() {
					public void run() {
						cmd.onCommand(p_event, p_options);
					}
				}, 30000, true);
			} else DiscordChatUtils.sendMessagePermissionCheckFailedMessage(p_event);
			
			return true;
		}
		
		return false;
	}
	
	public abstract CommandData generateCommandData();
	
	public abstract void onCommand(SlashCommandEvent p_event, List<OptionMapping> p_options);
	
	public static void registerCommands() {
		new HelpCommand();
		new EditCustomCommand();
		new StopCommand();
		
		CommandListUpdateAction slashCommands = Main.discordApi.updateCommands();
		
		List<CommandData> commandDataList = new LinkedList<>();
		
		for(Command command : commands) {
			commandDataList.add(command.generateCommandData());
		}
		
		slashCommands.addCommands(commandDataList);
		slashCommands.complete();
	}
}

class CommandInfo {
	public String trigger;
	public Permission permission;
	public boolean adminOnly;
	public boolean allowsDm;
	public boolean bypassMessageSendPermissions;
	public CommandCategory category;
	public String description;
	public String[][] usages; // contains the usage first and the description of it in second
}
