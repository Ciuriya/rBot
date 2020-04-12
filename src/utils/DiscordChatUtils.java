package utils;

import java.util.logging.Level;

import data.Log;
import managers.ApplicationStats;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.PrivateChannel;
import net.dv8tion.jda.api.entities.TextChannel;

/**
 * This class contains utility functions to handle discord messaging.
 * 
 * @author Smc
 */
public class DiscordChatUtils {

	public static void message(MessageChannel p_channel, String p_message) {
		// cut the message into parts discord can send (<2000 characters per message)
		// 1960 gives us some leeway just in case
		for(int i = 0; i < (int) Math.ceil(p_message.length() / 1960f); i++) {
			String cutMessage = p_message;
			
			if(i > 0) {
				if((i + 1) * 1960 > cutMessage.length())
					cutMessage = cutMessage.substring(i * 1960);
				else cutMessage = cutMessage.substring(i * 1960, (i + 1) * 1960);
			}
			
			final String part = cutMessage;
			
			p_channel.sendMessage(part).queue(
					(message) -> Log.log(Level.INFO, "{Message sent in " + 
													 getChannelLogString(p_channel) + "} " + 
													 part),
					(error) -> Log.log(Level.WARNING, "Could not send message", error));
	
			ApplicationStats.getInstance().addMessageSent();
		}
	}
	
	public static void embed(MessageChannel p_channel, MessageEmbed p_embed) {
		p_channel.sendMessage(p_embed).queue(
				(message) -> Log.log(Level.INFO, "{Embed sent in " + 
												 getChannelLogString(p_channel) + "} " + 
												 p_embed.getAuthor().getName() + 
												 (p_embed.getTitle() != null ? "\n" + 
												 p_embed.getTitle() : "")),
				(error) -> Log.log(Level.WARNING, "Could not send embed", error));

		ApplicationStats.getInstance().addMessageSent();
	}
	
	public static String getPrefix(MessageChannel p_channel) {
		if(p_channel.getType() == ChannelType.PRIVATE) return Constants.DEFAULT_PREFIX;
		else {
			TextChannel text = (TextChannel) p_channel;
			String prefix = SQLUtils.getGuildSetting(text.getGuild().getId(), "prefix", text.getId());
		
			return prefix.length() > 0 ? prefix : Constants.DEFAULT_PREFIX;
		}
	}
	
	private static String getChannelLogString(MessageChannel p_channel) {
		if(p_channel.getType() == ChannelType.PRIVATE)
			return "Private/" + ((PrivateChannel) p_channel).getUser().getId();
		
		TextChannel text = (TextChannel) p_channel;
		
		return text.getGuild().getName() + "#" + text.getName();
	}
}
