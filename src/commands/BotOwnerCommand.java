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
 * A command allowing a bot admin to execute specific operations that should be locked to bot admins.
 * 
 * @author Smc
 */
public class BotOwnerCommand extends Command {

	public BotOwnerCommand() {
		super(buildCommandInfo());
	}
	
	private static CommandInfo buildCommandInfo() {
		CommandInfo info = new CommandInfo();
		
		info.permission = null;
		info.adminOnly = true;
		info.allowsDm = true;
		info.bypassMessageSendPermissions = true;
		info.category = CommandCategory.ADMIN;
		info.trigger = "botowner";
		info.description = "Command restricted to the bot owner.";
		info.usages = new String[][] { new String[]{"botowner stop", "Stops the bot."},
			  						   new String[]{"botowner restart", "Restarts the bot."},
			  						   new String[]{"botowner update", "Updates the bot."} };
		
		return info;
	}
	
	@Override
	public CommandData generateCommandData() {
		CommandData data = new CommandData(getTrigger(), getDescription());
		
		data.addOption(OptionType.STRING, "operation", "The operation for the bot to execute.", true);
		
		return data;
	}

	@Override
	public void onCommand(SlashCommandEvent p_event, List<OptionMapping> p_options) {
		switch(p_options.get(0).getAsString().toLowerCase()) {
			case "stop": 
				stopCommand(p_event, 1); 
				break;
			case "restart": 
				stopCommand(p_event, 2); 
				break;
			case "update": 
				stopCommand(p_event, 3); 
				break;
			default: 
				DiscordChatUtils.message(p_event, "Invalid operation! Check **__`/help botowner`__** for more info", true, false);
		}

	}
	
	private void stopCommand(SlashCommandEvent p_event, int p_code) {	
		DiscordChatUtils.message(p_event, getCodeMessage(p_code), true, false);
		Main.stop(p_code);
	}
	
	private String getCodeMessage(int p_code) {
		switch(p_code) {
			case 2: return "Restarting...";
			case 3: return "Updating...";
			default: return "Shutting down...";
		}
	}
}
