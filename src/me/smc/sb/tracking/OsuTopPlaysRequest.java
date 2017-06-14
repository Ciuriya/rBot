package me.smc.sb.tracking;

import org.json.JSONArray;

import me.smc.sb.discordcommands.OsuStatsCommand;
import me.smc.sb.utils.Utils;

public class OsuTopPlaysRequest extends OsuRequest{

	public OsuTopPlaysRequest(String... specifics){
		super("top-plays", RequestTypes.API, specifics);
	}

	@Override
	public void send(boolean api) throws Exception{
		if(specifics.length < 2){
			answer = "invalid";
			setDone(true);
			return;
		}
		
		String type = "id";
		
		if(specifics.length >= 3)
			type = specifics[2];
		
		String post = Utils.sendPost("https://osu.ppy.sh/api/", "get_user_best?k=" + OsuStatsCommand.apiKey + 
																"&u=" + specifics[0] + "&m=" + specifics[1] + "&limit=100&type=" + type);
		
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
