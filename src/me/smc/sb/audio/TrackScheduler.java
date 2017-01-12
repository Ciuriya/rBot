package me.smc.sb.audio;

import java.awt.Color;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;

import me.smc.sb.utils.Utils;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.MessageChannel;

public class TrackScheduler extends AudioEventAdapter{

	private final AudioPlayer player;
	private final BlockingQueue<CustomAudioTrack> queue;
	private MessageChannel channel;
	private CustomAudioTrack currentSong;
	private AudioPlaylist currentPlaylist;
	private String playlistUrl;
	private CustomAudioTrack nextPlaylistSong;
	private boolean randomizePlaylist;

	public TrackScheduler(AudioPlayer player){
		this.player = player;
		this.queue = new LinkedBlockingQueue<>();
	}
	
	public void setMessageChannel(MessageChannel channel){
		this.channel = channel;
	}
	
	public int size(){
		return queue.size();
	}
	
	public void clear(){
		queue.clear();
	}
	
	public void queue(AudioPlaylist playlist, String url, boolean random){
		this.currentPlaylist = playlist;
		this.randomizePlaylist = random;
		this.playlistUrl = url;

		nextPlaylistSong = new CustomAudioTrack(currentPlaylist.getSelectedTrack(), "");
		
		queueNextPlaylistSong();
	}

	public void queue(AudioTrack track, String url){
		CustomAudioTrack customTrack = new CustomAudioTrack(track, url);
		
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
		if(queue.isEmpty() && !currentPlaylist.getTracks().isEmpty()){
			player.stopTrack();
			
			queueNextPlaylistSong();
			return;
		}
		
		nextPlaylistSong = null;
		
		CustomAudioTrack track = queue.poll();

		if(track != null && player.startTrack(track.getTrack(), false)){
			currentSong = track;
			updateChannel();
		}else player.startTrack(null, false);
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
	
	private void updateChannel(){
		if(channel != null){
			AudioTrack track = player.getPlayingTrack();
			String currentUrl = currentSong.getURL();
			
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
			
			if(currentPlaylist != null)
				builder.addField("Playlist", currentPlaylist.getName() + " (" + (currentPlaylist.getTracks().size() + 1) + " left)\n" +
											 playlistUrl, true);
			
			Utils.info(channel, builder.build());
		}
	}
}
