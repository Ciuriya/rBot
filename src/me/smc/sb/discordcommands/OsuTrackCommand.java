package me.smc.sb.discordcommands;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Timer;

import me.smc.sb.main.Main;
import me.smc.sb.tracking.OsuTrackRunnable;
import me.smc.sb.tracking.TrackedPlayer;
import me.smc.sb.tracking.TrackingGuild;
import me.smc.sb.tracking.TrackingUtils;
import me.smc.sb.utils.Configuration;
import me.smc.sb.utils.Utils;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

public class OsuTrackCommand extends GlobalCommand{

	private static int REQUESTS_PER_MINUTE = 250;
	private static Timer trackingTimer = null;
	public static double currentRefreshRate = 0;
	
	public OsuTrackCommand(){
		super(null, 
			  " - Lets you track osu! players", 
			  "{prefix}osutrack\nThis command lets you track osu! players' recent performance\n\n" +
		      "----------\nUsage\n----------\n{prefix}osutrack {player} ({mode={0/1/2/3}}) - Tracks or untracks the player's recent statistics for this mode\n" + 
			  "{prefix}osutrack {player} ({ext}) - Tracks or untracks the player in a separate channel\n" +
		      "{prefix}osutrack list - Lists all players currently tracked in the server\n" +
		      "{prefix}osutrack ({best}) - Tracks only players' best plays in this server\n" +
			  "{prefix}osutrack ({pp=XXX}) - Tracks every play above XXX pp\n\n" +
		      "----------\nAliases\n----------\nThere are no aliases.", 
			  false, 
			  "osutrack");
		
		load();
	}
	
	@Override
	public void onCommand(MessageReceivedEvent e, String[] args){
		Utils.deleteMessage(e.getChannel(), e.getMessage());
		if(!Utils.checkArguments(e, args, 1)) return;
		
		String user = "";
		int mode = 0;
		boolean ext = false;
		boolean scanServer = false;
		boolean changeToBestPlayTracking = false;
		boolean ppCap = false;
		boolean listing = false;
		int ppAmount = 0;
		
		for(int i = 0; i < args.length; i++)
			if(args[i].contains("{mode="))
				mode = Utils.stringToInt(args[i].split("\\{mode=")[1].split("}")[0]);
			else if(args[i].contains("{ext}")) ext = true;
			else if(args[i].contains("{scan}")) scanServer = true;
			else if(args[i].contains("{best}")) changeToBestPlayTracking = true;
			else if(args[i].equalsIgnoreCase("list") && i == 0) listing = true;
			else if(args[i].contains("{pp=")){
				ppCap = true;
				ppAmount = Utils.stringToInt(args[i].split("pp=")[1].split("}")[0]);
			}else user += " " + args[i];
		
		TrackingGuild guild = TrackingGuild.get(e.getGuild().getId());
		
		if(guild == null) guild = new TrackingGuild(e.getGuild().getId());
		
		if(listing){
			Configuration sCfg = Main.serverConfigs.get(e.getGuild().getId());
			String trackingId = sCfg.getValue("track-update-group");
			String info = "```diff\n+ Tracked users in " + e.getGuild().getName() + "\n" +
					  	  "+ Track Channel - #" + e.getJDA().getTextChannelById(trackingId).getName() + " (" + trackingId + ")\n";
			List<TrackedPlayer> playerList = new ArrayList<>(TrackedPlayer.registeredPlayers);
    		
			Collections.sort(playerList, new Comparator<TrackedPlayer>(){
				@Override
				public int compare(TrackedPlayer o1, TrackedPlayer o2){
					int result = o1.getUsername().compareTo(o2.getUsername());
					
					if(result == 0){
						if(o1.getMode() < o2.getMode()) return 1;
						else if(o1.getMode() > o2.getMode()) return -1;
						else return 0;
					}else return result;
				}
			});
			
    		for(TrackedPlayer player : playerList){
    			if(player.isTracked(guild)){
        			info += "\n- " + player.getUsername() + " | " + TrackingUtils.convertMode(player.getMode());
        			
        			if(guild.getChannel(player) != null){
        				TextChannel channel = guild.getChannel(player);
        				
        				if(!channel.getId().equalsIgnoreCase(trackingId))
        					info += " | #" + channel.getName();
        			}
        			
        			if(info.length() > 1900){
        				Utils.info(e.getChannel(), info + "```");
        				
        				info = "```diff";
        			}
    			}
    		}
    		
    		if(info.length() > 0) Utils.info(e.getChannel(), info + "```");
    		
			return;
		}
		
		if(ppCap){
			guild.getConfig().writeValue("track-pp-minimum", ppAmount);
			
			Utils.info(e.getChannel(), "Only plays above " + ppAmount + "pp will be tracked!");
			return;
		}
		
		if(changeToBestPlayTracking){
			boolean wasTracking = false;
			
			if(guild.getConfig().getBoolean("track-best-only"))
				wasTracking = true;
			
			guild.getConfig().writeValue("track-best-only", !wasTracking);
			
			Utils.info(e.getChannel(), !wasTracking ? "Only personal bests will be tracked!" :
									   "All plays (except failed plays) will be tracked!");
			return;
		}
		
		if(scanServer){
			String trackingId = Main.serverConfigs.get(e.getGuild().getId()).getValue("track-update-group");
			String info = "```Stored info for server " + e.getGuild().getName() + "\n" +
						  "Tracking channel: " + e.getJDA().getTextChannelById(trackingId).getAsMention() + " (" + trackingId + ")\n" +
						  "Tracked users";
			
			Configuration sCfg = guild.getConfig();
    		ArrayList<String> list = sCfg.getStringList("tracked-players");
			
    		for(String pl : list)
    			info += "\n- " + pl;
    		
			Utils.info(e.getChannel(), info + "```");
			
			return;
		}
		
		user = user.substring(1);
		
		TrackedPlayer player = TrackedPlayer.get(user, mode);
		
		if(player != null && player.isTracked(guild)){
			boolean untracked = guild.untrack(player.getUsername(), mode);
			
			guild.setChannel(e.getTextChannel().getId());
			
			if(untracked)
				Utils.info(e.getChannel(), "Stopped tracking " + player.getUsername() + " in the " + TrackingUtils.convertMode(mode) + " mode!" +
						   				   "\nA full refresh cycle now takes " + currentRefreshRate + " seconds!");
			
			return;
		}
		
		boolean tracked = guild.track(user, mode, true);
		
		player = TrackedPlayer.get(user, mode);
		
		if(player != null && ext && tracked) guild.setPlayerChannel(player.getUserId(), mode, e.getTextChannel().getId());
		if(!ext) guild.setChannel(e.getTextChannel().getId());
		
		if(tracked)
			Utils.info(e.getChannel(), "Started tracking " + user + " in the " + TrackingUtils.convertMode(mode) + " mode!" +
									   "\nA full refresh cycle now takes " + currentRefreshRate + " seconds!");
	}
	
	public void load(){
		new Thread(new Runnable(){
			public void run(){
				for(Guild guild : Main.api.getGuilds())
					new TrackingGuild(guild.getId());
				
				if(!Main.discordConnected)
					Utils.infoBypass(Main.api.getUserById("91302128328392704").openPrivateChannel().complete(), "Tracking now waiting for discord to connect...");
				else
					Utils.infoBypass(Main.api.getUserById("91302128328392704").openPrivateChannel().complete(), "Tracking started!");
				
				while(!Main.discordConnected){
					Utils.sleep(100);
				}
				
				TrackedPlayer.changeOccured = false;
				startTracker();
			}
		}).start();
	}
	
	public void startTracker(){
		double refreshRate = calculateRefreshRate();
		double delay = Math.abs(refreshRate / (double) TrackedPlayer.registeredPlayers.size());
		
		if(trackingTimer != null){
			trackingTimer.cancel();
			trackingTimer = null;
		}
		
		trackingTimer = new Timer();
		trackingTimer.scheduleAtFixedRate(new OsuTrackRunnable(this), (long) 0, (long) (delay * 1000));
	}
	
	private double calculateRefreshRate(){
		for(double i = 5; ; i += 0.25)
			if(calculateRequestsPerMinute(i) <= REQUESTS_PER_MINUTE){
				currentRefreshRate = i;
				
				return i;
			}
	}
	
	private double calculateRequestsPerMinute(double refreshRate){
		int users = TrackedPlayer.registeredPlayers.size();
		
		return 60.0 / refreshRate * (double) users;
	}
	
}
