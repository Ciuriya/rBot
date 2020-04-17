package commands;

import data.CommandCategory;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import utils.DiscordChatUtils;

/**
 * A command allowing the modification of custom commands by discord users.
 * 
 * @author Smc
 */
public class EditCustomCommand extends Command {

	public EditCustomCommand() {
		super(Permission.MESSAGE_MANAGE, false, false, CommandCategory.GENERAL, 
			  new String[]{"editcom", "editcmd"}, 
			  "Create, edit or delete custom commands.", 
			  "Allows creation, editing or deletion of custom commands in the server this " +
			  "command is used in.", 
			  new String[]{"editcom <command name> <instructions...>", 
					  	   "Adds or edits a command in the server under the command name given using " +
			  			   "the instructions given."},
			  new String[]{"editcom <command name>", "Deletes the command in the server."});
	}

	@Override
	public void onCommand(MessageReceivedEvent p_event, String[] args) {
		if(args.length == 0) {
			DiscordChatUtils.message(p_event.getChannel(), "Invalid arguments! Use **__" + 
									 DiscordChatUtils.getPrefix(p_event.getChannel()) + 
									 "help editcom__** for info on this command!");
			
			return;
		}
		
		String commandName = args[0];
		
		if(args.length == 1) { // delete
			if(new CustomCommand(commandName, p_event.getGuild().getId(), "").delete() > 0)
				DiscordChatUtils.message(p_event.getChannel(), "Command deleted!");
			else DiscordChatUtils.message(p_event.getChannel(), "This command already doesn't exist!");
		} else { // add/edit
			String instructions = "";
			
			for(int i = 1; i < args.length; i++)
				instructions += " " + args[i];
			
			if(new CustomCommand(commandName, p_event.getGuild().getId(), 
								 instructions.substring(1)).save())
				DiscordChatUtils.message(p_event.getChannel(), "Command saved!");
			else DiscordChatUtils.message(p_event.getChannel(), "There was an error saving the command!");
		}
	}
}
