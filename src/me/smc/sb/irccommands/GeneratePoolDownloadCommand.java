package me.smc.sb.irccommands;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.pircbotx.PircBotX;
import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.PrivateMessageEvent;

import me.smc.sb.multi.Map;
import me.smc.sb.multi.MapPool;
import me.smc.sb.multi.Tournament;
import me.smc.sb.perm.Permissions;
import me.smc.sb.utils.Log;
import me.smc.sb.utils.Utils;

public class GeneratePoolDownloadCommand extends IRCCommand{

	public GeneratePoolDownloadCommand(){
		super("Generates a zip download for the map pool.",
			  "<tournament name> <map pool num> ",
			  Permissions.IRC_BOT_ADMIN,
			  "pooldl");
	}
	
	@Override
	public String onCommand(MessageEvent<PircBotX> e, PrivateMessageEvent<PircBotX> pe, String discord, String[] args){
		String argCheck = Utils.checkArguments(args, 2);
		if(argCheck.length() > 0) return argCheck;
		
		String tournamentName = "";
		
		for(int i = 0; i < args.length - 1; i++) tournamentName += args[i] + " ";
		Tournament t = Tournament.getTournament(tournamentName.substring(0, tournamentName.length() - 1));
		
		if(t == null) return "Invalid tournament!";
		if(Utils.stringToInt(args[args.length - 1]) == -1) return "Map pool number needs to be a number!";
		
		MapPool pool = t.getPool(Utils.stringToInt(args[args.length - 1]));
		
		Thread thread = new Thread(new Runnable(){
			@SuppressWarnings("deprecation")
			public void run(){
				File dlFolder = new File("temp||" + t.getName() + "||pool||" + pool.getPoolNum());
				dlFolder.mkdir();
				
				List<File> mapsDownloaded = new ArrayList<>();
				
				Utils.Login.osu();
				
				for(Map map : pool.getMaps())
					if(map.getBeatmapID() != 0){
						File mapFile = downloadMap(dlFolder, map);
						
						if(mapFile != null && mapFile.exists()) mapsDownloaded.add(mapFile);
					}
				
				if(mapsDownloaded.isEmpty()) Utils.info(e, pe, discord, "Could not download any maps!");
				else{
					zipPackage(dlFolder, "/var/www/html/s/" + t.getName() + "-" + pool.getPoolNum() + ".zip");
					
					try{
						String url = "http://smcmax.com/s/" + URLEncoder.encode(t.getName() + "-" + pool.getPoolNum(), "UTF-8").replaceAll("\\+", "%20") + ".zip";
						Utils.info(e, pe, discord, "Here is the zipped map pool: " + url);
					}catch(Exception ex){
						Utils.info(e, pe, discord, "Could not zip the package: " + ex.getMessage());
					}
				}
				
				Thread.currentThread().stop();
			}
		});
		thread.start();
		
		return "";
	}
	
	private File downloadMap(File parent, Map map){
		String[] html = Utils.getHTMLCode(map.getURL());

		ArrayList<String> line = Utils.getNextLineCodeFromLink(html, 0, "playBeatmapPreview");
		if(line.isEmpty()) return null;

		int setID = Utils.stringToInt(line.get(0).split("playBeatmapPreview\\(")[1].split("\\); return")[0]);
		if(setID == -1) return null;
		
		String url = "https://osu.ppy.sh/d/" + setID + "n";
		
		url = Utils.getFinalURL(url);

		URLConnection connection = establishConnection(url);

		boolean bloodcat = false;

		if(connection.getContentLength() <= 100){
			connection = establishConnection(Utils.getFinalURL("https://osu.ppy.sh/d/" + setID));

			if(connection.getContentLength() <= 100){
				connection = establishConnection(Utils.getFinalURL("http://bloodcat.com/osu/s/" + setID));
				bloodcat = true;
			}
		}

		String location = downloadFromURL(connection, parent, setID);
		
		if(!oszContainsAudio(location)){
			if(!bloodcat) location = downloadFromURL(establishConnection(Utils.getFinalURL("https://osu.ppy.sh/d/" + setID)), parent, setID);
			else return null;
		}
		
		return new File(location);
	}
	
	private static String downloadFromURL(URLConnection connection, File parent, int setID){
		try{
			InputStream in = connection.getInputStream();
			FileOutputStream out = new FileOutputStream(parent.getAbsolutePath() + File.separator + setID + ".osz");

			byte[] b = new byte[4096];
			int count;

			while((count = in.read(b)) >= 0)
				out.write(b, 0, count);

			in.close();
			out.close();
			return parent.getAbsolutePath() + File.separator + setID + ".osz";
		}catch(Exception e){
			e.printStackTrace();
			return null;
		}
	}
	
	private static boolean oszContainsAudio(String oszLocation){
		try{
			FileInputStream fis = new FileInputStream(oszLocation);
			ZipInputStream zis = new ZipInputStream(fis);
			
			while(zis.available() == 1){
				ZipEntry entry = zis.getNextEntry();
				
				if(entry.getName().endsWith(".mp3")){
					zis.close();
					fis.close();
					return true;
				}
				
				zis.closeEntry();
			}
			
			zis.close();
			fis.close();
		}catch(Exception e){
			e.printStackTrace();
		}
		
		return false;
	}
	
	private URLConnection establishConnection(String url){
		URLConnection connection = null;
		
		try{
			connection = new URL(url).openConnection();
		}catch(Exception e){
			Log.logger.log(Level.SEVERE, e.getMessage(), e);
		}
		
        connection.setDoOutput(true);
        connection.setDoInput(true);
        connection.setRequestProperty("User-Agent", "Mozilla/5.0");
        connection.setRequestProperty("content-type", "binary/data");
        return connection;
	}
	
	private void zipPackage(File parent, String zipLocation){
		byte[] b = new byte[1024];
		
		try{
			if(new File(zipLocation).exists()) new File(zipLocation).delete();
			
			FileOutputStream fos = new FileOutputStream(zipLocation);
			ZipOutputStream zos = new ZipOutputStream(fos);
			
			for(File file : new ArrayList<File>(Arrays.asList(parent.listFiles()))){
				ZipEntry ze = new ZipEntry(file.getName());
				zos.putNextEntry(ze);
				
				FileInputStream in = new FileInputStream(file.getAbsolutePath());
				
				int count;
		        while((count = in.read(b)) >= 0)
		        	zos.write(b, 0, count);
		        
		        in.close();
		        
		        file.delete();
			}
			
			zos.closeEntry();
			zos.close();
			
			parent.delete();
		}catch(Exception e){
			Log.logger.log(Level.SEVERE, e.getMessage(), e);
		}
	}
	
}
