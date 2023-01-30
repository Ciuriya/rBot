package me.smc.sb.discordcommands;

import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;

import me.smc.sb.audio.CustomAudioLoadResultHandler;
import me.smc.sb.audio.GuildMusicManager;
import me.smc.sb.main.Main;
import me.smc.sb.perm.Permissions;
import me.smc.sb.utils.Configuration;
import me.smc.sb.utils.Utils;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.VoiceChannel;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.managers.AudioManager;

public class VoiceCommand extends GlobalCommand{

	public final AudioPlayerManager playerManager;
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
			  "\n{prefix}voice perm-lock {@roles} - Only allows usage of voice commands to the mentioned roles." +
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
		String permLock = Main.serverConfigs.get(e.getGuild().getId()).getValue("voice-perm-lock");
		
		if(permLock.length() > 0 && !Permissions.check(e.getAuthor(), Permissions.BOT_ADMIN)){
			boolean allowed = false;
			
			for(String role : permLock.split(","))
				if(e.getGuild().getMembersWithRoles(e.getGuild().getRolesByName(role, true)).contains(e.getMember())){
					allowed = true;
					break;
				}
			
			if(!allowed) return;
		}
		
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
				
				Main.serverConfigs.get(e.getGuild().getId()).writeValue("voice-state", "playing");
				
				break;
			case "pause": 
				getGuildAudioPlayer(e.getChannel(), e.getGuild()).player.setPaused(true);
				
				Main.serverConfigs.get(e.getGuild().getId()).writeValue("voice-state", "paused");
				
				Utils.info(e.getChannel(), "Player paused!");
				break;
			case "stop": 
				GuildMusicManager gmm = getGuildAudioPlayer(e.getChannel(), e.getGuild());
				
				gmm.player.stopTrack();
				
				gmm.scheduler.updateNP(null);
				
				Main.serverConfigs.get(e.getGuild().getId()).writeValue("voice-state", "stopped");
				
				Utils.info(e.getChannel(), "Player stopped!");
				break;
			case "volume": 
				int volume = Utils.stringToInt(args[1]);
				
				if(volume != -1){
					getGuildAudioPlayer(e.getChannel(), e.getGuild()).player.setVolume(volume);
					
					Main.serverConfigs.get(e.getGuild().getId()).writeValue("voice-volume", volume);
					
					Utils.info(e.getChannel(), "Player set to " + volume + "% volume!");
				}
				
				break;
			case "queue": queueSong(e, args); break;
			case "clear": 
				getGuildAudioPlayer(e.getChannel(), e.getGuild()).scheduler.clear();
				
				Utils.info(e.getChannel(), "Player queue cleared!");
				break;
			case "skip": skipSong(e, args); break;
			case "np":
				if(args.length < 2){
					Main.serverConfigs.get(e.getGuild().getId()).writeValue("voice-np-ip", "");
					return;
				}
				
				Main.serverConfigs.get(e.getGuild().getId()).writeValue("voice-np-ip", args[1]);
				break;
			case "perm-lock":			
				String roles = "";
				
				for(Role role : e.getMessage().getMentions().getRoles())
					roles += "," + role.getName();
				
				if(roles == "") roles = "a";
					
				Main.serverConfigs.get(e.getGuild().getId()).writeValue("voice-perm-lock", roles.substring(1));
				
				Utils.info(e.getChannel(), "The voice commands are now only available to the mentioned roles!");
				break;
		}
	}
	
	public synchronized GuildMusicManager getGuildAudioPlayer(MessageChannelUnion channel, Guild guild){
		GuildMusicManager musicManager = players.get(guild.getId());

		if(musicManager == null){
			musicManager = new GuildMusicManager(playerManager);
			musicManager.player.setVolume(35);
			players.put(guild.getId(), musicManager);
		}

		musicManager.scheduler.setMessageChannel(channel.getType().isGuild() ? channel : null);
		guild.getAudioManager().setSendingHandler(musicManager.getSendHandler());

		return musicManager;
	}
	
	private void joinVoiceChannel(MessageReceivedEvent e, String[] args){
		if(!Utils.checkArguments(e, args, 2)) return;
		
		String cName = "";
		
		for(int i = 1; i < args.length; i++)
			cName += args[i] + " ";
		
		joinVoiceChannel(e.getGuild(), e.getChannel(), cName.substring(0, cName.length() - 1));
	}
	
	public void joinVoiceChannel(Guild guild, MessageChannelUnion channel, String name){
		VoiceChannel vChannel = null;
		
		vChannel = guild.getVoiceChannelsByName(name, true).stream().findFirst().orElse(null);
		
		if(vChannel == null){
			Utils.info(channel, "This voice channel does not exist!"); 
			return;
		}
		
		
		AudioManager audioManager = guild.getAudioManager();
		GuildMusicManager music = getGuildAudioPlayer(channel, guild);
		
		if(audioManager.isConnected() && audioManager.getConnectedChannel().getId().equalsIgnoreCase(vChannel.getId())){
			return;
		}else if(audioManager.isConnected()){
			music.player.setPaused(true);
			audioManager.closeAudioConnection();
			audioManager.openAudioConnection(vChannel);
			music.player.setPaused(false);
		}else audioManager.openAudioConnection(vChannel);
		
		Main.serverConfigs.get(guild.getId()).writeValue("voice-channel", vChannel.getName());
		
		if(players.containsKey(guild.getId()))
			if(players.get(guild.getId()).player.isPaused()){
				players.get(guild.getId()).player.setPaused(false);
				Main.serverConfigs.get(guild.getId()).writeValue("voice-state", "playing");
			}
	}
	
	private void leaveVoiceChannel(MessageReceivedEvent e, String[] args){
		if(players.containsKey(e.getGuild().getId()))
			players.get(e.getGuild().getId()).player.setPaused(true);

		Main.serverConfigs.get(e.getGuild().getId()).writeValue("voice-state", "");
		Main.serverConfigs.get(e.getGuild().getId()).writeValue("voice-channel", "");
		
		e.getGuild().getAudioManager().closeAudioConnection();
	}
	
	private void queueSong(MessageReceivedEvent e, String[] args){
		if(!Utils.checkArguments(e, args, 2)) return;
		
		GuildMusicManager music = getGuildAudioPlayer(e.getChannel(), e.getGuild());
		
		boolean random = args.length >= 3 && args[2].equalsIgnoreCase("true") ? true : false;
		
		if(music.scheduler.size() >= 10){
			Utils.infoBypass(e.getChannel(), "Cannot queue more than 10 elements at once!");
			return;
		}
		
		playerManager.loadItemOrdered(music, args[1], new CustomAudioLoadResultHandler(e.getChannel(), e.getGuild(), args[1], random, false, this));
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
	
	public static void loadRadio(Guild guild, Configuration config, boolean retry){
		if(config.getStringList("voice-queue").size() > 0 ||
		   config.getValue("voice-current").length() > 0 ||
		   config.getValue("voice-state").length() > 0){
			VoiceCommand voice = null;
			
			for(GlobalCommand gc : GlobalCommand.commands)
				if(gc != null && gc instanceof VoiceCommand){
					voice = (VoiceCommand) gc;
					break;
				}
			
			if(voice != null){
				String textChannel = config.getValue("voice-text-channel");
				
				if(textChannel.length() == 0) return;

				GuildMusicManager music = voice.getGuildAudioPlayer(guild.getChannelById(MessageChannelUnion.class, textChannel), guild);
				int volume = Utils.stringToInt(config.getValue("voice-volume"));
				
				if(volume == -1) volume = 35;
				music.player.setVolume(volume);
				music.scheduler.loadScheduling(voice, music, guild.getChannelById(MessageChannelUnion.class, textChannel), config);
			}else if(!retry){
				new Timer().schedule(new TimerTask(){
					public void run(){
						loadRadio(guild, config, true);
					}
				}, 1500);
			}
		}
	}
	
	public static void saveAllRadios(){
		for(String server : players.keySet()){
			players.get(server).scheduler.saveScheduling(Main.serverConfigs.get(server));
			players.get(server).player.setPaused(true);
		}
	}
}
