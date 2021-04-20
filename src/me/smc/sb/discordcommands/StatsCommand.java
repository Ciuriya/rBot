package me.smc.sb.discordcommands;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import me.smc.sb.main.Main;
import me.smc.sb.perm.Permissions;
import me.smc.sb.tracking.HybridRegulator;
import me.smc.sb.tracking.OsuTrackRunnable;
import me.smc.sb.tracking.TrackedPlayer;
import me.smc.sb.utils.Configuration;
import me.smc.sb.utils.Utils;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

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
		int servers = Main.api.getGuilds().size();
		int users = 0, connected = 0;
		
		for(Guild guild : Main.api.getGuilds()){
			users += guild.getMembers().size();
			
			connected += getOnlineUsers(guild);
		}
		
		StringBuilder builder = new StringBuilder();
		
		long uptime = System.currentTimeMillis() - Main.bootTime;
		float averageRequestsPerMinute = Main.requestsSent == 0 ? 0 : (Main.requestsSent / ((float) uptime / 60000f));
		float averageHtmlScrapesPerMinute = Main.htmlScrapes == 0 ? 0 : (Main.htmlScrapes / ((float) uptime / 60000f));
		float averageOsuHtmlScrapesPerMinute = Main.osuHtmlScrapes == 0 ? 0 : (Main.osuHtmlScrapes / ((float) uptime / 60000f));
		int tracked = 0;
		int highestTrackAmount = 0;
		Guild highestTrackGuild = null;
		
		List<String> trackedPlayers = new ArrayList<>();
		
    	for(Guild guild : Main.api.getGuilds()){
    		Configuration sCfg = new Configuration(new File("Guilds/" + guild.getId() + ".txt"));
    		ArrayList<String> list = sCfg.getStringList("tracked-players");
    		
    		if(list.size() > highestTrackAmount){
    			highestTrackAmount = list.size();
    			highestTrackGuild = guild;
    		}
    		
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
			   .append("osu!api load: " + Utils.df(HybridRegulator.apiLoad * 100) + "% | osu!html load: " + Utils.df(HybridRegulator.htmlLoad * 100) + "%\n")
			   .append("osu!api requests: " + Main.requestsSent + " (" + averageRequestsPerMinute + " average/min, " + Main.highestBurstRequestsSent + " burst/min)\n")
			   .append("Queued osu!requests: " + HybridRegulator.requests.size() + " | Failed osu!requests: " + Main.failedRequests + "\n")
			   .append("Current osu!track refresh rate: " + OsuTrackCommand.currentRefreshRate + " seconds (" + 
					   (tracked + trackedPlayers.size()) + " tracked, " + TrackedPlayer.registeredPlayers.size() + " without duplicates)\n")
			   .append("osu!track first cycle players: " + (TrackedPlayer.getActivePlayers().size()) + "\n")
			   .append("osu!track second cycle players: " + TrackedPlayer.secondCyclePlayers.size() + "\n")
			   .append("Currently inactive tracked players: " + TrackedPlayer.inactivePlayers.size() + " players\n")
			   .append("Actual osu!track refresh rate per player: " + Utils.df((double) OsuTrackRunnable.totalTimeUsed / (double) OsuTrackRunnable.trackedTotal) + "ms\n")
			   .append("Request send time: " + Utils.df((double) HybridRegulator.timeExecutingRequests / (double) HybridRegulator.requestsSent) + "ms\n")
			   .append("Most tracked users in a single server: " + highestTrackAmount + " players (" + highestTrackGuild.getName() + ")\n")
			   .append("HTML pages scraped: " + Main.htmlScrapes + " (" + Main.osuHtmlScrapes + " from osu!)\n")
			   .append("HTML pages scraped/min average: " + averageHtmlScrapesPerMinute + " (" + averageOsuHtmlScrapesPerMinute + " for osu!)```");
		
		Utils.infoBypass(e.getChannel(), builder.toString());
	}

	public static int getOnlineUsers(Guild guild){
		int connected = 0;
		
		for(Member member : guild.getMembers()){
			OnlineStatus status = member.getOnlineStatus();
			
			if(status != OnlineStatus.OFFLINE && status != OnlineStatus.UNKNOWN)
				connected++;
		}
		
		return connected;
	}
	
}
