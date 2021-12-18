package me.smc.sb.discordcommands;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;

import org.json.JSONArray;
import org.json.JSONObject;

import me.smc.sb.main.Main;
import me.smc.sb.perm.Permissions;
import me.smc.sb.tracking.OsuBeatmapsRequest;
import me.smc.sb.tracking.OsuRequest;
import me.smc.sb.tracking.TrackingUtils;
import me.smc.sb.utils.Utils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class OsuMapRankTrackCommand extends GlobalCommand{
	
	private static Map<Integer, List<TextChannel>> channels = new HashMap<>();
	private Timer mapRankTrackingTimer;
	private static boolean doneProcessing;
	
	public OsuMapRankTrackCommand(){
		super(Permissions.MANAGE_MESSAGES, 
			  " - Lets you know when maps get ranked", 
			  "{prefix}osutrack\nThis command lets you know when new maps get ranked\n\n" +
		      "----------\nUsage\n----------\n{prefix}osumaptrack {mode (0, 1, 2, 3)} - Tracks or untracks maps in the current channel for the mode\n\n" + 
		      "----------\nAliases\n----------\n{prefix}maptrack", 
			  false,
			  "osumaptrack", "maptrack");
		load();
	}
	
	@Override
	public void onCommand(MessageReceivedEvent e, String[] args){
		if(!Utils.checkArguments(e, args, 1)) return;
		
		if(!doneProcessing){
			Utils.info(e.getChannel(), "The bot just started and is still processing vital information, please try again in a little bit!");
			return;
		}
		
		int mode = Utils.stringToInt(args[0]);
		
		if(mode < 0 || mode > 3){
			Utils.info(e.getChannel(), "The mode must be either 0, 1, 2 or 3!");
			return;
		}
		
		List<TextChannel> trackChannels = new ArrayList<>();
		ArrayList<String> strTrackChannels = Main.serverConfigs.get(e.getGuild().getId()).getStringList("map-rank-track-channels");

		if(channels.containsKey(mode)) trackChannels = channels.get(mode);
		
		if(trackChannels.contains(e.getTextChannel())){
			trackChannels.remove(e.getTextChannel());
			
			strTrackChannels.remove(e.getTextChannel().getId() + "--" + mode);

			Utils.info(e.getChannel(), "No longer tracking new maps for " + TrackingUtils.convertMode(mode) + " in this channel!");
		}else{
			trackChannels.add(e.getTextChannel());
			strTrackChannels.add(e.getTextChannel().getId() + "--" + mode);
			
			Utils.info(e.getChannel(), "Now tracking new maps for " + TrackingUtils.convertMode(mode) + " in this channel!");
		}
		
		Main.serverConfigs.get(e.getGuild().getId()).writeStringList("map-rank-track-channels", strTrackChannels, false);
		channels.put(mode, trackChannels);
	}
	
	public void load(){
		doneProcessing = false;
		
		new Thread(new Runnable(){
			public void run(){
				while(!Main.discordConnected) Utils.sleep(100);
				
				List<TextChannel> std = new ArrayList<>();
				List<TextChannel> taiko = new ArrayList<>();
				List<TextChannel> ctb = new ArrayList<>();
				List<TextChannel> mania = new ArrayList<>();
				
				for(Guild guild : Main.api.getGuilds()){
					List<String> trackChannels = Main.serverConfigs.get(guild.getId()).getStringList("map-rank-track-channels");
					
					if(trackChannels.size() > 0)
						for(String c : trackChannels){
							int mode = Utils.stringToInt(c.split("--")[1]);
							String strChannel = c.split("--")[0];
							
							if(strChannel.length() > 0 && mode >= 0 && mode <= 3){
								TextChannel channel = Main.api.getTextChannelById(strChannel);
								
								if(channel != null) 
									switch(mode){
										case 0: std.add(channel); break;
										case 1: taiko.add(channel); break;
										case 2: ctb.add(channel); break;
										case 3: mania.add(channel); break;
									}
							}
						}
				}
				
				channels.put(0, std);
				channels.put(1, taiko);
				channels.put(2, ctb);
				channels.put(3, mania);
				
				doneProcessing = true;
				
				findAndPostNewMaps();
			}
		}).start();
	}
	
	public void findAndPostNewMaps(){
		if(mapRankTrackingTimer != null) mapRankTrackingTimer.cancel();
		
		mapRankTrackingTimer = new Timer();
		
		Calendar firstCheckTime = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		int currentMinute = firstCheckTime.get(Calendar.MINUTE);
		
		if(currentMinute < 8 || currentMinute >= 48){
			firstCheckTime.set(Calendar.MINUTE, 8);
			
			if(currentMinute >= 48) firstCheckTime.add(Calendar.HOUR, 1);
		}else if(currentMinute < 28) firstCheckTime.set(Calendar.MINUTE, 28);
		else if(currentMinute < 48) firstCheckTime.set(Calendar.MINUTE, 48);
		
		firstCheckTime.set(Calendar.SECOND, 0);
		firstCheckTime.set(Calendar.MILLISECOND, 0);
		
		Calendar lastUpdate = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		
		lastUpdate.setTimeInMillis(firstCheckTime.getTimeInMillis() - 1200000);
		
		mapRankTrackingTimer.scheduleAtFixedRate(new TimerTask(){
			public void run(){
				OsuRequest beatmapsRequest = new OsuBeatmapsRequest("-1", "-1", "-1", "-1", Utils.getMySQLDate(lastUpdate));
				Object beatmapsObj = Main.hybridRegulator.sendRequest(beatmapsRequest, true);
				JSONArray beatmapsArray = null;
				
				lastUpdate.setTimeInMillis(lastUpdate.getTimeInMillis() + 1200000);
				
				if(beatmapsObj != null && beatmapsObj instanceof JSONArray)
					beatmapsArray = (JSONArray) beatmapsObj;
				else return;
				
				Map<Integer, List<JSONObject>> rankedSets = new HashMap<>();
				
				if(beatmapsArray.length() == 0) return;
				
				for(int i = 0; i < beatmapsArray.length(); i++){
					JSONObject jsonObj = beatmapsArray.getJSONObject(i);
					
					int approvedStatus = jsonObj.getInt("approved");
					if(approvedStatus == 1 || approvedStatus == 2){
						int setId = jsonObj.getInt("beatmapset_id");
						List<JSONObject> set = new ArrayList<>();
						
						if(rankedSets.containsKey(setId)) set = rankedSets.get(setId);
						
						set.add(jsonObj);
						rankedSets.put(setId, set);
					}
				}
				
				for(int set : rankedSets.keySet()){
					List<JSONObject> diffs = rankedSets.get(set);
					
					diffs.sort(new Comparator<JSONObject>(){
						public int compare(JSONObject o1, JSONObject o2){
							int modeCompare = Integer.compare(o1.getInt("mode"), o2.getInt("mode"));
							
							if(modeCompare == 0) 
								return Double.compare(o1.getDouble("difficultyrating"), o2.getDouble("difficultyrating"));
							else return modeCompare;
						}
					});
					
					List<Integer> modes = new ArrayList<>();
					EmbedBuilder builder = new EmbedBuilder();
					JSONObject infoDiff = diffs.get(0);
					
					builder.setColor(Color.CYAN);
					builder.setThumbnail("http://b.ppy.sh/thumb/" + set + "l.jpg");
					builder.setAuthor("New ranked map by " + infoDiff.getString("creator"), 
									  "https://osu.ppy.sh/users/" + infoDiff.getInt("creator_id"), 
									  "https://a.ppy.sh/" + infoDiff.getInt("creator_id"));
					builder.setTitle(TrackingUtils.escapeCharacters(infoDiff.getString("artist") + " - " + infoDiff.getString("title")), 
							 		 "https://osu.ppy.sh/beatmapsets/" + set);
					builder.setFooter(TrackingUtils.analyzeMapStatus(infoDiff.getInt("approved")) + " at " + infoDiff.getString("approved_date") + " UTC",
		  							  "http://b.ppy.sh/thumb/" + set + "l.jpg");
					
					String currentText = "";
					int currentMode = 0;
					
					for(JSONObject diff : diffs){
						int mode = diff.getInt("mode");
						
						if(!modes.contains(mode)) modes.add(mode);
						
						if(currentText == "" || currentMode != mode){
							if(currentText != "") currentText += "\n\n";
							
							currentText += "```\n" + TrackingUtils.convertMode(mode) + "\n```";
							currentMode = mode;
						}
						
						String diffText = "\n[" + TrackingUtils.escapeCharacters(diff.getString("version")) + 
										  "](https://osu.ppy.sh/beatmapsets/" + set + "#" + TrackingUtils.convertModeToURLPart(mode) + 
										  "/" + diff.getInt("beatmap_id") + ") • **" + Utils.df(diff.getDouble("difficultyrating"), 2) + "**\u2605";
						
						diffText += "\n\u25b8 CS **" + Utils.df(diff.getDouble("diff_size"), 2) + 
									"** • AR **" + Utils.df(diff.getDouble("diff_approach"), 2) +
									"** • OD **" + Utils.df(diff.getDouble("diff_overall"), 2) +
									"** • HP **" + Utils.df(diff.getDouble("diff_drain"), 2) +
									(mode == 0 || mode == 2 ? "** • **" + diff.getInt("max_combo") + "**x": "**");
						
						currentText += diffText;
					}
					
					currentText = "\n**" + Utils.toDuration(infoDiff.getInt("total_length") * 1000) + "**" + 
							      " • **" + Utils.df(infoDiff.getDouble("bpm"), 2) + "**bpm" +
							      "\n[osu](https://osu.ppy.sh/d/" + set + ")" +
								  " - [no video](https://osu.ppy.sh/d/" + set + "n)\n" + currentText;
					
					builder.setDescription(currentText);
					
					List<TextChannel> channelsToSendTo = new ArrayList<>();
					
					for(int mode : modes){
						List<TextChannel> trackers = new ArrayList<>();
						
						if(channels.containsKey(mode)) trackers = channels.get(mode);
						
						for(TextChannel tracker : trackers)
							if(!channelsToSendTo.contains(tracker))
								channelsToSendTo.add(tracker);
					}
					
					MessageEmbed embed = builder.build();
					
					for(TextChannel channel : channelsToSendTo)
						Utils.info(channel, embed);
				}
			}
		}, firstCheckTime.getTimeInMillis() - Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTimeInMillis(), 1200000);
	}
}
