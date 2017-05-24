package me.smc.sb.tracking;

import org.json.JSONArray;

import me.smc.sb.discordcommands.OsuStatsCommand;
import me.smc.sb.utils.Utils;

public class OsuBeatmapsRequest extends OsuRequest{

	public OsuBeatmapsRequest(String... specifics){
		super("beatmaps", RequestTypes.API, specifics);
	}

	@Override
	public void send(boolean api) throws Exception{
		if(specifics.length < 4){
			answer = "invalid";
			setDone(true);
			return;
		}
		
		String post = Utils.sendPost("https://osu.ppy.sh/api/", "get_beatmaps?k=" + OsuStatsCommand.apiKey + 
				  					 "&b=" + specifics[0] + "&m=" + specifics[1] + "&a=" + specifics[2] + "&limit=" + specifics[3]);

		if(post == "" || !post.contains("{")){
			answer = "invalid";
			setDone(true);
			return;
		}
		
		post = "[" + post + "]";
		answer = new JSONArray(post);
		setDone(true);
	}
}
