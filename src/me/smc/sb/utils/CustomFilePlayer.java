package me.smc.sb.utils;

import java.io.File;
import java.io.IOException;

import javax.sound.sampled.UnsupportedAudioFileException;

import me.smc.sb.discordcommands.GlobalCommand;
import me.smc.sb.discordcommands.VoiceCommand;
import net.dv8tion.jda.audio.player.FilePlayer;

public class CustomFilePlayer extends FilePlayer{

	public CustomFilePlayer(File audio) throws IOException, UnsupportedAudioFileException{
		super(audio);
	}
	
	@Override
	public void stop(){
		super.stop();
		
		for(GlobalCommand gc :GlobalCommand.commands)
			if(gc.isName("voice")){
				((VoiceCommand) gc).nextSong();
				break;
			}
	}
	
	public void stop(boolean next){
		super.stop();
		
		if(next){
			for(GlobalCommand gc :GlobalCommand.commands)
				if(gc.isName("voice")){
					((VoiceCommand) gc).nextSong();
					break;
				}	
		}
	}
	
}
