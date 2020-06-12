package me.smc.sb.discordcommands;

import java.io.File;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;

import com.google.common.collect.Lists;

import me.smc.sb.perm.Permissions;
import me.smc.sb.utils.Log;
import me.smc.sb.utils.Utils;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class YoutubeDownloadCommand extends GlobalCommand{

	public YoutubeDownloadCommand(){
		super(Permissions.BOT_ADMIN, 			  
			  " - Downloads a song from youtube and serves it as an mp3 file.", 
			  "{prefix}ydl\nDownloads linked song from youtube and uploads it as mp3.\n\n" +
			  "----------\nUsage\n----------\n{prefix}ydl {youtube link} - Downloads song from youtube.\n\n" + 
			  "----------\nAliases\n----------\nThere are no aliases.", 
			  true, 
			  "ydl");
		
		File[] ydls = new File("/var/www/html/ydl").listFiles();
		if(ydls.length > 0)
			for(File f : Lists.newArrayList(ydls))
				f.delete();
	}

	@Override
	public void onCommand(MessageReceivedEvent e, String[] args){
		if(!Utils.checkArguments(e, args, 1)) return;
		
		File downloadedSong = null;
		
		try{
			Process proc = Runtime.getRuntime().exec("youtube-dl " +
	 			     "-o /home/discordbot/Songs/%(title)s~" + args[0].replaceAll("/", "|") + ".%(ext)s "
	 			     + "-x --audio-format mp3 " + args[0]);
			proc.waitFor();	
			
			
			
			for(File f : new File("Songs").listFiles())
				if(f.getName().contains(args[0].replaceAll("/", "|"))){
					downloadedSong = f;
					break;
				}
			
			if(downloadedSong == null) throw new Exception("Invalid file!");
			
			String newName = downloadedSong.getName().replace("~" + args[0].replaceAll("/", "|"), "");
			
			downloadedSong.renameTo(new File("/var/www/html/ydl/" + newName));
			
			Utils.infoBypass(e.getChannel(), "You have one hour to download the song!\nIf it only loads the song for playback, please use CTRL+S to save it!\n" +
					                         "```http://smcmax.com/ydl/" + newName + "```");
			
			Timer t = new Timer();
			t.schedule(new TimerTask(){
				public void run(){
					File file = new File("/var/www/html/ydl/" + newName);
					
					if(file != null && file.exists())
						file.delete();
				}
			}, 3600000);
		}catch(Exception ex){
			Log.logger.log(Level.SEVERE, ex.getMessage(), ex);
			Utils.infoBypass(e.getChannel(), "Could not download from youtube!\n" +
                    "Error: " + ex.getMessage());
			if(downloadedSong != null) downloadedSong.delete();
		}
	}

}
