package commands;

import java.util.List;

import data.CommandCategory;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import utils.Constants;
import utils.DiscordChatUtils;

/**
 * A command allowing the modification of custom commands by discord users.
 * 
 * @author Smc
 */
public class EditCustomCommand extends Command {

	public EditCustomCommand() {
		super(buildCommandInfo());
	}
	
	private static CommandInfo buildCommandInfo() {
		CommandInfo info = new CommandInfo();
		
		info.permission = Permission.MESSAGE_MANAGE;
		info.adminOnly = false;
		info.allowsDm = false;
		info.bypassMessageSendPermissions = false;
		info.category = CommandCategory.ADMIN;
		info.trigger = "editcmd";
		info.description = "Create, edit or delete custom commands in a server. Requires the Manage Messages permission.";
		info.usages = new String[][] { new String[] {"editcmd <command name> <instructions...>", 
											  	     "Adds or edits a command in the server under the command name given using " +
									  			     "the instructions given."}, 
									   new String[] {"editcmd <command name>", "Deletes the command in the server."}};
		
		return info;
	}
	
	@Override
	public CommandData generateCommandData() {
		CommandData data = new CommandData(getTrigger(), getDescription());
		
		data.addOption(OptionType.STRING, "command", "The command to create, edit or remove. Using no other arguments will delete the command.", true);
		data.addOption(OptionType.STRING, "instructions", "Creates or edits the command with provided instructions.");
		
		return data;
	}

	@Override
	public void onCommand(SlashCommandEvent p_event, List<OptionMapping> p_options) {
		if(p_options.size() == 0) {
			sendInvalidArgumentsError(p_event);
			return;
		}
		
		String commandName = p_options.get(0).getAsString();
		
		if(p_options.size() == 1) { // delete
			if(new CustomCommand(commandName, p_event.getGuild().getId(), "").delete() > 0)
				DiscordChatUtils.message(p_event, "Command deleted!", false);
			else DiscordChatUtils.message(p_event, "This command already doesn't exist!", false);
		} else { // add/edit
			String instructions = "";
			
			for(int i = 1; i < p_options.size(); i++)
				instructions += " " + p_options.get(i).getAsString();
			
			if(new CustomCommand(commandName, p_event.getGuild().getId(), 
								 instructions.substring(1)).save())
				DiscordChatUtils.message(p_event, "Command saved!", false);
			else DiscordChatUtils.message(p_event, "There was an error saving the command!\n" +
												   "Please try again later or contact the developer via the **__report__** command or the support server!\n<" +
												   Constants.SUPPORT_SERVER_LINK + ">", false);
		}
	}
}
