package me.smc.sb.tracking;

import java.io.File;
import java.util.ArrayList;

import me.smc.sb.utils.Configuration;
import me.smc.sb.utils.Utils;
import net.dv8tion.jda.core.entities.TextChannel;

public class TrackingGuild{

	private String guildId;
	private TextChannel trackUpdateChannel;
	
	public TrackingGuild(String guildId){
		this.guildId = guildId;
		
		load();
	}
	
	public String getGuildId(){
		return guildId;
	}
	
	public Configuration getConfig(){
		return new Configuration(new File("Guilds/" + guildId + ".txt"));
	}
	
	public void track(String username, int mode, boolean addToFile){
		Configuration config = getConfig();
		
		boolean success = false;
		TrackedPlayer registered = TrackedPlayer.get(username, mode);
		
		if(registered != null){
			success = registered.trackPlayer(this);
		}else{
			int userId = Utils.stringToInt(Utils.getOsuPlayerId(username));
			
			if(userId == -1){
				Utils.info(trackUpdateChannel, "Could not fetch userId from " + username + "! Please try again later. (osu! website error)");
				return;
			}
			
			registered = new TrackedPlayer(username, userId, mode);
			success = registered.trackPlayer(this);
		}
		
		if(success && addToFile){
			config.appendToStringList(registered.getUserId() + "&m=" + mode, "tracked-players", true);
			Utils.info(trackUpdateChannel, "Now tracking " + username + " in the " + TrackingUtils.convertMode(mode) + " mode!");
		}
	}
	
	public void untrack(String username, int mode){
		Configuration config = getConfig();
		
		TrackedPlayer registered = TrackedPlayer.get(username, mode);
		
		if(registered == null) return;
		
		if(registered.isTracked(this)){
			config.removeFromStringList(registered.getUserId() + "&m=" + mode, "tracked-players", true);
			Utils.info(trackUpdateChannel, "Stopped tracking " + username + " in the " + TrackingUtils.convertMode(mode) + " mode!");
		}
	}
	
	public void load(){
		ArrayList<String> trackedList = getConfig().getStringList("tracked-players");
		
		for(String tracked : trackedList){
			int userId = Utils.stringToInt(tracked.split("&m=")[0]);
			int mode = Utils.stringToInt(tracked.split("&m=")[1]);
			
			if(userId == -1 || mode == -1) continue;
			
			TrackedPlayer registered = TrackedPlayer.get(userId, mode);
			
			if(registered == null) registered = new TrackedPlayer(userId, mode);
			
			registered.trackPlayer(this);
		}
	}
}
