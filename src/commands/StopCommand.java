package commands;

import java.util.List;

import data.CommandCategory;
import main.Main;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import utils.DiscordChatUtils;

/**
 * A command allowing a bot admin to stop, restart or update the bot.
 * 
 * @author Smc
 */
public class StopCommand extends Command {

	public StopCommand() {
		super(buildCommandInfo());
	}
	
	private static CommandInfo buildCommandInfo() {
		CommandInfo info = new CommandInfo();
		
		info.permission = null;
		info.adminOnly = true;
		info.allowsDm = true;
		info.bypassMessageSendPermissions = true;
		info.category = CommandCategory.ADMIN;
		info.trigger = "stop";
		info.description = "Stops, restarts or updates the bot. Limited to bot owner only.";
		info.usages = new String[][] { new String[]{"stop", "Stops the bot."},
			  						   new String[]{"stop restart", "Restarts the bot."},
			  						   new String[]{"stop update", "Updates the bot."} };
		
		return info;
	}
	
	@Override
	public CommandData generateCommandData() {
		CommandData data = new CommandData(getTrigger(), getDescription());
		
		data.addOption(OptionType.STRING, "operation", "The operation for the bot to execute. Valid operations: stop, restart, update.");
		
		return data;
	}

	@Override
	public void onCommand(SlashCommandEvent p_event, List<OptionMapping> p_options) {
		int code = 1;
		
		if(p_options.size() > 0) {
			String operation = p_options.get(0).getAsString();
			
			if(operation.equalsIgnoreCase("restart")) code = 2;
			else if(operation.equalsIgnoreCase("update")) code = 3;
		}
		
		DiscordChatUtils.message(p_event, getCodeMessage(code), true);
		Main.stop(code);
	}
	
	private String getCodeMessage(int p_code) {
		switch(p_code) {
			case 2: return "Restarting...";
			case 3: return "Updating...";
			default: return "Shutting down...";
		}
	}
}
