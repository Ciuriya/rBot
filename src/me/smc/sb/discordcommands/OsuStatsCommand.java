package me.smc.sb.discordcommands;

import java.io.File;

import org.json.JSONObject;

import me.smc.sb.utils.Configuration;
import me.smc.sb.utils.Utils;
import net.dv8tion.jda.MessageBuilder;
import net.dv8tion.jda.events.message.MessageReceivedEvent;

public class OsuStatsCommand extends GlobalCommand{

	public static String apiKey = "";
	
	public OsuStatsCommand(){
		super(null, 
			  " - Shows a specified osu! player's stats", 
			  "{prefix}osustats\nThis command lets you see all kinds of stats about the specified player.\n\n" +
			  "----------\nUsage\n----------\n{prefix}osustats {player} - Shows stats about the player\n" + 
			  "{prefix}osustats {player} ({mode={0/1/2/3}}) - Shows stats about the player in the specified mode\n\n" +
			  "----------\nAliases\n----------\nThere are no aliases.",
			  true, 
			  "osustats");
		apiKey = new Configuration(new File("login.txt")).getValue("apiKey");
	}
	
	private static int getCountryRank(int userId, String mode){
		String[] pageProfile = Utils.getHTMLCode("https://osu.ppy.sh/pages/include/profile-general.php?u=" + userId + "&m=" + mode);
		try{
			return Integer.parseInt(Utils.getNextLineCodeFromLink(pageProfile, 2, "<img class='flag' title='' src=").get(0).replace("#", "").replace(",", ""));
		}catch(Exception e){}
		return -1;
	}

	@Override
	public void onCommand(MessageReceivedEvent e, String[] args) {
		Utils.deleteMessage(e.getChannel(), e.getMessage());
		
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
		
		builder.appendString("```osu! user stats for " + jsonResponse.getString("username") + " (" + userId + ")")
		       .appendString("\n\nFrom " + jsonResponse.getString("country"))
		       .appendString("\nWorld #" + Utils.veryLongNumberDisplay(jsonResponse.getInt("pp_rank")) + " Country #" + Utils.veryLongNumberDisplay(getCountryRank(userId, mode)))
		       .appendString("\n" + jsonResponse.getDouble("pp_raw") + "pp")
		       .appendString("\nLevel " + jsonResponse.getDouble("level") + " Play Count: " + Utils.veryLongNumberDisplay(jsonResponse.getInt("playcount")))
		       .appendString("\nScore (Ranked): " + Utils.veryLongNumberDisplay(jsonResponse.getLong("ranked_score")) + " (Total): " + Utils.veryLongNumberDisplay(jsonResponse.getLong("total_score")))
		       .appendString("\n" + jsonResponse.getDouble("accuracy") + "% accuracy")
		       .appendString("\n" + totalAcc + "% total accuracy")
		       .appendString("\n(" + jsonResponse.getInt("count_rank_ss") + " SS) (" + jsonResponse.getInt("count_rank_s") + " S) (" + jsonResponse.getInt("count_rank_a") + " A)");
		builder.appendString("```");
		Utils.infoBypass(e.getChannel(), builder.build().getContent());
	}
	
}
