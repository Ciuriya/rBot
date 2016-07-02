package me.smc.sb.discordcommands;

import me.smc.sb.main.Main;
import me.smc.sb.perm.Permissions;
import me.smc.sb.utils.Utils;
import net.dv8tion.jda.OnlineStatus;
import net.dv8tion.jda.entities.Guild;
import net.dv8tion.jda.entities.User;
import net.dv8tion.jda.events.message.MessageReceivedEvent;

public class StatsCommand extends GlobalCommand{

	public StatsCommand(){
		super(Permissions.BOT_ADMIN, 
			  " - Shows useful bot stats such as uptime", 
			  "{prefix}stats\nThis command displays various bot related stats.\n\n" +
			  "----------\nUsage\n----------\n{prefix}stats - Lays out bot related stats\n\n" + 
			  "----------\nAliases\n----------\nThere are no aliases.", 
			  true, 
			  "stats");
	}

	@Override
	public void onCommand(MessageReceivedEvent e, String[] args){
		Utils.deleteMessage(e.getChannel(), e.getMessage());
		
		int servers = Main.api.getGuilds().size();
		int users = 0, connected = 0;
		
		for(Guild guild : Main.api.getGuilds()){
			users += guild.getUsers().size();
			
			for(User user : guild.getUsers()){
				OnlineStatus status = user.getOnlineStatus();
				if(status == OnlineStatus.ONLINE || status == OnlineStatus.AWAY)
					connected++;
			}
		}
		
		StringBuilder builder = new StringBuilder();
		
		long uptime = System.currentTimeMillis() - Main.bootTime;
		builder.append("```Connected to " + servers + " servers!\n") 
			   .append("There are " + users + " total users (" + connected + " connected) in those servers!\n")
			   .append("Uptime: " + Utils.toDuration(uptime) + "\n")
			   .append("Messages received since startup: " + Main.messagesReceivedThisSession + "\n")
			   .append("Messages sent since startup: " + Main.messagesSentThisSession + "\n")
			   .append("Commands used since startup: " + Main.commandsUsedThisSession + "```");
		
		Utils.infoBypass(e.getChannel(), builder.toString());
	}

}
