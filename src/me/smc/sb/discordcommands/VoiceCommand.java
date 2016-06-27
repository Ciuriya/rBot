package me.smc.sb.discordcommands;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;

import me.smc.sb.main.Main;
import me.smc.sb.utils.CustomFilePlayer;
import me.smc.sb.utils.Log;
import me.smc.sb.utils.Utils;
import net.dv8tion.jda.entities.Guild;
import net.dv8tion.jda.entities.User;
import net.dv8tion.jda.entities.VoiceChannel;
import net.dv8tion.jda.events.message.MessageReceivedEvent;

public class VoiceCommand extends GlobalCommand{

	private static HashMap<String, CustomFilePlayer> players;
	
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
		players = new HashMap<>();
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
		
		VoiceChannel vChannel = null;
		
		String cName = "";
		
		for(int i = 1; i < args.length; i++)
			cName += args[i] + " ";
		
		for(VoiceChannel voice : e.getGuild().getVoiceChannels())
			if(voice.getName().equalsIgnoreCase(cName.substring(0, cName.length() - 1))){
				vChannel = voice;
			}
		
		if(vChannel == null){Utils.info(e.getChannel(), "This voice channel does not exist!"); return;}
		
		if(!players.containsKey(e.getGuild().getId())){
			CustomFilePlayer player = new CustomFilePlayer(e.getGuild());
			player.stop(false);
			players.put(e.getGuild().getId(), player);
		}
		
		players.get(e.getGuild().getId()).getQueuedSongs().clear();
		
		if(Main.api.getAudioManager(e.getGuild()).isConnected() && 
		   Main.api.getAudioManager(e.getGuild()).getConnectedChannel().getId().equalsIgnoreCase(vChannel.getId())){
			Utils.info(e.getChannel(), "Already connected to this channel!");
			return;
		}else if(Main.api.getAudioManager(e.getGuild()).isConnected()){
			if(players.containsKey(e.getGuild().getId())) players.get(e.getGuild().getId()).stop(false);
			Main.api.getAudioManager(e.getGuild()).moveAudioConnection(vChannel);
		}else if(Main.api.getAudioManager(e.getGuild()).isAttemptingToConnect()){
			Main.api.getAudioManager(e.getGuild()).closeAudioConnection();
			Main.api.getAudioManager(e.getGuild()).openAudioConnection(vChannel);
		}else Main.api.getAudioManager(e.getGuild()).openAudioConnection(vChannel);
		
		wipeAllSongs(e.getGuild());
	}
	
	private void leaveVoiceChannel(MessageReceivedEvent e, String[] args){
		if(players.containsKey(e.getGuild().getId())) players.get(e.getGuild().getId()).stop(false);
		Main.api.getAudioManager(e.getGuild()).closeAudioConnection();
		
		players.get(e.getGuild().getId()).getQueuedSongs().clear();
		
		wipeAllSongs(e.getGuild());
	}
	
	private void wipeAllSongs(Guild guild){
		List<File> delete = new ArrayList<>();
		if(!new File("Songs/" + guild.getId()).exists())
			new File("Songs/" + guild.getId()).mkdir();
			
		for(File f : new File("Songs/" + guild.getId()).listFiles())
			delete.add(f);
		for(File f : new ArrayList<File>(delete))
			f.delete();
	}
	
	private void queueSong(MessageReceivedEvent e, String[] args){
		if(!Utils.checkArguments(e, args, 2)) return;
		
		players.get(e.getGuild().getId()).addQueuedSong(args[1] + "||" + e.getAuthor().getId());
		
		int queuedSongsSize = players.get(e.getGuild().getId()).getQueuedSongs().size();
		
		if(players.containsKey(e.getGuild().getId()) || (queuedSongsSize == 1 && 
		   !players.get(e.getGuild().getId()).isPlaying() && !players.get(e.getGuild().getId()).isConverting())) 
			startPlaying(e, args);
		
		players.get(e.getGuild().getId()).setLastEvent(e);
		players.get(e.getGuild().getId()).setLastArgs(args);
		
		Utils.info(e.getChannel(), "Queued your song, there are currently " + (queuedSongsSize <= 1 ? "no" : queuedSongsSize - 1) + " songs ahead!");
	}
	
	private void clearQueue(MessageReceivedEvent e, String[] args){
		players.get(e.getGuild().getId()).getQueuedSongs().clear();
		Utils.info(e.getChannel(), "Cleared the player's queue!");
	}
	
	private void skipSong(MessageReceivedEvent e, String[] args){
		if(!checkPlayer(e)) return;
		
		if(players.containsKey(e.getGuild().getId())) players.get(e.getGuild().getId()).stop(false);
		startPlaying(e, args);
		
		Utils.info(e.getChannel(), "Skipping current song...");
	}
	
	private void startPlaying(MessageReceivedEvent e, String[] args){
		if(!Utils.checkArguments(e, args, 1)) return;
		if(!Main.api.getAudioManager(e.getGuild()).isConnected()){Utils.infoBypass(e.getChannel(), "You are not connected to a voice channel!"); return;}
		
		Thread t = new Thread(new Runnable(){
			@SuppressWarnings("deprecation")
			public void run(){
				if(players.containsKey(e.getGuild().getId()) && players.get(e.getGuild().getId()).isPaused()){
					players.get(e.getGuild().getId()).play();
					Utils.info(e.getChannel(), "Player resumed!");
				}else if(players.get(e.getGuild().getId()).isStopped()){
					File audioFile = null;
					CustomFilePlayer player = players.get(e.getGuild().getId());
					
					if(player.isConverting()) return;
					
					try{
						player.setConverting(true);
						
						String song = player.getQueuedSongs().getFirst();
						player.removeQueuedSong();
						
						User requester = Main.api.getUserById(song.split("\\|\\|")[1]);
						audioFile = fetchMP3(e, song.split("\\|\\|")[0]);
							
						player.setAudioFile(audioFile);
							
						Main.api.getAudioManager(e.getGuild()).setSendingHandler(player);
							
						player.play();
							
						String songName = audioFile.getName().substring(0, audioFile.getName().length() - 4);
						songName = songName.split("~")[0];
						
						Utils.infoBypass(e.getChannel(), 
										"Now playing **" + songName + 
										"**\nAs requested by " + requester.getUsername());
						
						player.setConverting(false);
					}catch(Exception ex){
						player.setConverting(false);
						Log.logger.log(Level.SEVERE, ex.getMessage(), ex);
						Utils.infoBypass(e.getChannel(), "Could not start playing!\n" +
								                         "Error: " + ex.getMessage());
						if(audioFile != null){
							player.getLoadedSongs().remove(audioFile);
							if(audioFile != null) audioFile.delete();
						}
					}	
				}
				
				Thread.currentThread().stop();
			}
		});
		
		t.start();
	}
	
	public void nextSong(Guild guild){
		if(players.get(guild.getId()).getQueuedSongs().size() > 0) 
			startPlaying(players.get(guild.getId()).getLastEvent(), players.get(guild.getId()).getLastArgs());
	}
	
	private File fetchMP3(MessageReceivedEvent e, String url) throws Exception{
		CustomFilePlayer player = players.get(e.getGuild().getId());
		
		if(player.getLoadedSongs().size() >= 3) wipeOldestSong(player);
		
		if(!player.getLoadedSongs().isEmpty())
			for(File loaded : player.getLoadedSongs())
				if(loaded.getName().contains(url))
					return loaded;
		
		Process proc = Runtime.getRuntime().exec("youtube-dl --max-filesize 100m " +
								  			     "-o /home/discordbot/Songs/" + player.getGuild().getId() + "/%(title)s~" + 
								  			     url.replaceAll("/", "|") + ".%(ext)s " + url);
		proc.waitFor();
		
		File file = findNewSong(url.replaceAll("/", "|"), player);
		if(file == null) throw new Exception("Invalid file!");
		
		if(!songExists(file, player)) player.getLoadedSongs().add(file);
		
		return file;
	}
	
	private boolean songExists(File file, CustomFilePlayer player){
		for(File f : player.getLoadedSongs())
			if(f.getName().equalsIgnoreCase(file.getName()))
				return true;
		return false;
	}
	
	private File findNewSong(String url, CustomFilePlayer player){
		for(File f : new File("Songs/" + player.getGuild().getId()).listFiles())
			if(f.getName().contains(url))
				return f;
		return null;
	}
	
	private void wipeOldestSong(CustomFilePlayer player){
		File file = player.getLoadedSongs().getFirst();
		player.getLoadedSongs().removeFirst();
		file.delete();
	}
	
	private void pausePlaying(MessageReceivedEvent e, String[] args){
		if(!checkPlayer(e)) return;

		players.get(e.getGuild().getId()).pause();
		Utils.info(e.getChannel(), "Player paused!");
	}
	
	private void stopPlaying(MessageReceivedEvent e, String[] args){
		if(!checkPlayer(e)) return;
		
		players.get(e.getGuild().getId()).stop(false);
		
		Utils.info(e.getChannel(), "Player stopped!");
	}
	
	private void setVolume(MessageReceivedEvent e, String[] args){
		if(!Utils.checkArguments(e, args, 2)) return;
		if(!checkPlayer(e)) return;
		if(Utils.stringToInt(args[1]) == -1){Utils.infoBypass(e.getChannel(), "The volume argument is invalid!"); return;}
		
		int volume = Utils.stringToInt(args[1]);
		
		if(volume > 100) volume = 100;
		else if(volume < 0) volume = 0;
		
		players.get(e.getGuild().getId()).setVolume(volume);
		
		Utils.info(e.getChannel(), "Player's volume set to " + volume + "%!");
	}
	
	private boolean checkPlayer(MessageReceivedEvent e){
		if(!players.containsKey(e.getGuild().getId())){
			Utils.infoBypass(e.getChannel(), "The player isn't started!");
			return false;
		}
		
		return true;
	}
	
}
