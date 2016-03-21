package me.smc.sb.discordcommands;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;

import me.smc.sb.main.Main;
import me.smc.sb.perm.Permissions;
import me.smc.sb.utils.CustomFilePlayer;
import me.smc.sb.utils.Log;
import me.smc.sb.utils.Utils;
import net.dv8tion.jda.entities.User;
import net.dv8tion.jda.entities.VoiceChannel;
import net.dv8tion.jda.events.message.MessageReceivedEvent;

public class VoiceCommand extends GlobalCommand{

	//make this not static since the object is only instantiated once?
	private static CustomFilePlayer player = null;
	private static LinkedList<File> loadedSongs;
	private static LinkedList<String> queuedSongs;
	private static int volume = 100;
	private static MessageReceivedEvent lastEv;
	private static String[] lastArgs;
	
	public VoiceCommand(){ //queue has separate command please
		super(null, 
			  " - Lets the bot join the requested voice channel", 
			  "{prefix}voice\nThis command makes the bot join the requested voice channel.\n\n" +
			  "----------\nUsage\n----------\n{prefix}voice join {channel} - Makes the bot join the voice channel\n" + 
			  "{prefix}voice leave - Makes the bot leave the current voice channel\n{prefix}voice play (link) - " +
			  "Plays either the first queued song, playlist or the link\n{prefix}voice pause - Pauses the current song\n" +
			  "{prefix}voice stop - Stops the player\n{prefix}voice volume {0-100} - Sets the volume of the player\n" +
			  "{prefix}voice queue {link} - Queues up a song in the player\n{prefix}voice clear - Clears the player's queue\n" +
			  "{prefix}voice skip - Skips the currently playing song" +
			  "\n\n----------\nAliases\n----------\n{prefix}v\n{prefix}music",  
			  false, 
			  "voice", "v", "music");
		loadedSongs = new LinkedList<>();
		queuedSongs = new LinkedList<>();
		lastEv = null;
		lastArgs = null;
	}

	@Override
	public void onCommand(MessageReceivedEvent e, String[] args){
		Utils.deleteMessage(e.getChannel(), e.getMessage());
		if(!Utils.checkArguments(e, args, 1)) return;
		
		switch(args[0]){
			case "join": joinVoiceChannel(e, args); break;
			case "leave": leaveVoiceChannel(e, args); break;
			case "play": startPlaying(e, args); break;
			case "pause": pausePlaying(e, args); break;
			case "stop": stopPlaying(e, args); break;
			case "volume": setVolume(e, args); break;
			case "queue": queueSong(e, args); break;
			case "clear": clearQueue(e, args); break;
			case "skip": skipSong(e, args); break;
		}
	}
	
	private void joinVoiceChannel(MessageReceivedEvent e, String[] args){
		if(!Utils.checkArguments(e, args, 2)) return;
		if(!Permissions.hasPerm(e.getAuthor(), e.getTextChannel(), Permissions.BOT_ADMIN)) return;
		
		VoiceChannel vChannel = null;
		
		String cName = "";
		
		for(int i = 1; i < args.length; i++)
			cName += args[i] + " ";
		
		for(VoiceChannel voice : e.getGuild().getVoiceChannels())
			if(voice.getName().equalsIgnoreCase(cName.substring(0, cName.length() - 1))){
				vChannel = voice;
			}
		
		if(vChannel == null){Utils.info(e.getChannel(), "This voice channel does not exist!"); return;}
		
		queuedSongs.clear();
		
		if(Main.api.getAudioManager().isConnected() && 
		   Main.api.getAudioManager().getConnectedChannel().getId().equalsIgnoreCase(vChannel.getId())){
			Utils.info(e.getChannel(), "Already connected to this channel!");
			return;
		}else if(Main.api.getAudioManager().isConnected()){
			if(player != null) player.stop(false);
			Main.api.getAudioManager().moveAudioConnection(vChannel);
		}else if(Main.api.getAudioManager().isAttemptingToConnect()){
			Main.api.getAudioManager().closeAudioConnection();
			Main.api.getAudioManager().openAudioConnection(vChannel);
		}else Main.api.getAudioManager().openAudioConnection(vChannel);
		
		wipeAllSongs();
	}
	
	private void leaveVoiceChannel(MessageReceivedEvent e, String[] args){
		if(!Permissions.hasPerm(e.getAuthor(), e.getTextChannel(), Permissions.BOT_ADMIN)) return;
	
		if(player != null) player.stop(false);
		Main.api.getAudioManager().closeAudioConnection();
		
		queuedSongs.clear();
		
		wipeAllSongs();
	}
	
	private void wipeAllSongs(){
		List<File> delete = new ArrayList<>();
		for(File f : new File("Songs/").listFiles())
			delete.add(f);
		for(File f : new ArrayList<File>(delete))
			f.delete();
	}
	
	private void queueSong(MessageReceivedEvent e, String[] args){
		if(!Utils.checkArguments(e, args, 2)) return;
		
		queuedSongs.add(args[1] + "||" + e.getAuthor().getId());
		
		if(player == null || (queuedSongs.size() == 1 && !player.isPlaying())) startPlaying(e, args);
		
		Utils.info(e.getChannel(), "Queued your song, there are currently " + (queuedSongs.size() <= 1 ? "no" : queuedSongs.size() - 1) + " songs ahead!");
	}
	
	private void clearQueue(MessageReceivedEvent e, String[] args){
		if(!Permissions.hasPerm(e.getAuthor(), e.getTextChannel(), Permissions.MANAGE_SERVER)) return;
		
		queuedSongs.clear();
		Utils.info(e.getChannel(), "Cleared the player's queue!");
	}
	
	private void skipSong(MessageReceivedEvent e, String[] args){
		if(!Permissions.hasPerm(e.getAuthor(), e.getTextChannel(), Permissions.MANAGE_SERVER)) return;
		if(!checkPlayer(e)) return;
		
		if(player != null) player.stop(false);
		startPlaying(e, args);
		
		Utils.info(e.getChannel(), "Skipping current song...");
	}
	
	private void startPlaying(MessageReceivedEvent e, String[] args){
		if(!Utils.checkArguments(e, args, 1)) return;
		if(!Permissions.hasPerm(e.getAuthor(), e.getTextChannel(), Permissions.VOICE_MUTE_MEMBERS)) return;
		if(!Main.api.getAudioManager().isConnected()){Utils.infoBypass(e.getChannel(), "You are not connected to a voice channel!"); return;}
		
		if(queuedSongs.size() == 0 && !player.isPaused()){Utils.infoBypass(e.getChannel(), "There is no song to play!"); return;}
		
		Thread t = new Thread(new Runnable(){
			@SuppressWarnings("deprecation")
			public void run(){
				if(player != null && player.isPaused()){
					player.play();
					Utils.info(e.getChannel(), "Player resumed!");
				}else if(player == null || player.isStopped()){
					File audioFile = null;
					
					lastEv = e;
					lastArgs = args;
					
					try{
						String song = queuedSongs.getFirst();
						queuedSongs.removeFirst();
						
						User requester = Main.api.getUserById(song.split("\\|\\|")[1]);
						audioFile = fetchMP3(e, song.split("\\|\\|")[0]);
							
						if(player == null) player = new CustomFilePlayer(audioFile);
						else player.setAudioFile(audioFile);
						
						player.setVolume((float) ((float) volume / 100.0));
							
						Main.api.getAudioManager().setSendingHandler(player);
							
						player.play();
							
						String songName = audioFile.getName().substring(0, audioFile.getName().length() - 4);
						songName = songName.split("~")[0];
						
						Utils.infoBypass(e.getChannel(), 
										"Now playing **" + songName + 
										"**\nAs requested by " + requester.getUsername());
					}catch(Exception ex){
						Log.logger.log(Level.SEVERE, ex.getMessage(), ex);
						Utils.infoBypass(e.getChannel(), "Could not start playing!\n" +
								                         "Error: " + ex.getMessage());
						if(audioFile != null){
							loadedSongs.remove(audioFile);
							if(audioFile != null) audioFile.delete();
						}
					}	
				}
				
				Thread.currentThread().stop();
			}
		});
		
		t.start();
	}
	
	public void nextSong(){
		if(queuedSongs.size() > 0) startPlaying(lastEv, lastArgs);
	}
	
	private File fetchMP3(MessageReceivedEvent e, String url) throws Exception{
		if(loadedSongs.size() >= 3) wipeOldestSong();
		
		if(!loadedSongs.isEmpty())
			for(File loaded : loadedSongs)
				if(loaded.getName().contains(url))
					return loaded;
		
		Process proc = Runtime.getRuntime().exec("youtube-dl --max-filesize 100m " +
								  			     "-o /home/discordbot/Songs/%(title)s~" + url.replaceAll("/", "|") + ".%(ext)s "
								  			     + url);
		proc.waitFor();
		
		File file = findNewSong(url.replaceAll("/", "|"));
		if(file == null) throw new Exception("Invalid file!");
		
		if(!songExists(file)) loadedSongs.add(file);
		
		return file;
	}
	
	private boolean songExists(File file){
		for(File f : loadedSongs)
			if(f.getName().equalsIgnoreCase(file.getName()))
				return true;
		return false;
	}
	
	private File findNewSong(String url){
		for(File f : new File("Songs").listFiles())
			if(f.getName().contains(url))
				return f;
		return null;
	}
	
	private void wipeOldestSong(){
		File file = loadedSongs.getFirst();
		loadedSongs.removeFirst();
		file.delete();
	}
	
	private void pausePlaying(MessageReceivedEvent e, String[] args){
		if(!Permissions.hasPerm(e.getAuthor(), e.getTextChannel(), Permissions.VOICE_MUTE_MEMBERS)) return;
		
		if(!checkPlayer(e)) return;

		player.pause();
		Utils.info(e.getChannel(), "Player paused!");
	}
	
	private void stopPlaying(MessageReceivedEvent e, String[] args){
		if(!Permissions.hasPerm(e.getAuthor(), e.getTextChannel(), Permissions.VOICE_MUTE_MEMBERS)) return;
		
		if(!checkPlayer(e)) return;
		
		player.stop(false);
		
		Utils.info(e.getChannel(), "Player stopped!");
	}
	
	private void setVolume(MessageReceivedEvent e, String[] args){
		if(!Permissions.hasPerm(e.getAuthor(), e.getTextChannel(), Permissions.VOICE_MUTE_MEMBERS)) return;
		if(!Utils.checkArguments(e, args, 2)) return;
		if(!checkPlayer(e)) return;
		if(Utils.stringToInt(args[1]) == -1){Utils.infoBypass(e.getChannel(), "The volume argument is invalid!"); return;}
		
		int volume = Utils.stringToInt(args[1]);
		
		if(volume > 100) volume = 100;
		else if(volume < 0) volume = 0;
		
		VoiceCommand.volume = volume;
		player.setVolume((float) ((float) volume / 100.0));
		
		Utils.info(e.getChannel(), "Player's volume set to " + volume + "%!");
	}
	
	private boolean checkPlayer(MessageReceivedEvent e){
		if(player == null){
			Utils.infoBypass(e.getChannel(), "The player isn't started!");
			return false;
		}
		return true;
	}
	
}
