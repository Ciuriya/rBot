package me.smc.sb.commands;

import org.json.JSONObject;

import me.itsghost.jdiscord.events.UserChatEvent;
import me.itsghost.jdiscord.message.MessageBuilder;
import me.smc.sb.utils.Utils;

public class OsuStatsCommand extends GlobalCommand{

	private static String apiKey = "07aa8c33fcfaef704aa81f66a5803bfc6f4ba6da";
	
	public OsuStatsCommand(){
		super(null, 
			  " - Shows a specified osu! player's stats", 
			  "{prefix}osustats\nThis command lets you see all kinds of stats about the specified player.\n\n" +
			  "----------\nUsage\n----------\n{prefix}osustats {player} - Shows stats about the player\n" + 
			  "{prefix}osustats {player} ({mode={0/1/2/3}}) - Shows stats about the player in the specified mode\n\n" +
			  "----------\nAliases\n----------\nThere are no aliases.",
			  true, 
			  "osustats");
	}
	
	private static int getCountryRank(int userId, String mode){
		String[] pageProfile = Utils.getHTMLCode("https://osu.ppy.sh/pages/include/profile-general.php?u=" + userId + "&m=" + mode);
		try{
			return Integer.parseInt(Utils.getNextLineCodeFromLink(pageProfile, 2, "<img class='flag' title='' src=").get(0).replace("#", "").replace(",", ""));
		}catch(Exception e){}
		return -1;
	}

	@Override
	public void onCommand(UserChatEvent e, String[] args) {
		String user = "";
		String mode = "0";
		for(int i = 0; i < args.length; i++)
			if(args[i].contains("{mode=")){
				mode = args[i].split("\\{mode=")[1].split("}")[0];
			}else user += " " + args[i];
		user = user.substring(1);
		
		MessageBuilder builder = new MessageBuilder();
		String post = Utils.sendPost("https://osu.ppy.sh/api/", "get_user?k=" + apiKey + "&u=" + user + "&m=" + mode + "&type=string&event_days=1");
		if(post == "" || !post.contains("{")) return;
		
		JSONObject jsonResponse = new JSONObject(post);
		
		int userId = jsonResponse.getInt("user_id");
		double totalAcc = (double) jsonResponse.getInt("count300") * 300.0 + (double) jsonResponse.getInt("count100") * 100.0 + (double) jsonResponse.getInt("count50") * 50.0;
		totalAcc = (totalAcc / ((double) (jsonResponse.getInt("count300") + jsonResponse.getInt("count100") + jsonResponse.getInt("count50")) * 300.0)) * 100.0;
		
		builder.addString("```osu! user stats for " + jsonResponse.getString("username") + " (" + userId + ")")
		       .addString("\n\nFrom " + jsonResponse.getString("country"))
		       .addString("\nWorld #" + Utils.veryLongNumberDisplay(jsonResponse.getInt("pp_rank")) + " Country #" + Utils.veryLongNumberDisplay(getCountryRank(userId, mode)))
		       .addString("\n" + jsonResponse.getDouble("pp_raw") + "pp")
		       .addString("\nLevel " + jsonResponse.getDouble("level") + " Play Count: " + Utils.veryLongNumberDisplay(jsonResponse.getInt("playcount")))
		       .addString("\nScore (Ranked): " + Utils.veryLongNumberDisplay(jsonResponse.getLong("ranked_score")) + " (Total): " + Utils.veryLongNumberDisplay(jsonResponse.getLong("total_score")))
		       .addString("\n" + jsonResponse.getDouble("accuracy") + "% accuracy")
		       .addString("\n" + totalAcc + "% total accuracy")
		       .addString("\n(" + jsonResponse.getInt("count_rank_ss") + " SS) (" + jsonResponse.getInt("count_rank_s") + " S) (" + jsonResponse.getInt("count_rank_a") + " A)");
		builder.addString("```");
		Utils.infoBypass(e.getGroup(), builder.build());
	}
	
}
