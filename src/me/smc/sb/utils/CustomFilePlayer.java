package me.smc.sb.utils;

import java.io.File;
import java.util.LinkedList;

import me.smc.sb.discordcommands.GlobalCommand;
import me.smc.sb.discordcommands.VoiceCommand;
import net.dv8tion.jda.audio.player.FilePlayer;
import net.dv8tion.jda.entities.Guild;
import net.dv8tion.jda.events.message.MessageReceivedEvent;

public class CustomFilePlayer extends FilePlayer{

	private Guild guild;
	private LinkedList<File> loadedSongs;
	private LinkedList<String> queuedSongs;
	private MessageReceivedEvent lastEv;
	private String[] lastArgs;
	private boolean isConverting;
	
	public CustomFilePlayer(Guild guild){
		super();
		
		this.guild = guild;
		loadedSongs = new LinkedList<>();
		queuedSongs = new LinkedList<>();
	}
	
	public Guild getGuild(){
		return guild;
	}
	
	public MessageReceivedEvent getLastEvent(){
		return lastEv;
	}
	
	public String[] getLastArgs(){
		return lastArgs;
	}
	
	public boolean isConverting(){
		return isConverting;
	}
	
	public void setLastEvent(MessageReceivedEvent e){
		lastEv = e;
	}
	
	public void setLastArgs(String[] args){
		lastArgs = args;
	}
	
	public void setConverting(boolean converting){
		isConverting = converting;
	}
	
	public void addLoadedSong(File file){
		loadedSongs.add(file);
	}
	
	public void addQueuedSong(String queuedSong){
		queuedSongs.add(queuedSong);
	}
	
	public void removeQueuedSong(){
		queuedSongs.remove();
	}
	
	public LinkedList<File> getLoadedSongs(){
		return loadedSongs;
	}
	
	public LinkedList<String> getQueuedSongs(){
		return queuedSongs;
	}
	
	public void setVolume(int volume){
		this.setVolume((float) ((float) volume / 100.0));
	}
	
	@Override
	public void stop(){
		super.stop();
		
		for(GlobalCommand gc : GlobalCommand.commands)
			if(gc.isName("voice")){
				((VoiceCommand) gc).nextSong(guild);
				break;
			}
	}
	
	public void stop(boolean next){
		super.stop();
		
		if(next){
			for(GlobalCommand gc : GlobalCommand.commands)
				if(gc.isName("voice")){
					((VoiceCommand) gc).nextSong(guild);
					break;
				}	
		}
	}
	
}
