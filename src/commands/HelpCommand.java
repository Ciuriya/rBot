package commands;

import java.util.List;

import data.CommandCategory;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import utils.Constants;
import utils.DiscordChatUtils;

/**
 * A command giving command references and support information.
 * 
 * @author Smc
 */
public class HelpCommand extends Command {

	public HelpCommand() {
		super(buildCommandInfo());
	}
	
	private static CommandInfo buildCommandInfo() {
		CommandInfo info = new CommandInfo();
		
		info.permission = null;
		info.adminOnly = false;
		info.allowsDm = true;
		info.bypassMessageSendPermissions = false;
		info.category = CommandCategory.GENERAL;
		info.trigger = "help";
		info.description = "Shows command list or specific command information.";
		info.usages = new String[][] { new String[] {"help", "Shows the command list."}, 
									   new String[] {"help <command>", "Displays a detailed help page for the specified command."} };
		
		return info;
	}
	
	@Override
	public CommandData generateCommandData() {
		CommandData data = new CommandData(getTrigger(), getDescription());
		
		data.addOption(OptionType.STRING, "command", "Displays a detailed help page for the specified command.");
		
		return data;
	}

	@Override
	public void onCommand(SlashCommandEvent p_event, List<OptionMapping> p_options) {
		EmbedBuilder builder = new EmbedBuilder();
		
		builder.setColor(Constants.DEFAULT_EMBED_COLOR);
		builder.setFooter("Official rBot server: " + Constants.SUPPORT_SERVER_LINK);
		
		MessageChannel channel = p_event.getChannel();
		User user = p_event.getUser();
		
		if(p_options.size() > 0) {
			Command cmd = Command.findCommand(p_options.get(0).getAsString());
			
			if(cmd != null) {
				if(!cmd.canUse(user, channel)) return;
				
				builder.setAuthor("Command information for " + cmd.getTrigger(),
								  Constants.SUPPORT_SERVER_LINK,
								  p_event.getJDA().getSelfUser().getAvatarUrl());
				builder.setColor(cmd.getCategory().getColor());
				
				String description = "**__" + cmd.getDescription() + "__**\n\n";
				String[][] usages = cmd.getUsages();
				
				for(int i = 0; i < usages.length; i++) {
					String[] usage = usages[i];
					
					description += "```\n/" + usage[0] + "```\n";
					description += usage[1] + "\n";
					
					if(i + 1 < usages.length) description += "\n";
				}
				
				builder.setDescription(description);
			} else {
				DiscordChatUtils.message(p_event, "Command not found!\nUse **__`/help`__** to get the full command list!", false);
				
				return;
			}
		} else {
			builder.setAuthor("rBot command list", Constants.SUPPORT_SERVER_LINK,
							  p_event.getJDA().getSelfUser().getAvatarUrl());
			
			String description = "**__Use `/help <command>` for details about specific commands.__**\n"
								 + (p_event.isFromGuild() ? "**__Only commands you can use in this channel are listed.__**\n" : "\n");
			
			for(CommandCategory category : CommandCategory.values()) {
				List<Command> commands = Command.findCommandsInCategory(category);
				
				if(commands.size() > 0) {
					String categoryText = "";
					
					for(Command cmd : commands)
						if(cmd.canUse(user, channel))
							categoryText += " `" + cmd.getTrigger() + "`";
					
					if(categoryText.length() > 0) {
						categoryText = categoryText.substring(1);
						builder.addField(category.getName(), categoryText, true);
					}
				}
			}
			
			if(p_event.isFromGuild()) {
				String customCommandText = "";
				List<String> customCommands = CustomCommand.getAllCommandTriggers(p_event.getGuild().getId());
				
				if(customCommands.size() > 0) {
					for(String trigger : customCommands)
						customCommandText += " `" + trigger + "`";
					
					builder.addField("Custom", customCommandText.substring(1), true);
				}
			}
			
			builder.setDescription(description);
		}
		
		DiscordChatUtils.embed(p_event, builder.build(), false);
	}
}
