package me.smc.sb.discordcommands;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import me.smc.sb.perm.Permissions;
import me.smc.sb.utils.Utils;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class ListServersCommand extends GlobalCommand{

	public ListServersCommand(){
		super(Permissions.BOT_ADMIN, 
			  " - Lists all servers where the bot is in", 
			  "{prefix}listservers\nThis command shows all connected servers and some general info.\n\n" +
			  "----------\nUsage\n----------\n{prefix}listservers - Lays out all servers\n\n" + 
			  "----------\nAliases\n----------\nThere are no aliases.", 
			  true, 
			  "listservers");
	}

	@Override
	public void onCommand(MessageReceivedEvent e, String[] args){
		StringBuilder builder = new StringBuilder();
		
		builder.append("```" + e.getJDA().getGuilds().size() + " servers\n\n");
		
		List<String> guildPosts = new ArrayList<>();
		
		DateTimeFormatter dtf = DateTimeFormatter.ISO_DATE_TIME;
		
		for(Guild guild : e.getJDA().getGuilds()){
			OffsetDateTime lastMessageDate = null;
			TextChannel lastMessageChannel = guild.getDefaultChannel();
			User poster = null;
			
			for(TextChannel channel : guild.getTextChannels()){
				Member member = channel.getMembers().stream().filter(m -> m.getUser().getId().equals(e.getJDA().getSelfUser().getId())).findFirst().orElse(null);
				
				if(member != null)
					if(!member.hasPermission(channel, Permission.MESSAGE_HISTORY))
						continue;
				
				List<Message> messages = null;
				
				try{
					messages = channel.getHistory().retrievePast(1).complete();
				}catch(Exception ex){
					continue;
				}
				
				if(messages != null && messages.size() > 0){
					Message msg = messages.get(0);
					OffsetDateTime creationTime = msg.getTimeCreated();
					
					if(lastMessageDate == null || creationTime.isAfter(lastMessageDate)){
						lastMessageDate = creationTime;
						lastMessageChannel = channel;
						poster = msg.getAuthor();
					}
				}
				
				Utils.sleep(300);
			}
			
			String date = "**unavailable**";
			
			if(lastMessageDate != null) 
				date = lastMessageDate.format(dtf);
			
			builder.append("- " + guild.getName().replaceAll("\\n", "") + " (" + guild.getMembers().size() + " users, " + 
						   StatsCommand.getOnlineUsers(guild) + " online)\n")
				   .append("  Image URL: " + guild.getIconUrl() + "\n")
				   .append("  " + guild.getId() + " | " + guild.getTextChannels().size() + " channels | " + 
						   guild.getVoiceChannels().size() + " voice chats\n")
				   .append("  Last message posted in " + lastMessageChannel.getName() + "(" + lastMessageChannel.getId() + ") " +
						   "at " + date + (poster == null ? "" : " by " + poster.getName() + "#" + poster.getDiscriminator()) + "\n")
				   .append("  Owned by " + guild.getOwner().getUser().getName() + "#" + guild.getOwner().getUser().getDiscriminator() + 
						   " (" + guild.getOwner().getUser().getId() + ")\n\n");
			
			guildPosts.add(builder.toString());
			builder = new StringBuilder();
		}
		
		List<String> posts = new ArrayList<>();
		
		String post = "";
		
		for(String guildPost : guildPosts){
			if(post.length() + guildPost.length() > 1995){
				posts.add(post + "```");
				post = "```";
			}
			
			post += guildPost;
		}
		
		posts.add(post + "```");
		
		for(String text : posts)
			Utils.infoBypass(e.getChannel(), text);
	}
	
}
