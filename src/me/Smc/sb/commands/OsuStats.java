package me.Smc.sb.commands;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.json.JSONObject;

import me.Smc.sb.utils.Utils;
import me.itsghost.jdiscord.events.UserChatEvent;
import me.itsghost.jdiscord.message.MessageBuilder;

public class OsuStats{

	private static String apiKey = "07aa8c33fcfaef704aa81f66a5803bfc6f4ba6da";
	
	public static void execute(UserChatEvent e, String msg, boolean dm){
		String user = "";
		String mode = "0";
		if(msg.contains("{mode=")){
			mode = msg.split("\\{mode=")[1].split("}")[0];
			msg = msg.replace(" {mode=" + mode + "}", "");
		}
		String[] split = msg.split(" ");
		for(int i = 1; i < split.length; i++)
			user += " " + split[i];
		user = user.substring(1);
		MessageBuilder builder = new MessageBuilder();
		String post = sendPost("https://osu.ppy.sh/api/", "get_user?k=" + apiKey + "&u=" + user + "&m=" + mode + "&type=string&event_days=1");
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
		if(!dm){
			Utils.infoBypass(e.getGroup(), e.getUser().getUser(), builder.build().getMessage());
		}else Utils.infoBypass(e.getGroup(), builder.build().getMessage());
	}
	
	private static int getCountryRank(int userId, String mode){
		String[] pageProfile = Utils.getHTMLCode("https://osu.ppy.sh/pages/include/profile-general.php?u=" + userId + "&m=" + mode);
		try{
			return Integer.parseInt(Utils.getNextLineCodeFromLink(pageProfile, 2, "<img class='flag' title='' src=").get(0).replace("#", "").replace(",", ""));
		}catch(Exception e){}
		return -1;
	}

	private static String sendPost(String urlString, String urlParameters){
		String answer = "";
		try{
			URL url = new URL(urlString + urlParameters);
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod("POST");
			connection.setConnectTimeout(5000);
			connection.setReadTimeout(5000);
			connection.setRequestProperty("User-Agent", "Mozilla/5.0");
			connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
			connection.setRequestProperty("charset", "utf-8");
			connection.setRequestProperty("Content-Length", "" + Integer.toString(urlParameters.getBytes().length));
			BufferedReader inputStream = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			String inputLine;
			StringBuffer response = new StringBuffer();
			while((inputLine = inputStream.readLine()) != null) response.append(inputLine);
			inputStream.close();
			response.deleteCharAt(0);
			response.deleteCharAt(response.length() - 1);
			answer = response.toString();
		}catch(Exception e){e.printStackTrace();}
		return answer;
	}
	
}
