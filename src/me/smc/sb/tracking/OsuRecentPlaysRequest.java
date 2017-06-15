package me.smc.sb.tracking;

import java.util.logging.Level;

import org.json.JSONArray;
import org.json.JSONObject;

import me.smc.sb.discordcommands.OsuStatsCommand;
import me.smc.sb.utils.Log;
import me.smc.sb.utils.Utils;

public class OsuRecentPlaysRequest extends OsuRequest{

	public OsuRecentPlaysRequest(String... specifics){
		super("recent-plays", RequestTypes.HYBRID, specifics);
	}
	
	public OsuRecentPlaysRequest(RequestTypes type, String... specifics){
		super("recent-plays", type, specifics);
	}

	@Override
	public void send(boolean api) throws Exception{
		if(specifics.length < 2){
			answer = "invalid";
			setDone(true);
			return;
		}
		
		if(api){
			String type = "id";
			int limit = TrackedPlayer.API_FETCH_PLAY_LIMIT;
			
			if(specifics.length >= 3)
				type = specifics[2];
			
			if(specifics.length == 4)
				limit = Utils.stringToInt(specifics[3]);
			
			String post = Utils.sendPost("https://osu.ppy.sh/api/", "get_user_recent?k=" + OsuStatsCommand.apiKey + 
              	  						 "&u=" + specifics[0] + "&m=" + specifics[1] + "&limit=" + limit + "&type=" + type + "&event_days=1");
			
			if(post == "" || !post.contains("{")){
				answer = "failed";
				setDone(true);
				return;
			}
			
			post = "[" + post + "]";
			
			answer = new JSONArray(post);
			setDone(true);
		}else{
			String[] pageHistory = Utils.getHTMLCode("https://osu.ppy.sh/pages/include/profile-history.php?u=" + specifics[0] + "&m=" + specifics[1]);
			
			if(pageHistory.length == 0 || !pageHistory[0].contains("<div class='profileStatHeader'>Recent Plays (last 24h):")){
				answer = "failed";
				setDone(true);
				return;
			}
			
			String[] splitTime = pageHistory[0].split("<\\/time>");
			
			JSONArray response = new JSONArray();
			
			for(int i = 0; i < splitTime.length - 1; i++){
				try{
					JSONObject play = new JSONObject();
					String[] scoreSplit = splitTime[i + 1].split("<\\/a>");
					
					play.put("date", splitTime[i].split("time class=")[1].split(">")[1].replace(" UTC", ""));
					play.put("beatmap_id", scoreSplit[0].split("href='\\/b\\/")[1].split("'")[0]);
					play.put("score", scoreSplit[1].substring(1).split(" ")[0].replaceAll(",", ""));
					play.put("rank", scoreSplit[1].split("\\(")[1].split("\\)")[0]);
					play.put("user_id", specifics[0]);
					play.put("enabled_mods", Mods.getMods(scoreSplit[1].split("<br\\/>")[0].split(" ")[1]));
					
					response.put(play);
				}catch(Exception e){
					Log.logger.log(Level.INFO, "Error parsing recent-plays HTML, splitTime[" + i + "] =" + splitTime[i]);
				}
			}

			answer = response;
			setDone(true);
		}
	}
}
