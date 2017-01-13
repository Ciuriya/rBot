package me.smc.sb.audio;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import me.smc.sb.discordcommands.VoiceCommand;
import me.smc.sb.utils.Utils;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.MessageChannel;

public class CustomAudioLoadResultHandler implements AudioLoadResultHandler{

	private MessageChannel channel;
	private Guild guild;
	private String url;
	private boolean random;
	private VoiceCommand voice;
	private boolean loading;
	
	public CustomAudioLoadResultHandler(MessageChannel channel, Guild guild, String url,
										boolean random, boolean loading, VoiceCommand vc){
		this.channel = channel;
		this.guild = guild;
		this.url = url;
		this.random = random;
		this.loading = loading;
		voice = vc;
	}
	
	@Override
	public void trackLoaded(AudioTrack track){
		TrackScheduler scheduler = voice.getGuildAudioPlayer(channel, guild).scheduler;
		
		int size = scheduler.size() + 1;
		
		if(!loading)
			Utils.info(channel, "**" + track.getInfo().title + "** added to queue!\n(" + size + " item" + (size > 1 ? "s" : "") + " in queue)");
		
		scheduler.queue(track, url);
	}

	@Override
	public void playlistLoaded(AudioPlaylist playlist){
		TrackScheduler scheduler = voice.getGuildAudioPlayer(channel, guild).scheduler;
		
		int size = scheduler.size() + 1;
		
		if(!loading)
			Utils.info(channel, "**" + playlist.getName() + "** playlist added to queue! (" + playlist.getTracks().size() + " songs)" +
								"\n(" + size + " item" + (size > 1 ? "s" : "") + " in queue)");
		
		scheduler.queue(playlist, url, random);
	}

	@Override
	public void noMatches(){
		if(!loading)
			Utils.info(channel, "Nothing found by the name of " + url);
	}

	@Override
	public void loadFailed(FriendlyException exception){
		if(!loading)
			Utils.info(channel, "Could not play: " + exception.getMessage());
	}
	
}
