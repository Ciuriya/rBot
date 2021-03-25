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
		
		String postFilters = "";
		
		if(specifics[0] != "-1") postFilters += "&b=" + specifics[0];
		if(specifics[1] != "-1") postFilters += "&m=" + specifics[1];
		if(specifics[2] != "-1") postFilters += "&a=" + specifics[2];
		if(specifics[3] != "-1") postFilters += "&limit=" + specifics[3];
		if(specifics.length >= 5 && specifics[4] != "-1") postFilters += "&since=" + specifics[4];
		
		String post = Utils.sendPost("https://osu.ppy.sh/api/", "get_beatmaps?k=" + OsuStatsCommand.apiKey + postFilters);

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
