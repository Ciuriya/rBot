package me.smc.sb.discordcommands;

import java.io.File;

import org.json.JSONArray;
import org.json.JSONObject;

import me.smc.sb.main.Main;
import me.smc.sb.tracking.OsuRecentPlaysRequest;
import me.smc.sb.tracking.OsuRequest;
import me.smc.sb.tracking.OsuTopPlaysRequest;
import me.smc.sb.tracking.OsuUserRequest;
import me.smc.sb.tracking.RequestTypes;
import me.smc.sb.tracking.TrackedPlay;
import me.smc.sb.tracking.TrackingUtils;
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
				OsuRequest topPlaysRequest = new OsuTopPlaysRequest("" + finalUser, "" + finalMode, "string", "100");
				OsuRequest recentPlaysRequest = new OsuRecentPlaysRequest(RequestTypes.API, "" + finalUser, "" + finalMode, "string", "50");
				Object userObj = Main.hybridRegulator.sendRequest(userRequest, true);
				Object topPlaysObj = Main.hybridRegulator.sendRequest(topPlaysRequest, true);
				Object recentPlaysObj = Main.hybridRegulator.sendRequest(recentPlaysRequest, true);
				
				if(userObj != null && userObj instanceof JSONObject && topPlaysObj != null && topPlaysObj instanceof JSONArray){
					JSONObject user = (JSONObject) userObj;
					JSONObject topPlayObj = ((JSONArray) topPlaysObj).getJSONObject(0);
					TrackedPlay topPlay = new TrackedPlay(topPlayObj, Utils.stringToInt(finalMode));

					topPlay.loadMap();
					
					int userId = user.getInt("user_id");
					OsuRequest htmlUserRequest = new OsuUserRequest(RequestTypes.HTML, "" + userId, "" + finalMode);
					Object htmlUserObj = Main.hybridRegulator.sendRequest(htmlUserRequest, true);
					
					if(htmlUserObj == null || !(htmlUserObj instanceof JSONObject)){
						Utils.info(e.getChannel(), "Could not fetch player's stats!");
						return;
					}
					
					JSONObject htmlUser = (JSONObject) htmlUserObj;
					
					double totalAcc = (double) user.getInt("count300") * 300.0 + 
							          (double) user.getInt("count100") * 100.0 + 
							          (double) user.getInt("count50") * 50.0;
					totalAcc = (totalAcc / ((double) (user.getInt("count300") + 
													  user.getInt("count100") + 
													  user.getInt("count50")) 
							   						  * 300.0)) * 100.0;
					
					double recentAcc = 0.0;
					
					if(recentPlaysObj != null && recentPlaysObj instanceof JSONArray){
						JSONArray recentPlays = (JSONArray) recentPlaysObj;
						recentAcc = Utils.df(getRecentAcc(recentPlays, Utils.stringToInt(finalMode)));
					}
					
					builder.setColor(Utils.getRandomColor());
					builder.setThumbnail("https://a.ppy.sh/" + userId);
					builder.setAuthor(user.getString("username"), 
									  "https://osu.ppy.sh/u/" + userId, 
									  "https://a.ppy.sh/" + userId);
					
					builder.addField("Rank", "World #" + Utils.veryLongNumberDisplay(user.getInt("pp_rank")) + "\n" + 
											 user.getString("country") + " #" + Utils.veryLongNumberDisplay(user.getInt("pp_country_rank")) + "\n", 
											 true);
					
					int rankCount = user.getInt("count_rank_ss") + user.getInt("count_rank_s") + user.getInt("count_rank_a");
					builder.addField("Performance Points", Utils.veryLongNumberDisplay(user.getDouble("pp_raw")) + "pp" + "\n" +
											 			   Utils.veryLongNumberDisplay(Utils.df(findBonusPP(rankCount))) + " bonus pp", true);
					
					builder.addField("Accuracy", "Weighted • " + Utils.df(user.getDouble("accuracy"), 4) + "%" +
											 	 (finalMode.equals("2") ? "" : "\nTotal • " + Utils.df(totalAcc, 4) + "%") +
											 	 (recentAcc > 0.0 ? "\nRecent • " + recentAcc + "%" : ""), true);
					
					builder.addField("Play Stats", "Level • " + user.getDouble("level") + "\n" +
											 	   "Play Count • " + Utils.veryLongNumberDisplay(user.getInt("playcount")) + "\n" +
											 	   "Play Time • " + Utils.veryLongNumberDisplay(getPlayTime(userId, finalMode)) + " hours\n" +
											 	   "Total Hits • " + Utils.veryLongNumberDisplay(htmlUser.getInt("total_hits")) + " hits\n" +
											 	   "Hits / Play • " + Utils.df(((double) htmlUser.getInt("total_hits") / (double) user.getInt("playcount"))), 
											 	   true);
					
					builder.addField("Score", "Ranked • " + Utils.veryLongNumberDisplay(user.getLong("ranked_score")) + "\n" +
											  "Total • " + Utils.veryLongNumberDisplay(user.getLong("total_score")), true);
					
					builder.addField("Other Stats", Utils.veryLongNumberDisplay(user.getInt("count_rank_ss")) + " SS • " + 
											  		Utils.veryLongNumberDisplay(user.getInt("count_rank_s")) + " S • " + 
											  		Utils.veryLongNumberDisplay(user.getInt("count_rank_a")) + " A\n" +
											  		"Replays Watched • " + Utils.veryLongNumberDisplay(htmlUser.getInt("replays_watched")) + " times\n" +
											  		"Kudosu Earned • " + Utils.veryLongNumberDisplay(htmlUser.getInt("kudosu_earned")), 
											  		true);
					
					builder.addField("Top Play", "[" + topPlay.getFormattedTitle() + "](https://osu.ppy.sh/b/" + topPlay.getBeatmapId() + ") " + 
											  	 topPlay.getModDisplay() + "\n" + Utils.df(topPlay.getAccuracy()) + "% • " +
												 (!topPlay.isPerfect() ? topPlay.getCombo() + "x" + 
														 (!topPlay.hasMapCombo() ? "" : " / " + topPlay.getMaxCombo() + "x") : "FC") +
												 " • " + topPlay.getFormattedRank() + " rank" +
												 (topPlay.getFormattedRank().contains("SS") ? "" : "\n" + topPlay.getFullHitText()) + 
												 "\n**" + topPlayObj.getDouble("pp") + "pp**", 
												 false);
					
					Utils.info(e.getChannel(), builder.build());
				}else
					Utils.info(e.getChannel(), "Could not fetch player's stats!");
			}
		});
		
		t.start();
	}
	
	public static double findBonusPP(int rankCount){
		return 416.6667f * (1 - Math.pow(0.9994, rankCount));
	}
	
	public static double getRecentAcc(JSONArray recentPlays, int mode){
		int count300 = 0, count100 = 0, count50 = 0, countmiss = 0, countkatu = 0, countgeki = 0;
		
		for(int i = 0; i < recentPlays.length(); i++){
			JSONObject play = recentPlays.getJSONObject(i);
			
			count300 += play.getInt("count300");
			count100 += play.getInt("count100");
			count50 += play.getInt("count50");
			countmiss += play.getInt("countmiss");
			countkatu += play.getInt("countkatu");
			countgeki += play.getInt("countgeki");
		}
		
		JSONObject obj = new JSONObject();
		
		obj.put("count300", count300);
		obj.put("count100", count100);
		obj.put("count50", count50);
		obj.put("countmiss", countmiss);
		obj.put("countkatu", countkatu);
		obj.put("countgeki", countgeki);
		
		return TrackingUtils.getAccuracy(obj, mode);
	}
	
}
