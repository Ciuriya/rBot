package me.smc.sb.tracking;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.logging.Level;

import me.smc.sb.utils.Log;
import me.smc.sb.utils.Utils;

public class TrackingUtils {

	public static String convertMode(int mode){
		switch(mode){
			case 0: return "Standard";
			case 1: return "Taiko";
			case 2: return "Catch the Beat";
			case 3: return "Mania";
			default: return "Unknown";
		}
	}
	
	public static boolean playerHasRecentPlays(int userId, int mode, CustomDate lastUpdate){
		// loading recent plays history from profile to see if the player has done anything recently
		// this avoids spamming the api to check, note that this is called a LOT.
		String[] pageHistory = Utils.getHTMLCode("https://osu.ppy.sh/pages/include/profile-history.php?u=" + userId + "&m=" + mode);
		
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
	
}
