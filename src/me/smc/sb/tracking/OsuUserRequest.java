package me.smc.sb.tracking;

import java.util.List;

import org.json.JSONObject;

import me.smc.sb.discordcommands.OsuStatsCommand;
import me.smc.sb.utils.Utils;

public class OsuUserRequest extends OsuRequest{
	
	public OsuUserRequest(RequestTypes type, String... specifics){
		super("user", type, specifics);
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
			
			if(specifics.length >= 3)
				type = specifics[2];
			
			String post = Utils.sendPost("https://osu.ppy.sh/api/", "get_user?k=" + OsuStatsCommand.apiKey + 
										 "&u=" + specifics[0] + "&m=" + specifics[1] + "&type=" + type + "&event_days=1");
			
			if(post.equals("") || !post.contains("{")){
				// if it's invalid, user is probably banned
				answer = "failed";
				setDone(true);
				return;
			}

			answer = new JSONObject(post);
			setDone(true);
		}else{
			String[] pageGeneral = Utils.getHTMLCode("https://osu.ppy.sh/pages/include/profile-general.php?u=" + specifics[0] + "&m=" + specifics[1]);
			
			if(pageGeneral.length == 0){
				answer = "failed";
				setDone(true);
				return;
			}
			
			JSONObject user = new JSONObject();
			user.put("user_id", specifics[0]);
			
			List<String> ppLine = Utils.getNextLineCodeFromLink(pageGeneral, 0, "Performance</a>: ");
			List<String> userLine = Utils.getNextLineCodeFromLink(pageGeneral, 0, "&find=");
			List<String> countryRankLine = Utils.getNextLineCodeFromLink(pageGeneral, 3, "&find=");
			List<String> rankedScoreLine = Utils.getNextLineCodeFromLink(pageGeneral, 0, "<b>Ranked Score</b>");
			List<String> totalScoreLine = Utils.getNextLineCodeFromLink(pageGeneral, 0, "<b>Total Score</b>");
			List<String> accLine = Utils.getNextLineCodeFromLink(pageGeneral, 0, "<b>Hit Accuracy</b>");
			List<String> playCountLine = Utils.getNextLineCodeFromLink(pageGeneral, 0, "<b>Play Count</b>");
			List<String> playTimeLine = Utils.getNextLineCodeFromLink(pageGeneral, 0, "<b>Play Time</b>");
			List<String> levelLine = Utils.getNextLineCodeFromLink(pageGeneral, 0, "<b>Current Level</b>");
			List<String> levelPercentageLine = Utils.getNextLineCodeFromLink(pageGeneral, 1, "<b>Current Level</b>");
			List<String> hitsLine = Utils.getNextLineCodeFromLink(pageGeneral, 0, "<b>Total Hits</b>");
			List<String> maxComboLine = Utils.getNextLineCodeFromLink(pageGeneral, 0, "<b>Maximum Combo</b>");
			List<String> kudosuLine = Utils.getNextLineCodeFromLink(pageGeneral, 0, "Kudosu</a> Earned</b>");
			List<String> replaysLine = Utils.getNextLineCodeFromLink(pageGeneral, 0, "<b>Replays Watched by Others</b>");
			List<String> SSline = Utils.getNextLineCodeFromLink(pageGeneral, 0, "images/X.png'>");
			List<String> Sline = Utils.getNextLineCodeFromLink(pageGeneral, 0, "images/S.png'>");
			List<String> Aline = Utils.getNextLineCodeFromLink(pageGeneral, 0, "images/A.png'>");
			
			if(ppLine.size() > 0){
				user.put("pp_raw", ppLine.get(0).split("Performance<\\/a>: ")[1].split("pp ")[0].replaceAll(",", ""));
				user.put("pp_rank", ppLine.get(0).split("pp \\(#")[1].split("\\)<\\/b>")[0].replaceAll(",", ""));
			}
			
			if(userLine.size() > 0){
				user.put("username", userLine.get(0).split("&find=")[1].split("&")[0]);
				user.put("country", userLine.get(0).split("&c=")[1].split("&find=")[0]);
			}
			
			if(countryRankLine.size() > 0)
				user.put("pp_country_rank", countryRankLine.get(0).substring(1).replaceAll(",", ""));
				
			if(rankedScoreLine.size() > 0)
				user.put("ranked_score", rankedScoreLine.get(0).split("Ranked Score<\\/b>: ")[1].split("<\\/div>")[0].replaceAll(",", ""));
			
			if(totalScoreLine.size() > 0)
				user.put("total_score", totalScoreLine.get(0).split("Total Score<\\/b>: ")[1].split("<\\/div>")[0].replaceAll(",", ""));
			
			if(accLine.size() > 0)
				user.put("accuracy", accLine.get(0).split("Hit Accuracy<\\/b>: ")[1].split("%<\\/div>")[0]);
			
			if(playCountLine.size() > 0)
				user.put("playcount", playCountLine.get(0).split("Play Count<\\/b>: ")[1].split("<\\/div>")[0].replaceAll(",", ""));
			
			if(playTimeLine.size() > 0)
				user.put("playtime", playTimeLine.get(0).split("Play Time<\\/b>: ")[1].split(" hours<\\/div>")[0].replaceAll(",", ""));
			
			if(levelLine.size() > 0 && levelPercentageLine.size() > 0){
				String level = levelLine.get(0).split("Current Level<\\/b>: ")[1].split("<\\/div>")[0];
				level += "." + levelPercentageLine.get(0).split("align=right>")[1].split("%")[0];
				user.put("level", level);
			}
			
			if(hitsLine.size() > 0)
				user.put("total_hits", hitsLine.get(0).split("Total Hits<\\/b>: ")[1].split("<\\/div>")[0].replaceAll(",", ""));
			
			if(maxComboLine.size() > 0)
				user.put("max_combo", maxComboLine.get(0).split("Maximum Combo<\\/b>: ")[1].split("<\\/div>")[0].replaceAll(",", ""));
			
			if(kudosuLine.size() > 0)
				user.put("kudosu_earned", kudosuLine.get(0).split("Earned<\\/b>: ")[1].split("<\\/div>")[0].replaceAll(",", ""));
			
			if(replaysLine.size() > 0)
				user.put("replays_watched", replaysLine.get(0).split("Replays Watched by Others<\\/b>: ")[1].split(" times<\\/div>")[0].replaceAll(",", ""));
			
			if(SSline.size() > 0)
				user.put("count_rank_ss", SSline.get(0).split("<td width='50'>")[1].split("<\\/td>")[0].replaceAll(",", ""));
			
			if(Sline.size() > 0)
				user.put("count_rank_s", Sline.get(0).split("<td width='50'>")[1].split("<\\/td>")[0].replaceAll(",", ""));
			
			if(Aline.size() > 0)
				user.put("count_rank_a", Aline.get(0).split("<td width='50'>")[1].split("<\\/td>")[0].replaceAll(",", ""));
			
			answer = user;
			setDone(true);
		}
	}
}
