package me.smc.sb.audio;

import java.awt.Color;
import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;

import me.smc.sb.communication.Server;
import me.smc.sb.discordcommands.VoiceCommand;
import me.smc.sb.main.Main;
import me.smc.sb.utils.Configuration;
import me.smc.sb.utils.Log;
import me.smc.sb.utils.Utils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;

public class TrackScheduler extends AudioEventAdapter{

	private final AudioPlayer player;
	private final BlockingQueue<CustomAudioTrack> queue;
	private MessageChannelUnion channel;
	private CustomAudioTrack currentSong;
	private AudioPlaylist currentPlaylist;
	private String playlistUrl;
	private CustomAudioTrack nextPlaylistSong;
	private boolean randomizePlaylist;
	private boolean loading;

	public TrackScheduler(AudioPlayer player){
		this.player = player;
		this.queue = new LinkedBlockingQueue<>();
		loading = false;
	}
	
	public void setMessageChannel(MessageChannelUnion channel){
		this.channel = channel;
		Main.serverConfigs.get(channel.asGuildMessageChannel().getGuild().getId()).writeValue("voice-text-channel", channel == null ? "" : channel.getId());
	}
	
	public int size(){
		return queue.size();
	}
	
	public void clear(){
		queue.clear();
		
		currentPlaylist = null;
		playlistUrl = null;
		nextPlaylistSong = null;
		
		saveScheduling(Main.serverConfigs.get(channel.asGuildMessageChannel().getGuild().getId()));
	}
	
	public void queue(AudioPlaylist playlist, String url, boolean random){
		this.currentPlaylist = playlist;
		this.randomizePlaylist = random;
		this.playlistUrl = url;
		nextPlaylistSong = null;
		
		if(!loading) queueNextPlaylistSong();
	}

	public void queue(AudioTrack track, String url){
		CustomAudioTrack customTrack = new CustomAudioTrack(track, url);
		
		if(loading){
			queue.offer(customTrack);
			return;
		}
		
		if(!player.startTrack(track, true))
			queue.offer(customTrack);
		else{
			currentSong = customTrack;
			updateChannel();
		}
	}
	
	private void queueNextPlaylistSong(){
		if(nextPlaylistSong == null)
			nextPlaylistSong = new CustomAudioTrack(getNextPlaylistSong(), "");
		
		if(nextPlaylistSong == null) return;
		
		AudioTrack track = nextPlaylistSong.getTrack();
		
		nextPlaylistSong = new CustomAudioTrack(getNextPlaylistSong(), "");
		
		queue(track, "");
	}
	
	private AudioTrack getNextPlaylistSong(){
		if(currentPlaylist.getTracks().isEmpty()) return null;
		
		int listPosition = 0;
		
		if(randomizePlaylist && currentPlaylist.getTracks().size() > 1)
			listPosition = Utils.fetchRandom(0, currentPlaylist.getTracks().size() - 1);
		
		AudioTrack track = currentPlaylist.getTracks().get(listPosition);
		
		if(track != null) currentPlaylist.getTracks().remove(listPosition);
		
		return track;
	}

	public void nextTrack(){
		if(queue.isEmpty() && currentPlaylist != null && !currentPlaylist.getTracks().isEmpty()){
			player.stopTrack();
			
			queueNextPlaylistSong();
			return;
		}
		
		if(nextPlaylistSong != null)
			currentPlaylist.getTracks().add(0, nextPlaylistSong.getTrack());
		
		nextPlaylistSong = null;
		
		if(queue.isEmpty() && channel != null){
			Main.serverConfigs.get(channel.asGuildMessageChannel().getGuild().getId()).writeValue("voice-state", "stopped");
			return;
		}
		
		CustomAudioTrack track = queue.poll();

		if(track != null && player.startTrack(track.getTrack(), false)){
			Main.serverConfigs.get(channel.asGuildMessageChannel().getGuild().getId()).writeValue("voice-state", "playing");
			
			currentSong = track;
			updateChannel();
		}else player.startTrack(null, false);
	}
	
	@Override
	public void onPlayerPause(AudioPlayer player){
		super.onPlayerPause(player);
		
		updateNP(null);
	}
	
	@Override
	public void onPlayerResume(AudioPlayer player){
		super.onPlayerResume(player);
		
		updateChannel();
	}

	@Override
	public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason){
		if(endReason.mayStartNext)
			nextTrack();
	}
	
	public void updateNP(AudioTrack track){
		if(channel == null) return;
		
		String npIp = Main.serverConfigs.get(channel.asGuildMessageChannel().getGuild().getId()).getValue("voice-np-ip");
		
		String info = track == null ? "No Song" : track.getInfo().title;
		
		if(npIp.length() > 0){
			new Thread(new Runnable(){
				public void run(){
					try{
						Server.sendMessage(npIp, 13245, info + " --" + channel.asGuildMessageChannel().getGuild().getName().substring(0, 3));
					}catch(Exception e){}
				}
			}).start();
		}
	}
	
	private void updateChannel(){
		AudioTrack track = player.getPlayingTrack();
		String currentUrl = currentSong.getURL();
		
		if(channel != null){
			Configuration config = Main.serverConfigs.get(channel.asGuildMessageChannel().getGuild().getId());
			
			updateNP(track);
			
			saveScheduling(config);
			
			CustomAudioTrack upNext = queue.peek();
			
			if(upNext == null && nextPlaylistSong != null)
				upNext = nextPlaylistSong;
			
			String length = "";
			
			if(track.getPosition() > 1000)
				length = Utils.toDuration(track.getPosition()) + " / ";
			
			EmbedBuilder builder = new EmbedBuilder()
								   .setColor(Color.GREEN)
								   .addField("Title", track.getInfo().title + 
										   			  (currentUrl.length() > 0 ? "\n" + currentUrl : ""), 
										   			  true);
			
			if(track.getInfo().length < 2678400000L)
				builder.addField("Length", length + Utils.toDuration(track.getInfo().length), true);
			
			if(upNext != null)
				builder.addField("Up Next", upNext.getTrack().getInfo().title + 
											(upNext.getURL().length() > 0 ? "\n" + upNext.getURL() : ""), 
											true);
			
			if(currentPlaylist != null && nextPlaylistSong != null)
				builder.addField("Playlist", currentPlaylist.getName() + " (" + (currentPlaylist.getTracks().size() + 1) + " left)\n" +
											 playlistUrl, true);
			
			Utils.info(channel, builder.build());
		}
	}
	
	public void loadScheduling(VoiceCommand voice, GuildMusicManager music, MessageChannelUnion channel, Configuration config){
		try{
			String state = config.getValue("voice-state");
			
			loading = true;
			
			this.channel = channel;
			
			randomizePlaylist = config.getBoolean("voice-random");
			
			String currentInfo = config.getValue("voice-current");
			long currentPosition = 0;
			
			if(currentInfo.length() > 0){
				String[] splitInfo = currentInfo.split("\\|\\|[0-9]");
				currentInfo = splitInfo[0];
				
				if(splitInfo.length > 1)
					currentPosition = Long.parseLong(splitInfo[1]);
				
				if(!currentInfo.startsWith("|")){
					final String cInfo = currentInfo;
					
					try{
						voice.playerManager.loadItemOrdered(music, currentInfo, new AudioLoadResultHandler(){
							@Override
							public void trackLoaded(AudioTrack track){
								currentSong = new CustomAudioTrack(track, cInfo);
							}
							
							@Override public void playlistLoaded(AudioPlaylist playlist){}
							@Override public void loadFailed(FriendlyException exception){}
							@Override public void noMatches(){}
						}).get();
					}catch(Exception e){
						Log.logger.log(Level.SEVERE, e.getMessage(), e);
					}
				}
			}
			
			ArrayList<String> trackQueue = config.getStringList("voice-queue");
			
			if(trackQueue.size() > 0)
				for(String url : trackQueue)
					try{
						voice.playerManager.loadItemOrdered(music, url, 
											new CustomAudioLoadResultHandler(channel, channel.asGuildMessageChannel().getGuild(), url, randomizePlaylist, true, voice)).get();
					}catch(Exception e){
						Log.logger.log(Level.SEVERE, e.getMessage(), e);
					}
			
			String playlistUrl = config.getValue("voice-playlist");
			
			if(playlistUrl.length() > 0)
				try{
					voice.playerManager.loadItemOrdered(music, playlistUrl, 
										new CustomAudioLoadResultHandler(channel, channel.asGuildMessageChannel().getGuild(), playlistUrl, randomizePlaylist, true, voice)).get();
				}catch(Exception e){
					Log.logger.log(Level.SEVERE, e.getMessage(), e);
				}
				
			if(currentInfo.length() > 0 && currentInfo.startsWith("|")){
				for(AudioTrack track : currentPlaylist.getTracks())
					if(track.getInfo().title.equalsIgnoreCase(currentInfo.substring(1))){
						currentSong = new CustomAudioTrack(track, "");
						break;
					}
				
				if(currentSong != null)
					currentPlaylist.getTracks().remove(currentSong.getTrack());
			}
			
			String nextPS = config.getValue("voice-next-ps");
			
			if(nextPS.length() > 0 && queue.size() == 0){
				for(AudioTrack track : currentPlaylist.getTracks())
					if(track.getInfo().title.equalsIgnoreCase(nextPS)){
						nextPlaylistSong = new CustomAudioTrack(track, "");
						break;
					}
				
				if(nextPlaylistSong != null)
					currentPlaylist.getTracks().remove(nextPlaylistSong.getTrack());
			}
			
			loading = false;

			if(state.length() == 0) return;
			
			String voiceChannel = config.getValue("voice-channel");
			
			if(voiceChannel.length() > 0)
				voice.joinVoiceChannel(channel.asGuildMessageChannel().getGuild(), channel, voiceChannel);
			
			if(currentSong != null){
				player.startTrack(currentSong.getTrack(), false);
				
				if(!currentSong.getURL().contains("www.twitch.tv/"))
					player.getPlayingTrack().setPosition(currentPosition);
			}
			
			if(state.equalsIgnoreCase("paused"))
				player.setPaused(true);
			else if(state.equalsIgnoreCase("stopped"))
				player.stopTrack();
			else if(state.equalsIgnoreCase("playing"))
				updateChannel();
		}catch(Exception e){}
	}
	
	public void saveScheduling(Configuration config){
		ArrayList<String> trackQueue = new ArrayList<String>();
		
		for(CustomAudioTrack track : queue)
			trackQueue.add(track.getURL());
		
		config.writeStringList("voice-queue", trackQueue, false);
		config.writeValue("voice-playlist", playlistUrl);
		config.writeValue("voice-random", this.randomizePlaylist);
		
		String currentInfo = "";
		
		if(currentSong != null){
			currentInfo = currentSong.getURL();
			
			if(currentInfo.length() == 0)
				currentInfo = "|" + currentSong.getTrack().getInfo().title;
			
			if(player.getPlayingTrack() != null && !currentInfo.contains("www.twitch.tv/"))
				currentInfo += "||" + player.getPlayingTrack().getPosition();
			else currentInfo += "||0";
		}
		
		config.writeValue("voice-current", currentInfo);
		
		String nextPS = nextPlaylistSong == null ? "" : nextPlaylistSong.getTrack().getInfo().title;
		
		config.writeValue("voice-next-ps", nextPS);
	}
}
