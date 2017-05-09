package me.smc.sb.tracking;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import me.smc.sb.main.Main;
import me.smc.sb.utils.Configuration;
import me.smc.sb.utils.Utils;
import net.dv8tion.jda.core.entities.TextChannel;

public class TrackingGuild{

	private String guildId;
	private TextChannel trackUpdateChannel;
	private Map<String, TextChannel> specificUpdateChannels;
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
		trackUpdateChannel = Main.api.getTextChannelById(channelId);
	}
	
	public void setPlayerChannel(int userId, int mode, String channelId){
		getConfig().writeValue(userId + "&m=" + mode + "-update-group", channelId);
		specificUpdateChannels.put(userId + "&m=" + mode, Main.api.getTextChannelById(channelId));
	}
	
	public void setPlayerInfo(TrackedPlayer player){
		Configuration config = getConfig();
		String toSave = Utils.df(player.getPP()) + "&r=" + player.getRank() + "&cr=" + player.getCountryRank();
		
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
	
	public TextChannel getChannel(TrackedPlayer player){
		if(specificUpdateChannels.containsKey(player.getUserId() + "&m=" + player.getMode()))
			return specificUpdateChannels.get(player.getUserId() + "&m=" + player.getMode());
		
		return trackUpdateChannel;
	}
	
	public boolean track(String username, int mode, boolean addToFile){
		Configuration config = getConfig();
		
		boolean success = false;
		TrackedPlayer registered = TrackedPlayer.get(username, mode);
		
		if(registered != null){
			success = registered.trackPlayer(this);
		}else{
			int userId = Utils.stringToInt(Utils.getOsuPlayerId(username));
			
			if(userId == -1){
				Utils.info(trackUpdateChannel, "Could not fetch userId from " + username + "! Please try again later. (osu! website error)");
				return false;
			}
			
			registered = new TrackedPlayer(username, userId, mode);
			success = registered.trackPlayer(this);
		}
		
		if(success && addToFile)
			config.appendToStringList("tracked-players", registered.getUserId() + "&m=" + mode, true);
		
		return success;
	}
	
	public boolean untrack(String username, int mode){
		Configuration config = getConfig();
		
		TrackedPlayer registered = TrackedPlayer.get(username, mode);
		
		if(registered == null) return false;
		
		if(registered.isTracked(this)){
			registered.untrackPlayer(this);
			config.removeFromStringList("tracked-players", registered.getUserId() + "&m=" + mode, true);
			
			return true;
		}
		
		return false;
	}
	
	public void load(){
		Configuration config = getConfig();
		ArrayList<String> trackedList = config.getStringList("tracked-players");
		
		if(config.getValue("track-update-group").length() == 0)
			trackUpdateChannel = Main.api.getGuildById(guildId).getPublicChannel();
		else trackUpdateChannel = Main.api.getTextChannelById(config.getValue("track-update-group"));
		
		playFormat = PlayFormat.get(config.getValue("track-play-format"));
		
		Map<String, Integer> outdatedPlayers = new HashMap<>();
		
		for(String tracked : trackedList){
			int userId = Utils.stringToInt(tracked.split("&m=")[0]);
			int mode = Utils.stringToInt(tracked.split("&m=")[1]);
			
			if(userId == -1){
				userId = Utils.stringToInt(Utils.getOsuPlayerId(tracked.split("&m=")[0]));
				outdatedPlayers.put(tracked, userId);
			}
			
			if(mode == -1 || userId == -1) continue;
			
			String customChannel = config.getValue(userId + "&m=" + mode + "-update-group");
			
			if(customChannel.length() > 0) 
				specificUpdateChannels.put(userId + "&m=" + mode, Main.api.getTextChannelById(customChannel));
			
			TrackedPlayer registered = TrackedPlayer.get(userId, mode);
			
			if(registered == null) registered = new TrackedPlayer(userId, mode);
			
			String playerInfo = config.getValue(userId + "&m=" + mode + "-info");
			
			if(playerInfo.contains("&r=")){
				String sDate = playerInfo.split("&d=")[1];
				
				registered.setStats(playerInfo.replace("&d=" + sDate, ""));
				registered.setLastUpdate(new CustomDate(sDate));
			}
			
			registered.trackPlayer(this);
		}
		
		if(!outdatedPlayers.isEmpty()){
			for(String name : outdatedPlayers.keySet()){
				int userId = outdatedPlayers.get(name);
				
				config.removeFromStringList("tracked-players", name, false);
				
				if(userId != -1){
					config.appendToStringList("tracked-players", userId + "&m=" + name.split("&m=")[1], true);
				}
			}
		}
	}
}
