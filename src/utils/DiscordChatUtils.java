package utils;

import java.util.logging.Level;

import data.Log;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.PrivateChannel;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.ThreadChannel;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;

/**
 * This class contains utility functions to handle discord messaging.
 * 
 * @author Smc
 */
public class DiscordChatUtils {

	public static void message(MessageChannel p_channel, String p_message) {
		if(!checkMessagePermissionForChannel(p_channel)) return;
		
		// cut the message into parts discord can send (<4000 characters per message)
		// 3960 gives us some leeway just in case
		for(int i = 0; i < (int) Math.ceil(p_message.length() / 3960f); i++) {
			String cutMessage = p_message;
			
			if(i > 0) {
				if((i + 1) * 1960 > cutMessage.length())
					cutMessage = cutMessage.substring(i * 3960);
				else cutMessage = cutMessage.substring(i * 3960, (i + 1) * 3960);
			}
			
			final String part = cutMessage;
			
			p_channel.sendMessage(part).queue(
					(message) -> 
						{
							Log.log(Level.INFO, "{Message sent in " + getChannelLogString(p_channel) + "} " + part);
						},
					(error) -> Log.log(Level.WARNING, "Could not send message", error));
		}
	}
	
	public static void message(SlashCommandEvent p_event, String p_message, boolean p_bypassPermissionCheck, boolean p_isEphemeral, ActionRow... p_actionRows) {
		if(!p_bypassPermissionCheck && !checkMessagePermissionForChannel(p_event.getMessageChannel())) {
			sendMessagePermissionCheckFailedMessage(p_event);
			return;
		}
		
		p_event.reply(p_message).addActionRows(p_actionRows).setEphemeral(p_isEphemeral).queue(
				(message) -> 
					{
						// TODO: sanitize for customs
						Log.log(Level.INFO, "{Reply sent in " + getChannelLogString(p_event.getChannel()) + "} " + p_message);
					},
				(error) -> Log.log(Level.WARNING, "Could not send reply", error));
	}
	
	public static void embed(MessageChannel p_channel, MessageEmbed p_embed) {
		if(!checkMessagePermissionForChannel(p_channel)) return;
		
		p_channel.sendMessageEmbeds(p_embed).queue(
				(message) -> 
					{
						Log.log(Level.INFO, "{Embed sent in " + getChannelLogString(p_channel) + "} " + 
																p_embed.getAuthor().getName() + 
																(p_embed.getTitle() != null ? "\n" + 
																p_embed.getTitle() : ""));
					},
				(error) -> Log.log(Level.WARNING, "Could not send embed", error));
	}
	
	public static void embed(SlashCommandEvent p_event, MessageEmbed p_embed, boolean p_bypassPermissionCheck, boolean p_isEphemeral, ActionRow... p_actionRows) {
		if(!p_bypassPermissionCheck && !checkMessagePermissionForChannel(p_event.getMessageChannel())) {
			sendMessagePermissionCheckFailedMessage(p_event);
			return;
		}
		
		p_event.replyEmbeds(p_embed).addActionRows(p_actionRows).setEphemeral(p_isEphemeral).queue(
				(message) -> 
					{
						// TODO: sanitize for customs
						Log.log(Level.INFO, "{Embed reply sent in " + getChannelLogString(p_event.getChannel()) + "} " + 
											 p_embed.getAuthor().getName() + 
											 (p_embed.getTitle() != null ? "\n" + 
											 p_embed.getTitle() : ""));
					},
				(error) -> Log.log(Level.WARNING, "Could not send reply", error));
	}
	
	public static void sendMessagePermissionCheckFailedMessage(SlashCommandEvent p_event) {
		p_event.reply("This bot is not allowed to interact with this channel. Contact a server moderator if you believe this to be an error!").setEphemeral(true).queue();
	}
	
	public static String getChannelLogString(MessageChannel p_channel) {
		if(p_channel == null) return "Unknown";
		if(p_channel.getType() == ChannelType.PRIVATE)
			return "Private/" + ((PrivateChannel) p_channel).getUser().getId();

		return getGuildIdFromChannel(p_channel) + "#" + p_channel.getId();
	}
	
	public static String getGuildIdFromChannel(MessageChannel p_channel) {
		if(p_channel instanceof TextChannel)
			return ((TextChannel) p_channel).getGuild().getId();
		else if(p_channel instanceof ThreadChannel)
			return ((ThreadChannel) p_channel).getGuild().getId();
		
		return "";
	}
	
	public static boolean checkMessagePermissionForChannel(MessageChannel p_channel) {
		if(p_channel instanceof TextChannel) {
			TextChannel textChannel = (TextChannel) p_channel;
			
			return textChannel.getGuild().getSelfMember().hasPermission(textChannel, Permission.MESSAGE_SEND);
		} else if(p_channel instanceof ThreadChannel) {
			ThreadChannel threadChannel = (ThreadChannel) p_channel;
			
			return threadChannel.getGuild().getSelfMember().hasPermission(threadChannel, Permission.MESSAGE_SEND_IN_THREADS);
		}
		
		return true;
	}
}
