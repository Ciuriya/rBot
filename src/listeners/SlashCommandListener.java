package listeners;

import java.util.logging.Level;

import commands.Command;
import commands.CustomCommand;
import data.Log;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import utils.DiscordChatUtils;

/**
 * This listener listens to all slash commands being used and ensures they are executed properly.
 * 
 * @author Smc
 */
public class SlashCommandListener extends ListenerAdapter {

	@Override
	public void onSlashCommand(SlashCommandEvent p_event) {
		if(p_event.getUser().isBot()) return;
		
		User author = p_event.getUser();
		String commandString = p_event.getCommandString();
		String trigger = commandString.split(" ")[0].replace("/", "");
		
		Log.log(Level.INFO, "{Command received in " + DiscordChatUtils.getChannelLogString(p_event.getChannel()) + 
							" sent by " + author.getId() + "}\n" + commandString);
		
		boolean commandSuccess = Command.handleCommand(p_event, trigger, p_event.getOptions());
		
		// find and run global command, if we couldn't find/run it, we look for a custom command
		if(!commandSuccess && p_event.isFromGuild()) {
			// TODO: register custom commands
			// TODO: reply to custom commands
			CustomCommand cmd = CustomCommand.getCommand(p_event.getGuild().getId(), trigger);
			
			if(cmd != null) cmd.execute(p_event);
			else DiscordChatUtils.message(p_event, "No custom commands matching this trigger were found!", false, false);
		} else if(!commandSuccess && !p_event.isFromGuild()){
			DiscordChatUtils.message(p_event, "The command you are trying to use cannot be used in the current channel!", false, false);
		}
	}
}
