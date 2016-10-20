package me.smc.sb.discordcommands;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import me.smc.sb.main.Main;
import me.smc.sb.perm.Permissions;
import me.smc.sb.utils.Configuration;
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
			
			connected += getOnlineUsers(guild);
		}
		
		StringBuilder builder = new StringBuilder();
		
		long uptime = System.currentTimeMillis() - Main.bootTime;
		float averageRequestsPerMinute = Main.requestsSent == 0 ? 0 : (Main.requestsSent / ((float) uptime / 60000f));
		float averageHtmlScrapesPerMinute = Main.htmlScrapes == 0 ? 0 : (Main.htmlScrapes / ((float) uptime / 60000f));
		float averageOsuHtmlScrapesPerMinute = Main.osuHtmlScrapes == 0 ? 0 : (Main.osuHtmlScrapes / ((float) uptime / 60000f));
		int tracked = 0;
		List<String> trackedPlayers = new ArrayList<>();
		
    	for(Guild guild : Main.api.getGuilds()){
    		Configuration sCfg = new Configuration(new File("Guilds/" + guild.getId() + ".txt"));
    		ArrayList<String> list = sCfg.getStringList("tracked-players");
    		
    		for(String player : list){
    			if(trackedPlayers.contains(player))
    				tracked++;
    			else trackedPlayers.add(player);
    		}
    	}
		
		builder.append("```Connected to " + servers + " servers!\n") 
			   .append("There are " + users + " total users (" + connected + " connected) in those servers!\n")
			   .append("Uptime: " + Utils.toDuration(uptime) + "\n")
			   .append("Messages received: " + Main.messagesReceivedThisSession + "\n")
			   .append("Messages sent: " + Main.messagesSentThisSession + "\n")
			   .append("Commands used: " + Main.commandsUsedThisSession + "\n")
			   .append("osu!api requests: " + Main.requestsSent + " (" + averageRequestsPerMinute + " average/min, " + Main.highestBurstRequestsSent + " burst/min)\n")
			   .append("Current osu!track refresh rate: " + OsuTrackCommand.currentRefreshRate + " seconds (" + 
					   (tracked + trackedPlayers.size()) + " tracked, " + trackedPlayers.size() + " without duplicates)\n")
			   .append("HTML pages scraped: " + Main.htmlScrapes + " (" + Main.osuHtmlScrapes + " from osu!)\n")
			   .append("HTML pages scraped/min average: " + averageHtmlScrapesPerMinute + " (" + averageOsuHtmlScrapesPerMinute + " for osu!)```");
		
		Utils.infoBypass(e.getChannel(), builder.toString());
	}

	public static int getOnlineUsers(Guild guild){
		int connected = 0;
		
		for(User user : guild.getUsers()){
			OnlineStatus status = user.getOnlineStatus();
			if(status == OnlineStatus.ONLINE || status == OnlineStatus.AWAY)
				connected++;
		}
		
		return connected;
	}
	
}
