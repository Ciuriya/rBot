package me.smc.sb.multi;

import java.util.logging.Level;

import org.json.JSONArray;
import org.json.JSONObject;

import me.smc.sb.discordcommands.OsuStatsCommand;
import me.smc.sb.main.Main;
import me.smc.sb.utils.Log;
import me.smc.sb.utils.Utils;

public class Map{

	private String url;
	private int category; //0 - NM, 1 - FM, 2 - HD, 3 - HR, 4 - DT, 5 - TB
	
	public Map(String exported){
		this.url = exported.split("\\|\\|")[0];
		this.category = Utils.stringToInt(exported.split("\\|\\|")[1]);
	}
	
	public Map(String url, int category){
		this.url = url;
		this.category = category;
	}
	
	public String getURL(){
		return url;
	}
	
	public int getCategory(){
		return category;
	}
	
	public int getBeatmapID(){
		String cutURL = url.split("sh\\/b\\/")[1];
		if(cutURL.contains("?"))
			cutURL = cutURL.substring(0, cutURL.indexOf("?") + 1);
		
		if(cutURL.contains("&"))
			cutURL = cutURL.substring(0, cutURL.indexOf("&") + 1);
		
		if(Utils.stringToInt(cutURL) != -1) return Utils.stringToInt(cutURL);
		return 0;
	}
	
	public JSONObject getMapInfo(int mode){
		return getMapInfo(getBeatmapID(), mode, true);
	}
	
	public JSONObject getMapInfoNoPriority(int mode){
		return getMapInfo(getBeatmapID(), mode, false);
	}
	
	public String export(){
		return url + "||" + category;
	}
	
	public static JSONObject getMapInfo(int id, int mode, boolean priority){
		String post = "";
		if(priority){
			post = Utils.sendPost("https://osu.ppy.sh/api/", "get_beatmaps?k=" + OsuStatsCommand.apiKey + "&b=" + id + "&m=" + mode + "&a=1&limit=1");
			Main.requestsSent++;
			
			Log.logger.log(Level.INFO, "PRIORITY POST || " + post);
		}else post = Main.osuRequestManager.sendRequest("https://osu.ppy.sh/api/", "get_beatmaps?k=" + OsuStatsCommand.apiKey + "&b=" + id + "&m=" + mode + "&a=1&limit=1");
		
		if(post.equals("") || !post.contains("{")) return null;
		return new JSONArray("[" + post + "]").getJSONObject(0);
	}
	
}
