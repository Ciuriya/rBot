package me.smc.sb.discordcommands;

import java.util.HashMap;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import me.smc.sb.audio.GuildMusicManager;
import me.smc.sb.audio.TrackScheduler;
import me.smc.sb.utils.Utils;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.entities.VoiceChannel;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.managers.AudioManager;

public class VoiceCommand extends GlobalCommand{

	private final AudioPlayerManager playerManager;
	private static HashMap<String, GuildMusicManager> players;
	
	public VoiceCommand(){
		super(null, 
			  " - Lets the bot join the requested voice channel", 
			  "{prefix}voice\nThis command makes the bot join the requested voice channel.\n\n" +
			  "----------\nUsage\n----------\n{prefix}voice join {channel} - Makes the bot join the voice channel\n" + 
			  "{prefix}voice leave - Makes the bot leave the current voice channel\n{prefix}voice play - " +
			  "Plays the next item in the queue\n{prefix}voice pause - Pauses the current song\n" +
			  "{prefix}voice stop - Stops the player\n{prefix}voice volume {0-150} - Sets the volume of the player\n" +
			  "{prefix}voice queue {link} (true) - Queues up a song or playlist in the player, if true is specified, it randomizes the playlist" +
			  " (if it's a playlist)\nNote that if a playlist is playing, it will simply stop going through the playlist after current song" +
			  "\n{prefix}voice clear - Clears the player's queue\n{prefix}voice skip - Skips the currently playing song (does not skip the playlist)" +
			  "\n\n----------\nAliases\n----------\n{prefix}v\n{prefix}music",  
			  false, 
			  "voice", "v", "music");
		
		players = new HashMap<>();
		
	    this.playerManager = new DefaultAudioPlayerManager();
	    AudioSourceManagers.registerRemoteSources(playerManager);
	    AudioSourceManagers.registerLocalSource(playerManager);
	}

	@Override
	public void onCommand(MessageReceivedEvent e, String[] args){
		Utils.deleteMessage(e.getChannel(), e.getMessage());
		if(!Utils.checkArguments(e, args, 1)) return;
		
		switch(args[0]){
			case "join": joinVoiceChannel(e, args); break;
			case "leave": leaveVoiceChannel(e, args); break;
			case "play":
				GuildMusicManager music = getGuildAudioPlayer(e.getChannel(), e.getGuild());
				
				if(music.player.isPaused()){
					music.player.setPaused(false);
					
					Utils.info(e.getChannel(), "Player resumed!");
				}else music.scheduler.nextTrack();
				
				break;
			case "pause": 
				getGuildAudioPlayer(e.getChannel(), e.getGuild()).player.setPaused(true);
				
				Utils.info(e.getChannel(), "Player paused!");
				break;
			case "stop": 
				getGuildAudioPlayer(e.getChannel(), e.getGuild()).player.stopTrack();
				
				Utils.info(e.getChannel(), "Player stopped!");
				break;
			case "volume": 
				int volume = Utils.stringToInt(args[1]);
				
				if(volume != -1){
					getGuildAudioPlayer(e.getChannel(), e.getGuild()).player.setVolume(volume);
					
					Utils.info(e.getChannel(), "Player set to " + volume + "% volume!");
				}
				
				break;
			case "queue": queueSong(e, args); break;
			case "clear": 
				getGuildAudioPlayer(e.getChannel(), e.getGuild()).scheduler.clear();
				
				Utils.info(e.getChannel(), "Player queue cleared!");
				break;
			case "skip": skipSong(e, args); break;
		}
	}
	
	private synchronized GuildMusicManager getGuildAudioPlayer(MessageChannel channel, Guild guild){
		GuildMusicManager musicManager = players.get(guild.getId());

		if(musicManager == null){
			musicManager = new GuildMusicManager(playerManager);
			players.put(guild.getId(), musicManager);
		}

		musicManager.scheduler.setMessageChannel(channel);
		guild.getAudioManager().setSendingHandler(musicManager.getSendHandler());

		return musicManager;
	}
	
	private void joinVoiceChannel(MessageReceivedEvent e, String[] args){
		if(!Utils.checkArguments(e, args, 2)) return;
		
		VoiceChannel vChannel = null;
		
		String cName = "";
		
		for(int i = 1; i < args.length; i++)
			cName += args[i] + " ";
		
		for(VoiceChannel voice : e.getGuild().getVoiceChannels())
			if(voice.getName().equalsIgnoreCase(cName.substring(0, cName.length() - 1)))
				vChannel = voice;
		
		if(vChannel == null){
			Utils.info(e.getChannel(), "This voice channel does not exist!"); 
			return;
		}
		
		AudioManager audioManager = e.getGuild().getAudioManager();
		
		if(audioManager.isConnected() && audioManager.getConnectedChannel().getId().equalsIgnoreCase(vChannel.getId())){
			Utils.info(e.getChannel(), "Already connected to this channel!");
			return;
		}else if(audioManager.isConnected()){
			GuildMusicManager music = getGuildAudioPlayer(e.getChannel(), e.getGuild());
			music.player.setPaused(true);
			
			audioManager.closeAudioConnection();
			audioManager.openAudioConnection(vChannel);
			
			music.player.setPaused(false);
		}else if(audioManager.isAttemptingToConnect()){
			audioManager.closeAudioConnection();
			audioManager.openAudioConnection(vChannel);
		}else audioManager.openAudioConnection(vChannel);
	}
	
	private void leaveVoiceChannel(MessageReceivedEvent e, String[] args){
		if(players.containsKey(e.getGuild().getId())){
			players.get(e.getGuild().getId()).player.destroy();
			players.remove(e.getGuild().getId());
		}
		
		e.getGuild().getAudioManager().closeAudioConnection();
	}
	
	private void queueSong(MessageReceivedEvent e, String[] args){
		if(!Utils.checkArguments(e, args, 2)) return;
		
		GuildMusicManager music = getGuildAudioPlayer(e.getChannel(), e.getGuild());
		
		boolean random = args.length >= 3 && args[2].equalsIgnoreCase("true") ? true : false;
		
		playerManager.loadItemOrdered(music, args[1], new AudioLoadResultHandler(){
			@Override
			public void trackLoaded(AudioTrack track){
				TrackScheduler scheduler = getGuildAudioPlayer(e.getChannel(), e.getGuild()).scheduler;
				
				Utils.info(e.getChannel(), track.getInfo().title + " added to queue!\n(" + scheduler.size() + " items queued)");
				
				scheduler.queue(track, args[1]);
			}

			@Override
			public void playlistLoaded(AudioPlaylist playlist){
				TrackScheduler scheduler = getGuildAudioPlayer(e.getChannel(), e.getGuild()).scheduler;
				
				Utils.info(e.getChannel(), playlist.getName() + " playlist added to queue! (" + playlist.getTracks().size() + " songs)" +
										   "\n(" + (scheduler.size() + 1) + " items in queue)");
				
				scheduler.queue(playlist, args[1], random);
			}

			@Override
			public void noMatches(){
				Utils.info(e.getChannel(), "Nothing found by the name of " + args[1]);
			}

			@Override
			public void loadFailed(FriendlyException exception){
				Utils.info(e.getChannel(), "Could not play: " + exception.getMessage());
			}
		});
	}
	
	private void skipSong(MessageReceivedEvent e, String[] args){
		if(!checkPlayer(e)) return;
		
		Utils.info(e.getChannel(), "Skipping current song...");
		
		getGuildAudioPlayer(e.getChannel(), e.getGuild()).scheduler.nextTrack();
	}
	
	private boolean checkPlayer(MessageReceivedEvent e){
		if(!players.containsKey(e.getGuild().getId())){
			Utils.infoBypass(e.getChannel(), "The player isn't started!");
			return false;
		}
		
		return true;
	}
}
