package me.smc.sb.discordcommands;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import me.smc.sb.perm.Permissions;
import me.smc.sb.utils.Utils;
import net.dv8tion.jda.Permission;
import net.dv8tion.jda.entities.Guild;
import net.dv8tion.jda.entities.Message;
import net.dv8tion.jda.entities.TextChannel;
import net.dv8tion.jda.events.message.MessageReceivedEvent;

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
		Utils.deleteMessage(e.getChannel(), e.getMessage());
		
		StringBuilder builder = new StringBuilder();
		
		builder.append("```" + e.getJDA().getGuilds().size() + " servers\n\n");
		
		List<String> guildPosts = new ArrayList<>();
		
		for(Guild guild : e.getJDA().getGuilds()){
			long lastMessageDate = 0;
			TextChannel lastMessageChannel = guild.getPublicChannel();
			
			for(TextChannel channel : guild.getTextChannels()){
				if(!channel.checkPermission(e.getJDA().getSelfInfo(), Permission.MESSAGE_HISTORY))
					continue;
				
				List<Message> messages = channel.getHistory().retrieve(1);
				
				if(messages != null && messages.size() > 0){
					Message msg = messages.get(0);
					
					if(msg.getTime().toEpochSecond() < lastMessageDate || lastMessageDate == 0){
						lastMessageDate = msg.getTime().toEpochSecond();
						lastMessageChannel = channel;
					}
				}
			}
			
			Calendar c = Calendar.getInstance();
			c.setTimeInMillis(lastMessageDate);
			
			builder.append("- " + guild.getName().replaceAll("\\n", "") + " (" + guild.getUsers().size() + " users)\n")
				   .append("  " + guild.getId() + " | " + guild.getTextChannels().size() + " channels | " + 
						   guild.getVoiceChannels().size() + " voice chats\n")
				   .append("  Last message posted in " + lastMessageChannel.getName() + "(" + lastMessageChannel.getId() + ") " +
						   "at " + c.getTime().toString() + "\n")
				   .append("  Owned by " + guild.getOwner().getUsername() + "#" + guild.getOwner().getDiscriminator() + 
						   " (" + guild.getOwnerId() + ")\n\n");
			
			guildPosts.add(builder.toString());
			builder = new StringBuilder();
		}
		
		List<String> posts = new ArrayList<>();
		
		String post = "";
		
		for(String guildPost : guildPosts){
			if(post.length() + guildPost.length() > 1995){
				posts.add(post + "```");
				post = "";
			}
			
			post += guildPost;
		}
		
		posts.add(post + "```");
		
		for(String text : posts)
			Utils.infoBypass(e.getChannel(), text);
	}
	
}
