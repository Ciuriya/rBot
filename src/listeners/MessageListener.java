package listeners;

import commands.Command;
import managers.ApplicationStats;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import utils.Constants;
import utils.DiscordChatUtils;

/**
 * This listener listens to all message events sent to the bot
 * and redirects them appropriately.
 * 
 * @author Smc
 */
public class MessageListener extends ListenerAdapter {

	@Override
	public void onMessageReceived(MessageReceivedEvent p_event) {
		if(p_event.getAuthor().isBot()) return;
		
		ApplicationStats.getInstance().addMessageReceived();
		
		String message = p_event.getMessage().getContentRaw();
		
		// if we have a prefix, strip it, otherwise it's not a command (unless it's a dm)
		if(message.startsWith(Constants.DEFAULT_PREFIX))
			message = message.substring(Constants.DEFAULT_PREFIX.length());
		else if(p_event.isFromGuild()) {
			String prefix = DiscordChatUtils.getPrefix(p_event.getChannel());
			
			if(message.startsWith(prefix)) message = message.substring(prefix.length());
			else return;
		}
		
		// find and run global command, if we couldn't find/run it, we look for a custom command
		if(!Command.handleCommand(p_event, message)) {
			
		}
	}
}
