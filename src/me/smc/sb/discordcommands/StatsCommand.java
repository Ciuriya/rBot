package me.smc.sb.discordcommands;

import me.itsghost.jdiscord.OnlineStatus;
import me.itsghost.jdiscord.Server;
import me.itsghost.jdiscord.events.UserChatEvent;
import me.itsghost.jdiscord.message.MessageBuilder;
import me.itsghost.jdiscord.talkable.GroupUser;
import me.smc.sb.main.Main;
import me.smc.sb.perm.Permissions;
import me.smc.sb.utils.Utils;

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
	public void onCommand(UserChatEvent e, String[] args){
		e.getMsg().deleteMessage();
		int servers = Main.api.getAvailableServers().size();
		int users = 0, connected = 0;
		for(Server s : Main.api.getAvailableServers()){
			users += s.getConnectedClients().size();
			for(GroupUser user : s.getConnectedClients()){
				OnlineStatus status = user.getUser().getOnlineStatus();
				if(status == OnlineStatus.ONLINE || status == OnlineStatus.AWAY)
					connected += 1;
			}
		}
		MessageBuilder builder = new MessageBuilder();
		long uptime = System.currentTimeMillis() - Main.bootTime;
		builder.addString("```Connected to " + servers + " servers!\n") 
			   .addString("There are " + users + " total users (" + connected + " connected) in those servers!\n")
			   .addString("Uptime: " + Utils.toDuration(uptime) + "\n")
			   .addString("Messages received since startup: " + Main.messagesReceivedThisSession + "\n")
			   .addString("Messages sent since startup: " + Main.messagesSentThisSession + "\n")
			   .addString("Commands used since startup: " + Main.commandsUsedThisSession + "```");
		Utils.infoBypass(e.getGroup(), builder.build(Main.api).getMessage());
	}

}
