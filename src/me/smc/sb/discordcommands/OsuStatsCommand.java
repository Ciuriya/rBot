package me.smc.sb.discordcommands;

import java.io.File;

import org.json.JSONObject;

import me.smc.sb.main.Main;
import me.smc.sb.tracking.OsuRequest;
import me.smc.sb.tracking.OsuUserRequest;
import me.smc.sb.tracking.RequestTypes;
import me.smc.sb.utils.Configuration;
import me.smc.sb.utils.Utils;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

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
	
	private static int getPlayTime(int userId, String mode){
		String[] pageProfile = Utils.getHTMLCode("https://osu.ppy.sh/pages/include/profile-general.php?u=" + userId + "&m=" + mode);
		
		try{
			return Integer.parseInt(Utils.getNextLineCodeFromLink(pageProfile, 0, "Play Time").get(0).split(" hours")[0].split("<\\/b>: ")[1].replaceAll(",", ""));
		}catch(Exception e){}
		
		return -1;
	}

	@Override
	public void onCommand(MessageReceivedEvent e, String[] args){
		Utils.deleteMessage(e.getChannel(), e.getMessage());
		
		String user = "";
		String mode = "0";
		for(int i = 0; i < args.length; i++)
			if(args[i].contains("{mode=")){
				mode = args[i].split("\\{mode=")[1].split("}")[0];
			}else user += " " + args[i];
		final String finalUser = user.substring(1);
		final String finalMode = mode;
		
		Thread t = new Thread(new Runnable(){
			public void run(){
				EmbedBuilder builder = new EmbedBuilder();
				OsuRequest userRequest = new OsuUserRequest(RequestTypes.API, "" + finalUser, "" + finalMode, "string");
				Object userObj = Main.hybridRegulator.sendRequest(userRequest, true);
				
				if(userObj != null && userObj instanceof JSONObject){
					JSONObject jsonResponse = (JSONObject) userObj;
					
					int userId = jsonResponse.getInt("user_id");
					double totalAcc = (double) jsonResponse.getInt("count300") * 300.0 + 
							          (double) jsonResponse.getInt("count100") * 100.0 + 
							          (double) jsonResponse.getInt("count50") * 50.0;
					totalAcc = (totalAcc / ((double) (jsonResponse.getInt("count300") + 
							               			  jsonResponse.getInt("count100") + 
							               			  jsonResponse.getInt("count50")) 
							   						  * 300.0)) * 100.0;
					
					builder.setColor(Utils.getRandomColor());
					builder.addField("Player", jsonResponse.getString("username") + " (" + userId + ")", true);
					builder.setThumbnail("https://a.ppy.sh/" + userId);
					builder.addField("Rank", "World #" + Utils.veryLongNumberDisplay(jsonResponse.getInt("pp_rank")) + "\n" + 
											 jsonResponse.getString("country") + " #" + Utils.veryLongNumberDisplay(jsonResponse.getInt("pp_country_rank")) + "\n" +
											 Utils.veryLongNumberDisplay(jsonResponse.getDouble("pp_raw")) + "pp", true);
					builder.addField("Accuracy", "Weighted • " + Utils.df(jsonResponse.getDouble("accuracy"), 4) + "%" +
											 	 (finalMode.equals("2") ? "" : "\nTotal • " + Utils.df(totalAcc, 4) + "%"), true);
					builder.addField("Play Stats", "Level • " + jsonResponse.getDouble("level") + "\n" +
											 	   "Play Count • " + Utils.veryLongNumberDisplay(jsonResponse.getInt("playcount")) + "\n" +
											 	   "Play Time • " + Utils.veryLongNumberDisplay(getPlayTime(userId, finalMode)) + " hours", 
											 	   true);
					builder.addField("Score", "Ranked • " + Utils.veryLongNumberDisplay(jsonResponse.getLong("ranked_score")) + "\n" +
											  "Total • " + Utils.veryLongNumberDisplay(jsonResponse.getLong("total_score")), true);
					builder.addField("Ranks", Utils.veryLongNumberDisplay(jsonResponse.getInt("count_rank_ss")) + " SS • " + 
											  Utils.veryLongNumberDisplay(jsonResponse.getInt("count_rank_s")) + " S • " + 
											  Utils.veryLongNumberDisplay(jsonResponse.getInt("count_rank_a")) + " A", true);

					Utils.info(e.getChannel(), builder.build());
				}
			}
		});
		
		t.start();
	}
	
}
