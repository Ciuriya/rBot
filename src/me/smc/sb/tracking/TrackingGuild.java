package me.smc.sb.tracking;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import me.smc.sb.main.Main;
import me.smc.sb.utils.Configuration;
import me.smc.sb.utils.Utils;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;

public class TrackingGuild{

	private String guildId;
	private MessageChannelUnion trackUpdateChannel;
	private MessageChannelUnion leaderboardChannel;
	private Map<String, MessageChannelUnion> specificUpdateChannels;
	private PlayFormat playFormat;
	public static List<TrackingGuild> registeredGuilds = new ArrayList<>();
	
	public TrackingGuild(String guildId){
		this.guildId = guildId;
		specificUpdateChannels = new HashMap<>();
		
		load();
		registeredGuilds.add(this);
	}
	
	public static TrackingGuild get(String guildId){
		if(!registeredGuilds.isEmpty())
			for(TrackingGuild guild : registeredGuilds)
				if(guild.getGuildId().equals(guildId))
					return guild;
			
		return null;
	}
	
	public String getGuildId(){
		return guildId;
	}
	
	public Configuration getConfig(){
		if(Main.serverConfigs.containsKey(guildId)){
			return Main.serverConfigs.get(guildId);
		}
		
		return new Configuration(new File("Guilds/" + guildId + ".txt"));
	}
	
	public void setChannel(String channelId){
		getConfig().writeValue("track-update-group", channelId);
		trackUpdateChannel = Main.api.getChannelById(MessageChannelUnion.class, channelId);
	}
	
	public void setLeaderboardChannel(String channelId){
		getConfig().writeValue("leaderboard-group", channelId);
		leaderboardChannel = Main.api.getChannelById(MessageChannelUnion.class, channelId);
	}
	
	public void setPlayerChannel(int userId, int mode, String channelId){
		getConfig().writeValue(userId + "&m=" + mode + "-update-group", channelId);
		specificUpdateChannels.put(userId + "&m=" + mode, Main.api.getChannelById(MessageChannelUnion.class, channelId));
	}
	
	public void setPlayerInfo(TrackedPlayer player){
		Configuration config = getConfig();
		String toSave = Utils.df(player.getPP()) + "&r=" + player.getRank() + "&cr=" + player.getCountryRank();
		
		toSave += "&u=" + player.getUsername();
		toSave += "&a=" + player.getLastActive().getDate();
		toSave += "&d=" + player.getLastUpdate().getDate();
		
		config.writeValue(player.getUserId() + "&m=" + player.getMode() + "-info", toSave);
	}
	
	public boolean onlyTrackingBestPlays(){
		return getConfig().getBoolean("track-best-only");
	}
	
	public int getMinimumPPAmount(){
		return getConfig().getInt("track-pp-minimum");
	}
	
	public PlayFormat getPlayFormat(){
		return playFormat;
	}
	
	public void setPlayFormat(String name){
		getConfig().writeValue("track-play-format", name);
		playFormat = PlayFormat.get(name);
	}
	
	public MessageChannelUnion getChannel(TrackedPlayer player){
		if(specificUpdateChannels.containsKey(player.getUserId() + "&m=" + player.getMode()))
			return specificUpdateChannels.get(player.getUserId() + "&m=" + player.getMode());
		
		return trackUpdateChannel == null ? (MessageChannelUnion) Main.api.getGuildById(guildId).getDefaultChannel().asTextChannel() : trackUpdateChannel;
	}
	
	public MessageChannelUnion getLeaderboardChannel(){
		return leaderboardChannel == null ? (MessageChannelUnion) Main.api.getGuildById(guildId).getDefaultChannel().asTextChannel() : leaderboardChannel;
	}
	
	public boolean track(String username, int mode, boolean addToFile, boolean leaderboard){
		Configuration config = getConfig();
		
		boolean success = false;
		TrackedPlayer registered = TrackedPlayer.get(username, mode);
		
		if(registered != null && !registered.isTracked(this, leaderboard)){
			if(leaderboard) registered.setLeaderboardTrack(true);
			else registered.setNormalTrack(true);
			
			success = registered.trackPlayer(this, leaderboard);
		}else{
			int userId = Utils.stringToInt(Utils.getOsuPlayerId(username, true));
			
			if(userId == -1){
				Utils.info(leaderboard ? getLeaderboardChannel() : trackUpdateChannel, "Could not fetch userId from " + username + "! Please try again later. (osu! error)");
				return false;
			}
			
			registered = new TrackedPlayer(username, userId, mode);
			
			if(leaderboard) registered.setLeaderboardTrack(true);
			else registered.setNormalTrack(true);
			
			success = registered.trackPlayer(this, leaderboard);
		}
		
		if(success && addToFile){
			String tracked = registered.getUserId() + "&m=" + mode;
			
			if(leaderboard && !config.getStringList("leaderboard-tracked-players").contains(tracked))
				config.appendToStringList("leaderboard-tracked-players", tracked, true);
			else if(!leaderboard && !config.getStringList("tracked-players").contains(tracked))
				config.appendToStringList("tracked-players", tracked, true);
		}
		
		return success;
	}
	
	public boolean untrack(String username, int mode, boolean leaderboard){
		Configuration config = getConfig();
		
		TrackedPlayer registered = TrackedPlayer.get(username, mode);
		
		if(registered == null) return false;
		
		if(registered.isTracked(this, leaderboard)){
			if(leaderboard)
				config.removeFromStringList("leaderboard-tracked-players", registered.getUserId() + "&m=" + mode, true);
			else
				config.removeFromStringList("tracked-players", registered.getUserId() + "&m=" + mode, true);
			
			
			registered.untrackPlayer(this, leaderboard);
			
			return true;
		}
		
		return false;
	}
	
	public void load(){
		Configuration config = getConfig();
		ArrayList<String> trackedList = config.getStringList("tracked-players");
		ArrayList<String> leaderboardTrackedList = config.getStringList("leaderboard-tracked-players");
		
		if(config.getValue("track-update-group").length() == 0)
			trackUpdateChannel = (MessageChannelUnion) Main.api.getGuildById(guildId).getDefaultChannel().asTextChannel();
		else trackUpdateChannel = Main.api.getChannelById(MessageChannelUnion.class, config.getValue("track-update-group"));
		
		if(config.getValue("leaderboard-group").length() == 0)
			leaderboardChannel = (MessageChannelUnion) Main.api.getGuildById(guildId).getDefaultChannel().asTextChannel();
		else leaderboardChannel = Main.api.getChannelById(MessageChannelUnion.class, config.getValue("leaderboard-group"));
		
		playFormat = PlayFormat.get(config.getValue("track-play-format"));
		
		Map<String, Integer> outdatedPlayers = new HashMap<>();
		
		for(String tracked : trackedList){
			int userId = Utils.stringToInt(tracked.split("&m=")[0]);
			int mode = Utils.stringToInt(tracked.split("&m=")[1]);
			
			if(userId == -1){
				userId = Utils.stringToInt(Utils.getOsuPlayerId(tracked.split("&m=")[0], true));
				outdatedPlayers.put(tracked, userId);
			}
			
			if(mode == -1 || userId == -1) continue;
			
			String customChannel = config.getValue(userId + "&m=" + mode + "-update-group");
			
			if(customChannel.length() > 0) 
				specificUpdateChannels.put(userId + "&m=" + mode, Main.api.getChannelById(MessageChannelUnion.class, customChannel));
			
			loadPlayer(tracked, userId, mode, false, config);
		}
		
		for(String leaderboardTracked : leaderboardTrackedList){
			int userId = Utils.stringToInt(leaderboardTracked.split("&m=")[0]);
			int mode = Utils.stringToInt(leaderboardTracked.split("&m=")[1]);
			
			if(userId == -1){
				userId = Utils.stringToInt(Utils.getOsuPlayerId(leaderboardTracked.split("&m=")[0], true));
				outdatedPlayers.put(leaderboardTracked, userId);
			}
			
			if(mode == -1 || userId == -1) continue;
			
			loadPlayer(leaderboardTracked, userId, mode, true, config);
		}
		
		if(!outdatedPlayers.isEmpty()){
			for(String name : outdatedPlayers.keySet()){
				int userId = outdatedPlayers.get(name);
				
				if(trackedList.contains(name)) 
					config.removeFromStringList("tracked-players", name, false);
				
				if(leaderboardTrackedList.contains(name)) 
					config.removeFromStringList("leaderboard-tracked-players", name, false);
				
				if(userId != -1){
					if(trackedList.contains(name)) 
						config.appendToStringList("tracked-players", userId + "&m=" + name.split("&m=")[1], true);
					
					if(leaderboardTrackedList.contains(name)) 
						config.appendToStringList("leaderboard-tracked-players", userId + "&m=" + name.split("&m=")[1], true);
				}
			}
		}
	}
	
	private void loadPlayer(String player, int userId, int mode, boolean leaderboard, Configuration config){
		TrackedPlayer registered = TrackedPlayer.get(userId, mode);
		
		String playerInfo = config.getValue(userId + "&m=" + mode + "-info");
		String stats = "";
		String username = "";
		String sDate = "";
		String sActive = "";
		
		if(playerInfo.contains("&r=")){
			if(playerInfo.contains("&a=")){
				sActive = playerInfo.split("&a=")[1].split("&d=")[0];
				sDate = playerInfo.split("&d=")[1];
				username = playerInfo.split("&u=")[1].split("&a=")[0];
				stats = playerInfo.replace("&u=" + username + "&d=" + sDate, "");
			}else{
				sDate = playerInfo.split("&d=")[1];
				username = playerInfo.split("&u=")[1].split("&d=")[0];
				stats = playerInfo.replace("&u=" + username + "&d=" + sDate, "");
			}
		}
		
		if(registered == null)
			if(username.length() > 0)
				registered = new TrackedPlayer(username, userId, mode);
			else registered = new TrackedPlayer(userId, mode);
		
		if(playerInfo.contains("&r=")){
			registered.setStats(stats);
			registered.setLastUpdate(new CustomDate(sDate));
			
			if(sActive.length() > 0) 
				registered.setLastActive(new CustomDate(sActive));
		}
		
		if(leaderboard) registered.setLeaderboardTrack(true);
		else registered.setNormalTrack(true);
		
		registered.trackPlayer(this, leaderboard);
	}
}
