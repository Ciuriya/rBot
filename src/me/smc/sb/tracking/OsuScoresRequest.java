package me.smc.sb.tracking;

import org.json.JSONArray;

import me.smc.sb.discordcommands.OsuStatsCommand;
import me.smc.sb.utils.Utils;

public class OsuScoresRequest extends OsuRequest{

	public OsuScoresRequest(String... specifics){
		super("scores", RequestTypes.HYBRID, specifics);
	}
	
	public OsuScoresRequest(RequestTypes type, String... specifics){
		super("scores", type, specifics);
	}

	@Override
	public void send(boolean api) throws Exception{
		if(specifics.length < 3){
			answer = "invalid";
			setDone(true);
			return;
		}
		
		if(api){
			String type = "id";
			
			if(specifics.length >= 4)
				type = specifics[3];
			
			String post = Utils.sendPost("https://osu.ppy.sh/api/", "get_scores?k=" + OsuStatsCommand.apiKey + 
              	  						 "&b=" + specifics[0] + "&u=" + specifics[1] + "&m=" + specifics[2] + "&type=" + type + "&event_days=1");
			
			if(post == "" || !post.contains("{")){
				answer = "failed";
				setDone(true);
				return;
			}
			
			post = "[" + post + "]";
			
			answer = new JSONArray(post);
			setDone(true);
		}
	}
}
