package commands;

import java.util.List;

import data.CommandCategory;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import utils.Constants;
import utils.DiscordChatUtils;

/**
 * A command giving command references and support information.
 * 
 * @author Smc
 */
public class HelpCommand extends Command {

	public HelpCommand() {
		super(null, false, true, CommandCategory.GENERAL, new String[]{"help"}, 
			  "Shows command list or specific command information.", 
			  "Shows the command list based on the server this command was used from.\n" +
			  "Also allows the user to get more information about specific commands.", 
			  new String[]{"help", "Shows the command list."}, 
			  new String[]{"help <command>", "Displays a detailed help page for the specified command."});
	}

	@Override
	public void onCommand(MessageReceivedEvent p_event, String[] args) {
		EmbedBuilder builder = new EmbedBuilder();
		
		builder.setColor(Constants.DEFAULT_EMBED_COLOR);
		builder.setFooter("Official rBot server: " + Constants.SUPPORT_SERVER_LINK);
		
		if(args.length > 0) {
			Command cmd = Command.findCommand(args[0]);
			
			if(cmd != null) {
				if(!cmd.canUse(p_event.getAuthor(), p_event.getChannel())) return;
				
				builder.setAuthor("Command information for " + cmd.getTriggers()[0],
								  Constants.SUPPORT_SERVER_LINK,
								  p_event.getJDA().getSelfUser().getAvatarUrl());
				builder.setColor(cmd.getCategory().getColor());
				
				String description = "**__" + cmd.getDescription() + "__**\n\n";
				String[][] usages = cmd.getUsages();
				String prefix = DiscordChatUtils.getPrefix(p_event.getChannel());
				
				for(int i = 0; i < usages.length; i++) {
					String[] usage = usages[i];
					
					description += "```\n" + prefix + usage[0] + "```\n";
					description += usage[1] + "\n";
					
					if(i + 1 < usages.length) description += "\n";
				}
				
				String[] triggers = cmd.getTriggers();
				
				if(triggers.length > 1) {
					description += "\n**Alias" + (triggers.length > 2 ? "es" : "") + "**:";
					
					for(int i = 1; i < triggers.length; i++)
						description += " `" + triggers[i] + "`";
				}
				
				builder.setDescription(description);
			} else {
				DiscordChatUtils.message(p_event.getChannel(), "Command not found!\n" + 
										 "Use " + DiscordChatUtils.getPrefix(p_event.getChannel()) + 
										 "help to get the full command list!");
				
				return;
			}
		} else {
			builder.setAuthor("rBot command list", Constants.SUPPORT_SERVER_LINK,
							  p_event.getJDA().getSelfUser().getAvatarUrl());
			
			String description = "**__Use `" + DiscordChatUtils.getPrefix(p_event.getChannel()) + 
								 "help <command>` for details about specific commands.__**\n"
								 + (p_event.isFromGuild() ? "**__Only commands you can use in this channel are listed.__**\n" : "\n");
			
			for(CommandCategory category : CommandCategory.values()) {
				List<Command> commands = Command.findCommandsInCategory(category);
				
				if(commands.size() > 0) {
					String categoryText = "";
					
					for(Command cmd : commands)
						if(cmd.canUse(p_event.getAuthor(), p_event.getChannel()))
							categoryText += " `" + cmd.getTriggers()[0] + "`";
					
					categoryText = categoryText.substring(1);
					
					builder.addField(category.getName(), categoryText, true);
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
		
		DiscordChatUtils.embed(p_event.getChannel(), builder.build());
	}
}
