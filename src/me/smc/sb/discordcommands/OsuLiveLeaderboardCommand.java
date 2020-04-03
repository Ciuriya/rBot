package me.smc.sb.discordcommands;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.stream.Collectors;

import me.smc.sb.main.Main;
import me.smc.sb.perm.Permissions;
import me.smc.sb.tracking.TrackedPlayer;
import me.smc.sb.tracking.TrackingGuild;
import me.smc.sb.tracking.TrackingUtils;
import me.smc.sb.utils.Utils;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class OsuLiveLeaderboardCommand extends GlobalCommand{

	private static final int TIME_BETWEEN_UPDATES = 150; // seconds
	
	public OsuLiveLeaderboardCommand(){
		super(Permissions.MANAGE_MESSAGES,
			  " - Lets you keep track of osu! players' ranks in real-time (make sure the message is always visible)", 
			  "{prefix}osuboard\nThis command lets you track osu! players' ranks in real-time\n\n" +
		      "----------\nUsage\n----------\n{prefix}osuboard {player} ({mode={0/1/2/3}}) - Adds or removes the player from the leaderboard\n" + 
		      "{prefix}osuboard post - Reposts the leaderboard and uses that one instead\n\n" + 
		      "----------\nAliases\n----------\n{prefix}osuleaderboard\n{prefix}osuladder\n{prefix}leaderboard\n{prefix}ladder", 
			  false,
			  "osuboard", "osuladder", "osuleaderboard", "leaderboard", "ladder");
		
		start();
	}
	
	@Override
	public void onCommand(MessageReceivedEvent e, String[] args){
		Utils.deleteMessage(e.getChannel(), e.getMessage());
		if(!Utils.checkArguments(e, args, 1)) return;
		
		String user = "";
		int mode = 0;
		boolean post = false;
		
		for(int i = 0; i < args.length; i++)
			if(args[i].contains("{mode="))
				mode = Utils.stringToInt(args[i].split("\\{mode=")[1].split("}")[0]);
			else if((args[i].equalsIgnoreCase("post") || args[i].equalsIgnoreCase("show")) && i == 0) post = true;
			else user += " " + args[i];
		
		TrackingGuild guild = TrackingGuild.get(e.getGuild().getId());
		
		if(post){
			updatePost(guild);
			
			return;
		}
		
		user = user.substring(1);
		
		TrackedPlayer player = TrackedPlayer.get(user, mode);
		
		if(guild == null) guild = new TrackingGuild(e.getGuild().getId());
		
		if(player != null && player.isTracked(guild, true)){
			boolean untracked = guild.untrack(player.getUsername(), mode, true);
			
			guild.setLeaderboardChannel(e.getTextChannel().getId());
			
			if(untracked){
				Utils.info(e.getChannel(), "Removed " + player.getUsername() + " from the " + TrackingUtils.convertMode(mode) + " leaderboard!" +
						   				   "\nA full refresh cycle now takes " + OsuTrackCommand.currentRefreshRate + " seconds!");
				
				updatePost(guild);
			}
			
			return;
		}
		
		boolean tracked = guild.track(user, mode, true, true);
		
		player = TrackedPlayer.get(user, mode);
		
		guild.setLeaderboardChannel(e.getTextChannel().getId());
		
		if(tracked){
			Utils.info(e.getChannel(), "Added " + user + " to the " + TrackingUtils.convertMode(mode) + " leaderboard!" +
									   "\nA full refresh cycle now takes " + OsuTrackCommand.currentRefreshRate + " seconds!");
			
			updatePost(guild);
		}
	}
	
	public static void start(){
		new Timer().scheduleAtFixedRate(new TimerTask(){
			public void run(){
				if(OsuTrackCommand.trackingStarted){
					for(Guild guild : Main.api.getGuilds()){
						TrackingGuild trackGuild = TrackingGuild.get(guild.getId());
						
						if(trackGuild != null)
							new Timer().scheduleAtFixedRate(new TimerTask(){
								public void run(){
									updatePost(trackGuild);
								}
							}, TIME_BETWEEN_UPDATES * 1000, TIME_BETWEEN_UPDATES * 1000);
					}
					
					cancel();
				}
			}
		}, 1000, 1000);

	}
	
	public static void updatePost(TrackingGuild guild){
		if(guild == null) return;
		
		new Thread(new Runnable(){
			public void run(){
				TextChannel channel = guild.getLeaderboardChannel();
				List<Message> messages = new ArrayList<>();
				List<Message> standard = new ArrayList<>(), taiko = new ArrayList<>(), ctb = new ArrayList<>(), mania = new ArrayList<>();
				boolean error = false;
				
				try{
					messages = channel.getHistory().retrievePast(50).complete();
					
					if(messages.isEmpty()) error = true;
					else for(Message message : messages)
							if(message.getAuthor().getId().equalsIgnoreCase(Main.api.getSelfUser().getId()))
								if(message.getContentDisplay().contains("Ladder | ")){
									if(message.getContentDisplay().contains("Ladder | " + TrackingUtils.convertMode(0)))
										standard.add(message);
									else if(message.getContentDisplay().contains("Ladder | " + TrackingUtils.convertMode(1)))
										taiko.add(message);
									else if(message.getContentDisplay().contains("Ladder | " + TrackingUtils.convertMode(2)))
										ctb.add(message);
									else if(message.getContentDisplay().contains("Ladder | " + TrackingUtils.convertMode(3)))
										mania.add(message);
								}
				}catch(Exception ex){ error = true; }
				
				if(error) return;
				
				List<TrackedPlayer> tracked = TrackedPlayer.get(guild, true);
				
				for(int i = 0; i < 4; i++){
					final int mode = i;
					List<TrackedPlayer> modeTracked = tracked.stream().filter(p -> p.getMode() == mode).collect(Collectors.toList());
					
					if(modeTracked.size() > 0){
						List<Message> postedMessages = null;
						
						switch(i){
							case 1: postedMessages = taiko; break;
							case 2: postedMessages = ctb; break;
							case 3: postedMessages = mania; break;
							default: postedMessages = standard;
						}

						String header = "```diff\n---[ " + channel.getGuild().getName() + " Ladder | " + TrackingUtils.convertMode(i) + " ]---\n\n";
						String content = "";
						
						modeTracked.sort(new Comparator<TrackedPlayer>(){
							@Override
							public int compare(TrackedPlayer o1, TrackedPlayer o2){
								return Integer.compare(o1.getRank(), o2.getRank());
							}
						});
						
						int rank = 1;
						TrackedPlayer longestPPRank = modeTracked.stream().max(new Comparator<TrackedPlayer>(){
							@Override
							public int compare(TrackedPlayer o1, TrackedPlayer o2){
								return Integer.compare(String.valueOf(o1.getRank()).length() + String.valueOf(o1.getCountryRank()).length(), 
													   String.valueOf(o2.getRank()).length() + String.valueOf(o2.getCountryRank()).length());
							}
						}).orElse(null);
						TrackedPlayer longestUser = modeTracked.stream().max(new Comparator<TrackedPlayer>(){
							@Override
							public int compare(TrackedPlayer o1, TrackedPlayer o2){
								return Integer.compare(o1.getUsername().length(), o2.getUsername().length());
							}
						}).orElse(null);
						
						for(TrackedPlayer player : modeTracked){
							if(player.getRank() > 0){
								content += "+ #" + rank;
								
								int rankSpaces = String.valueOf(modeTracked.size()).length() - String.valueOf(rank).length();
								int usernameSpaces = longestUser == null ? 0 : longestUser.getUsername().length() - player.getUsername().length();
								int ppRankSpaces = longestPPRank == null ? 0 : (String.valueOf(longestPPRank.getRank()).length() + String.valueOf(longestPPRank.getCountryRank()).length()) -
																			   (String.valueOf(player.getRank()).length() + String.valueOf(player.getCountryRank()).length());
								
								for(int o = 0; o < rankSpaces; o++) content += " ";
								
								content += " | ";
								
								int u = 0;
								
								for(u = 0; u < usernameSpaces / 2; u++) content += " ";
								
								content += player.getUsername();
								
								for(; u < usernameSpaces; u++) content += " ";
								
								content += " | #" + player.getRank() + " (" + player.getCountry() + " #" + player.getCountryRank() + ")";
								
								for(int o = 0; o < ppRankSpaces; o++) content += " ";
								
								content += " | " + Utils.df(player.getPP(), 2) + "pp\n";
								
								if(header.length() + content.length() >= 1800){
									if(postedMessages.size() > 0){
										postedMessages.get(0).editMessage(header + content + "```").complete();
										postedMessages.remove(0);
									}else Utils.infoBypass(channel, header + content + "```");
									
									content = "";
								}
								
								rank++;
							}
						}
						
						content += "\n+ Last Updated - " + Utils.toDate(Utils.getCurrentTimeUTC()) + " UTC";
						
						if(postedMessages.size() > 0){
							postedMessages.get(0).editMessage(header + content + "```").complete();
							postedMessages.remove(0);
							
							if(postedMessages.size() > 0)
								for(Message toDelete : new ArrayList<>(postedMessages))
									toDelete.delete().complete();	
						}else Utils.infoBypass(channel, header + content + "```");
					}
				}
			}
		}).start();
	}
}
