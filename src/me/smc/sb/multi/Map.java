package me.smc.sb.multi;

import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import me.smc.sb.discordcommands.OsuStatsCommand;
import me.smc.sb.main.Main;
import me.smc.sb.utils.Utils;

public class Map{

	private String url;
	private int category; // 0 - NM, 1 - FM, 2 - HD, 3 - HR, 4 - DT, 5 - TB
	private String bloodcatLink; // only longer than 0 if available
	private MapPool pool;
	
	public Map(String exported, MapPool pool){
		this.pool = pool;
		this.url = exported.split("\\|\\|")[0];
		this.category = Utils.stringToInt(exported.split("\\|\\|")[1]);
		
		bloodcatLink = "";
		
		if(exported.split("\\|\\|").length > 2)
			bloodcatLink = exported.split("\\|\\|")[2];
		else findBloodcatLink(false);
	}
	
	public Map(String url, int category, MapPool pool){
		this.url = url;
		this.category = category;
		this.pool = pool;
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
	
	public String getBloodcatLink(){
		if(bloodcatLink.length() > 0 && !bloodcatLink.equalsIgnoreCase("none"))
			return "https://bloodcat.com/osu/s/" + bloodcatLink;
		
		return "";
	}
	
	public void findBloodcatLink(boolean save){
		new Thread(new Runnable(){
			public void run(){
				String[] bloodcatHTML = Utils.getHTMLCode("http://bloodcat.com/osu/b/" + getBeatmapID());
				
				if(bloodcatHTML.length > 0 && bloodcatHTML[0].startsWith("osu file format")){
					List<String> setIDList = Utils.getNextLineCodeFromLink(bloodcatHTML, 0, "BeatmapSetID:");
					
					if(setIDList.size() > 0){
						bloodcatLink = setIDList.get(0).split(":")[1];
						
						if(save && pool != null)
							pool.save(false);
					}else bloodcatLink = "none";
				}else bloodcatLink = "none";
			}
		}).start();
	}
	
	public JSONObject getMapInfo(int mode){
		return getMapInfo(getBeatmapID(), mode, true);
	}
	
	public JSONObject getMapInfoNoPriority(int mode){
		return getMapInfo(getBeatmapID(), mode, false);
	}
	
	public String export(){
		return url + "||" + category + (bloodcatLink.length() > 0 ? "||" + bloodcatLink : "");
	}
	
	public static JSONObject getMapInfo(int id, int mode, boolean priority){
		String post = "";
		if(priority){
			post = Utils.sendPost("https://osu.ppy.sh/api/", "get_beatmaps?k=" + OsuStatsCommand.apiKey + "&b=" + id + "&m=" + mode + "&a=1&limit=1");
			Main.requestsSent++;
		}else post = Main.osuRequestManager.sendRequest("https://osu.ppy.sh/api/", "get_beatmaps?k=" + OsuStatsCommand.apiKey + "&b=" + id + "&m=" + mode + "&a=1&limit=1");
		
		if(post.equals("") || !post.contains("{")) return null;
		return new JSONArray("[" + post + "]").getJSONObject(0);
	}
	
}
