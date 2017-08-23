package me.smc.sb.tracking;

import org.json.JSONArray;

import me.smc.sb.discordcommands.OsuStatsCommand;
import me.smc.sb.utils.Utils;

public class OsuMultiRequest extends OsuRequest{

	public OsuMultiRequest(String... specifics){
		super("multi", RequestTypes.API, specifics);
	}

	@Override
	public void send(boolean api) throws Exception{
		if(specifics.length < 1){
			answer = "invalid";
			setDone(true);
			return;
		}
		
		String post = Utils.sendPost("https://osu.ppy.sh/api/", "get_match?k=" + OsuStatsCommand.apiKey + 
				  					 "&mp=" + specifics[0]);

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
