package me.smc.sb.tracking;

import org.json.JSONArray;

import me.smc.sb.discordcommands.OsuStatsCommand;
import me.smc.sb.main.Main;
import me.smc.sb.utils.Utils;

public class OsuRecentPlaysRequest extends OsuRequest{

	public OsuRecentPlaysRequest(){
		super("recent-plays", RequestTypes.HYBRID);
	}

	@Override
	public void send(boolean api) throws Exception {
		if(specifics.length != 2){
			answer = "invalid";
			return;
		}
		
		if(api){
			String post = Utils.sendPost("https://osu.ppy.sh/api/", "get_user_recent?k=" + OsuStatsCommand.apiKey + 
              	  						 "&u=" + specifics[0] + "&m=" + specifics[1] + "&limit=" + TrackedPlayer.API_FETCH_PLAY_LIMIT + "&type=id&event_days=1");
			
			if(post == "" || !post.contains("{")){
				answer = "invalid";
				setDone(true);
				return;
			}
			
			post = "[" + post + "]";
			
			answer = new JSONArray(post);			
			setDone(true);
		}else{
			String[] pageHistory = Utils.getHTMLCode("https://osu.ppy.sh/pages/include/profile-history.php?u=" + specifics[0] + "&m=" + specifics[1]);
			
			if(pageHistory.length == 0 || !pageHistory[0].contains("<div class='profileStatHeader'>Recent Plays (last 24h):")){
				answer = "invalid";
				setDone(false);
				return;
			}
			
			// finish pls, also standardize output
		}
	}
	
	private void setDone(boolean api){
		done = true;
		
		if(api) Main.requestsSent++;
		else Main.requestHtmlSent++;
	}
}
