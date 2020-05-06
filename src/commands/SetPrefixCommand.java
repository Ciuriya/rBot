package commands;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.logging.Level;

import data.CommandCategory;
import data.Log;
import managers.DatabaseManager;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import utils.Constants;
import utils.DiscordChatUtils;

/**
 * A command allowing the modification of the server's bot prefix.
 * 
 * @author Smc
 */
public class SetPrefixCommand extends Command {
	
	public SetPrefixCommand() {
		super(Permission.MANAGE_SERVER, false, false, CommandCategory.ADMIN,
			  new String[]{"prefix", "setprefix"},
			  "Change the bot's prefix for this server.",
			  "Allows server managers to change the bot's prefix to a different one.\n" +
			  "The default prefix will always work.",
			  new String[]{"prefix <new prefix>", "Changes the server's prefix to the new prefix."});
	}

	@Override
	public void onCommand(MessageReceivedEvent p_event, String[] args) {
		if(args.length == 0) {
			sendInvalidArgumentsError(p_event.getChannel());
			return;
		}
		
		String currentPrefix = DiscordChatUtils.getPrefix(p_event.getChannel());
		
		if(currentPrefix.equalsIgnoreCase(args[0])) {
			DiscordChatUtils.message(p_event.getChannel(), "The current prefix is already set to **__" + args[0] + "__**");
			return;
		}
		
		try(Connection conn = DatabaseManager.getInstance().get("discord").getConnection()) {
			PreparedStatement st = conn.prepareStatement(
								   "INSERT INTO `guild-settings` (`guild-id`, `key`, `field`) " +
								   "VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE `field`=?");
			
			st.setString(1, p_event.getGuild().getId());
			st.setString(2, "prefix");
			st.setString(3, args[0].toLowerCase());
			st.setString(4, args[0].toLowerCase());
			
			st.executeUpdate();
			st.close();
			
			DiscordChatUtils.message(p_event.getChannel(), "The current prefix is now **__" + args[0] + "__**");
		} catch(Exception e) {
			Log.log(Level.SEVERE, "Could not set prefix to " + args[0].toLowerCase() + " for guild " + p_event.getGuild().getId(), e);
			DiscordChatUtils.message(p_event.getChannel(), "There was an error while changing the prefix!\n" + 
														   "Please try again later or contact the developer via the **__report__** command or the support server!\n<" +
														   Constants.SUPPORT_SERVER_LINK + ">"); // TODO: look into standardizing this?
		}
	}
}
