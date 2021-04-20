package me.smc.sb.discordcommands;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.logging.Level;

import me.smc.sb.perm.Permissions;
import me.smc.sb.utils.Log;
import me.smc.sb.utils.Utils;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class DownloadOsuMapCommand extends GlobalCommand{

	public DownloadOsuMapCommand(){
		super(Permissions.BOT_ADMIN, 
			  " - Downloads an osu! map using a given url", 
			  "{prefix}dlosu\nThis command downloads a mapset using the url\n\n" +
			  "----------\nUsage\n----------\n{prefix}dlosu {link} - Downloads the mapset\n\n" + 
			  "----------\nAliases\n----------\nThere are no aliases.",  
			  true, 
			  "dlosu");
	}

	@Override
	public void onCommand(MessageReceivedEvent e, String[] args){
		if(!Utils.checkArguments(e, args, 1)) return;
		
		try{
			String link = args[0];
			
			link = Utils.getFinalURL(link);
			
			URLConnection connection = null;
			
			connection = new URL(link).openConnection();
	        
	        File parent = new File("temp-dl");
	        parent.mkdir();
	        
			InputStream in = connection.getInputStream();
			FileOutputStream out = new FileOutputStream(parent.getAbsolutePath() + File.separator + "test-dl.osz");

			byte[] b = new byte[4096];
			int count;

			while((count = in.read(b)) >= 0)
				out.write(b, 0, count);

			in.close();
			out.close();
			
			File set = new File(parent.getAbsolutePath() + File.separator + "test-dl.osz");
			
			Utils.info(e.getChannel(), "Downloaded the file from " + link);
			
			if(args.length > 1) set.delete();
		}catch(Exception ex){
			Log.logger.log(Level.SEVERE, ex.getMessage(), ex);
		}
	}
	
}
