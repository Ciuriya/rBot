package me.smc.sb.tracking;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;

import me.smc.sb.main.Main;
import me.smc.sb.utils.Log;
import me.smc.sb.utils.Utils;

public class TrackingUtils{

	public static String convertMode(int mode){
		switch(mode){
			case 0: return "Standard";
			case 1: return "Taiko";
			case 2: return "CtB";
			case 3: return "Mania";
			default: return "Unknown";
		}
	}
	
	public static String analyzeMapStatus(int code){
		switch(code){
			case -2: return "Graveyard";
			case -1: return "WIP";
			case 0: return "Pending";
			case 1: return "Ranked";
			case 2: return "Approved";
			case 3: return "Qualified";
			case 4: return "Loved";
			default: return "Unsubmitted";
		}
	}
	
	public static String escapeCharacters(String toEscape){
		return toEscape.replaceAll("\\*", "\\*");
	}
	
	public static boolean playerHasRecentPlays(JSONArray response, CustomDate lastUpdate){
		boolean valid = false;
		
		for(int i = 0; i < response.length(); i++){
			JSONObject play = response.getJSONObject(i);
			CustomDate date = new CustomDate(play.getString("date"));
			
			if(date.after(lastUpdate)){
				valid = true;
				break;
			}
		}
		
		return valid;
	}
	
	public static boolean playerHasRecentPlays(int userId, int mode, CustomDate lastUpdate){
		// loading recent plays history from profile to see if the player has done anything recently
		// this avoids spamming the api to check, note that this is called a LOT.
		String[] pageHistory = Main.htmlRegulator.sendRequest("https://osu.ppy.sh/pages/include/profile-history.php?u=" + userId + "&m=" + mode);
		
		// if no plays, go back
		if(pageHistory.length == 0 || !pageHistory[0].contains("<div class='profileStatHeader'>Recent Plays (last 24h):"))
			return false;
		
		String[] splitTime = pageHistory[0].split("<\\/time>");
		
		boolean valid = false;
		
		// for every play, check if date is worth posting (recent enough)
		for(int i = 0; i < splitTime.length - 1; i++){
			CustomDate date = new CustomDate(splitTime[i].split("time class=")[1].split(">")[1].replace(" UTC", ""));
			
			if(date.after(lastUpdate)){
				valid = true;
				break;
			}
		}
		
		return valid;
	}
	
	public static List<RecentPlay> fetchPlayerRecentPlays(String[] html, CustomDate lastUpdate){
		List<RecentPlay> recentPlays = new ArrayList<>();
		
		if(Utils.getNextLineCodeFromLink(html, 0, "This user hasn't done anything notable recently!").size() == 0){
			List<String> list = Utils.getNextLineCodeFromLink(html, 0, "<div class='profileStatHeader'>Recent Activity</div>");
			
			if(list.size() != 0){
				String line = list.get(0);
				String[] plays = line.split("<tr>");
				
				for(int i = 1; i < plays.length; i++){
					if(plays[i].contains("achieved") && plays[i].contains("rank #")){
						CustomDate date = new CustomDate(plays[i].split("UTC")[0].split("Z'>")[1]);
						int rank = Utils.stringToInt(plays[i].split("rank #")[1].split(" on")[0].replace("</b>", ""));
						int beatmapId = Utils.stringToInt(plays[i].split("href='\\/b\\/")[1].split("\\?m=")[0]);
						
						RecentPlay play = new RecentPlay(beatmapId, date, rank);
						
						boolean equal = false;
						
						if(recentPlays.size() > 0)
							for(RecentPlay r : recentPlays)
								if(r.eq(play))
									equal = true;
						
						if(equal) continue;
						
						if(date.after(lastUpdate)) recentPlays.add(play);
					}
				}
			}
		}
		
		return recentPlays;
	}
	
	public static String osuDateToCurrentDate(String sDate){
		DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		formatter.setTimeZone(TimeZone.getTimeZone("Asia/Singapore"));
		
		Date date = null;
		
		try{
			date = formatter.parse(sDate);
		}catch(ParseException e){
			Log.logger.log(Level.SEVERE, e.getMessage(), e);
		}
		
		formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
		return formatter.format(date);
	}
	
	public static double getAccuracy(JSONObject play, int mode){
		int totalHits = 0;
		int points = 0;
		
		switch(mode){
			case 0:
				totalHits = play.getInt("count300") + play.getInt("count100") + play.getInt("count50") + play.getInt("countmiss");
				points = play.getInt("count300") * 300 + play.getInt("count100") * 100 + play.getInt("count50") * 50;
				
				totalHits *= 300;
				
				return ((double) points / (double) totalHits) * 100;
			case 1:
				totalHits = play.getInt("countmiss") + play.getInt("count100") + play.getInt("count300");
				double dPoints = play.getInt("count100") * 0.5 + play.getInt("count300");
				
				dPoints *= 300;
				totalHits *= 300;
				
				return (dPoints / (double) totalHits) * 100;
			case 2:
				int caught = play.getInt("count50") + play.getInt("count100") + play.getInt("count300");
				int fruits = play.getInt("countmiss") + caught + play.getInt("countkatu");
				
				return ((double) caught / (double) fruits) * 100;
			case 3:
				totalHits = play.getInt("countmiss") + play.getInt("count50") + play.getInt("count100") + 
						    play.getInt("count300") + play.getInt("countgeki") + play.getInt("countkatu");
				points = play.getInt("count50") * 50 + play.getInt("count100") * 100 + play.getInt("countkatu") * 200 +
						 play.getInt("count300") * 300 + play.getInt("countgeki") * 300;
				
				totalHits *= 300;
				
				return ((double) points / (double) totalHits) * 100;
			default: return 0.0;
		}
	}
	
	public static PPInfo fetchPPFromOppai(int beatmapId, int setId, double accuracy, int combo, String mods, int misses, int fifties, int hundreds){
		Utils.Login.osu();
		
		File osuFile = fetchOsuFile(beatmapId, setId);
		
		if(osuFile == null) return new PPInfo(0, 0);
		
		osuFile.renameTo(new File(beatmapId + ".osu"));
		osuFile = new File(beatmapId + ".osu");
		
		String actual = "./oppai " + osuFile.getName() + (accuracy == 100 ? "" : " " + accuracy + "%") +
						 (mods.length() != 0 ? " " + mods : "") +
						 (combo == 0 ? "" : " " + combo + "x") +
						 (misses == 0 ? "" : " " + misses + "m");
		
		String forFC = "./oppai " + osuFile.getName() + " " + hundreds + "x100 " + fifties + "x50" +
					   (mods.length() != 0 ? " " + mods : "");
		
		try{
			Process p = Runtime.getRuntime().exec(actual);
			double pp = fetchOppaiPP(p);
			double fc = 0.0;
			
			if(combo != 0){
				Process p2 = Runtime.getRuntime().exec(forFC);
				fc = fetchOppaiPP(p2);	
			}
			
			osuFile.delete();
			
			return new PPInfo(pp, fc);
		}catch(Exception e){
			Log.logger.log(Level.SEVERE, e.getMessage(), e);
			osuFile.delete();
			
			return new PPInfo(0, 0);
		}
	}
	
	private static double fetchOppaiPP(Process p){
		try{
			BufferedReader pIn = new BufferedReader(new InputStreamReader(p.getInputStream()));
			
			String s = null;
			String t = null;
			
			while((t = pIn.readLine()) != null)
				if(t != null) s = t;
			
			if(s == null) throw new Exception("string is null");
			
			double pp = Utils.stringToDouble(s.split("pp")[0]);
			
			return pp;	
		}catch(Exception e){
			Log.logger.log(Level.SEVERE, e.getMessage(), e);
			
			return 0.0;
		}
	}
	
	private static File fetchOsuFile(int beatmapId, int setId){
		String[] html = Utils.getHTMLCode("https://osu.ppy.sh/b/" + beatmapId);

		ArrayList<String> line = Utils.getNextLineCodeFromLink(html, 0, "beatmapTab active");
		if(line.isEmpty()) return null;

		String diffName = Jsoup.parse(line.get(0).split("<span>")[1].split("</span>")[0]).text();
		String url = "https://osu.ppy.sh/d/" + setId + "n";
		
		url = Utils.getFinalURL(url);
		
		URLConnection connection = establishConnection(url);
		boolean bloodcat = false;
		
		if(connection.getContentLength() <= 100){
			connection = establishConnection("http://bloodcat.com/osu/b/" + beatmapId);
			bloodcat = true;
		}
		
		File file = null;
		
        try{
			InputStream in = connection.getInputStream();
			FileOutputStream out = new FileOutputStream((bloodcat ? beatmapId + ".osu" : setId + ".zip"));
			
	        byte[] b = new byte[1024];
	        int count;
	        
	        while((count = in.read(b)) >= 0)
	        	out.write(b, 0, count);
	        
			in.close();
			out.close();
			
			file = new File((bloodcat ? beatmapId + ".osu" : setId + ".zip"));
			
			if(bloodcat && file != null) return file;
			if(file == null) return null;
			
			ZipInputStream zis = new ZipInputStream(new FileInputStream(file));
			ZipEntry entry = zis.getNextEntry();
			File finalFile = null;
			
			while(entry != null){
				if(entry.getName().endsWith(".osu")){
					BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(entry.getName()));
					
					byte[] buffer = new byte[4096];
					int read = 0;
					
					while((read = zis.read(buffer)) >= 0)
						bos.write(buffer, 0, read);
					
					bos.close();  
					
					BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(entry.getName()), "UTF-8"));
					
					String l = br.readLine();
					boolean found = false;
					
					while(l != null){
						if(l.startsWith("Version:") && l.replaceFirst("Version:", "").equalsIgnoreCase(diffName)){
							found = true;
							break;
						}
						
						l = br.readLine();
					}
					
					br.close();
					
					if(found){
						zis.closeEntry();
						zis.close();
						
						finalFile = new File(entry.getName());
						break;
					}
					
					new File(entry.getName()).delete();
				}
				
				zis.closeEntry();
				entry = zis.getNextEntry();
			}
			
			zis.close();
			file.delete();
			
			return finalFile;
        }catch(Exception e){
			Log.logger.log(Level.SEVERE, e.getMessage(), e);
			if(file != null && file.exists()) file.delete();
			
			return null;
		}
	}
	
	private static URLConnection establishConnection(String url){
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
	
}
