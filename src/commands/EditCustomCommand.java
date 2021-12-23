package commands;

import java.util.List;

import data.CommandCategory;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Button;
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

	// TODO: set desc?
	@Override
	public void onCommand(SlashCommandEvent p_event, List<OptionMapping> p_options) {
		if(p_options.size() == 0) {
			sendInvalidArgumentsError(p_event);
			return;
		}
		
		String commandName = p_options.get(0).getAsString();
		
		if(p_options.size() == 1) { // delete
			CustomCommand command = CustomCommand.getCommand(p_event.getGuild().getId(), commandName);
			
			if(command != null) {
				String buttonIdPrefix = getTrigger() + "|||" + commandName + "|||";
				
				p_event.reply("Are you sure you wish to delete **`/" + commandName + "`**?")
					   .addActionRow(Button.danger(buttonIdPrefix + "yes", "Yes"),
							   		 Button.primary(buttonIdPrefix + "no", "No"))
					   .setEphemeral(true)
					   .queue();
			} else DiscordChatUtils.message(p_event, "This command doesn't exist!", false, false);
		} else { // add/edit
			String instructions = "";
			
			for(int i = 1; i < p_options.size(); i++)
				instructions += " " + p_options.get(i).getAsString();
			
			if(new CustomCommand(commandName, p_event.getGuild().getId(), 
								 instructions.substring(1)).save()) {
				p_event.getGuild().upsertCommand(commandName, " ").queue();
				DiscordChatUtils.message(p_event, "Command saved!", false, false);
			} else {
				DiscordChatUtils.message(p_event, "There was an error saving the command!\n" +
												  "Please try again later or contact the developer via the support server!", false, false,
												  ActionRow.of(Button.link(Constants.SUPPORT_SERVER_LINK, "Support Server")));
			}
		}
	}
	
	@Override
	public void onButtonClick(ButtonClickEvent p_event, String[] p_args) {
		if(p_args.length != 2) {
			p_event.reply("An error occured while executing this command!\nPlease try again later or contact the developer via the support server!")
				   .addActionRow(Button.link(Constants.SUPPORT_SERVER_LINK, "Support Server"))
				   .queue();
			return;
		}

		p_event.editComponents().queue();
		
		if(p_args[1].equalsIgnoreCase("yes")) {
			CustomCommand command = CustomCommand.getCommand(p_event.getGuild().getId(), p_args[0]);
			
			if(command != null) {
				command.delete();
				
				Guild guild = p_event.getGuild();
				net.dv8tion.jda.api.interactions.commands.Command discordCommand = guild.retrieveCommands().complete().stream()
																													  .filter(c -> c.getName().equalsIgnoreCase(p_args[0]))
																													  .findFirst().orElse(null);
				if(discordCommand != null)
					guild.deleteCommandById(discordCommand.getId()).queue();
				
				p_event.getHook().editOriginal("Command deleted!").queue();
			} else p_event.getHook().editOriginal("This command was already deleted!").queue();
		} else {
			p_event.getHook().editOriginal("This command was not deleted.").queue();
		}
	}
}
