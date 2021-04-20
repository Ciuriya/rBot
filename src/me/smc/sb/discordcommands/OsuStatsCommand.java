package me.smc.sb.discordcommands;

import java.io.File;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import me.smc.sb.irccommands.ScanCheatersCommand;
import me.smc.sb.main.Main;
import me.smc.sb.tracking.Mods;
import me.smc.sb.tracking.OsuRecentPlaysRequest;
import me.smc.sb.tracking.OsuRequest;
import me.smc.sb.tracking.OsuTopPlaysRequest;
import me.smc.sb.tracking.OsuUserRequest;
import me.smc.sb.tracking.RequestTypes;
import me.smc.sb.tracking.TrackedPlay;
import me.smc.sb.tracking.TrackingUtils;
import me.smc.sb.utils.Configuration;
import me.smc.sb.utils.Utils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

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

	@Override
	public void onCommand(MessageReceivedEvent e, String[] args){
		String osuProfile = OsuSetProfileCommand.config.getValue(e.getAuthor().getId());
		
		if(args.length > 0) {
			osuProfile = "";
			
			for(int i = 0; i < args.length; i++)
				osuProfile += " " + args[i];
			
			osuProfile = osuProfile.substring(1);
			osuProfile = Utils.getOsuPlayerIdFast(osuProfile);
			
			if(osuProfile.equals("-1") || osuProfile.length() == 0) {
				Utils.info(e.getChannel(), "Could not find player!");
				return;
			}
		}
		
		if(osuProfile.length() == 0) {
			Utils.info(e.getChannel(), "Your osu! profile is not set! Use " + Main.getCommandPrefix(e.getGuild().getId()) + "osuset {player}");
			return;
		}
		
		final String finalUser = osuProfile;
		final String finalMode = "0";
		
		Thread t = new Thread(new Runnable(){
			public void run(){
				EmbedBuilder builder = new EmbedBuilder();
				OsuRequest userRequest = new OsuUserRequest(RequestTypes.API, "" + finalUser, "" + finalMode);
				OsuRequest topPlaysRequest = new OsuTopPlaysRequest("" + finalUser, "" + finalMode, "id", "100");
				OsuRequest recentPlaysRequest = new OsuRecentPlaysRequest(RequestTypes.API, "" + finalUser, "" + finalMode, "id", "50");
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
					int totalHits = user.getInt("count300") + user.getInt("count100") + user.getInt("count50");
					
					double totalAcc = (double) user.getInt("count300") * 300.0 + 
							          (double) user.getInt("count100") * 100.0 + 
							          (double) user.getInt("count50") * 50.0;
					totalAcc = (totalAcc / ((double) (totalHits * 300.0))) * 100.0;
					
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
					
					int[] rankIncrease = ScanCheatersCommand.calculateRankIncrease((String[]) htmlUser.get("general_page"));
					float growth = ((float) rankIncrease[3] / (float) rankIncrease[4]) * 100 - 100; //first / last
					
					builder.addField("Rank", "World #" + Utils.veryLongNumberDisplay(user.getInt("pp_rank")) + "\n" + 
											 user.getString("country") + " #" + Utils.veryLongNumberDisplay(user.getInt("pp_country_rank")) + "\n" +
											 Utils.df(growth) + "% growth\n(#" + rankIncrease[3] + " -> #" + rankIncrease[4] + ")",
											 true);
					
					int rankCount = user.getInt("count_rank_ss") + user.getInt("count_rank_ssh") + 
									user.getInt("count_rank_s") + user.getInt("count_rank_sh") + user.getInt("count_rank_a");
					builder.addField("Performance Points", Utils.veryLongNumberDisplay(user.getDouble("pp_raw")) + "pp" + "\n~" +
											 			   Utils.df(findBonusPP(rankCount), 2) + " bonus pp", true);
					
					builder.addField("Accuracy", "Overall • " + Utils.df(user.getDouble("accuracy"), 3) + "%" +
											 	 (finalMode.equals("2") ? "" : "\nTotal • " + Utils.df(totalAcc, 3) + "%") +
											 	 (recentAcc > 0.0 ? "\nRecent • " + recentAcc + "%" : "") +
											 	 "\n" + Utils.df((double) user.getInt("count300") / (double) totalHits * 100) + "% 300s" +
											 	 "\n" + Utils.df((double) user.getInt("count100") / (double) totalHits * 100) + "% 100s" +
											 	 "\n" + Utils.df((double) user.getInt("count50") / (double) totalHits * 100) + "% 50s", true);
					
					builder.addField("Play Stats", "Level • " + user.getDouble("level") + "\n" +
											 	   "Play Count • " + Utils.veryLongNumberDisplay(user.getInt("playcount")) + "\n" +
											 	   "Play Time • " + Utils.veryLongNumberDisplay(htmlUser.getInt("playtime")) + " hours\n" +
											 	   "Total Hits • " + Utils.veryLongNumberDisplay(htmlUser.getInt("total_hits")) + " hits\n" +
											 	   "Hits / Play • " + Utils.df(((double) htmlUser.getInt("total_hits") / (double) user.getInt("playcount"))), 
											 	   true);
					
					builder.addField("Score", "Ranked • " + Utils.veryLongNumberDisplay(user.getLong("ranked_score")) + "\n" +
											  "Total • " + Utils.veryLongNumberDisplay(user.getLong("total_score")), true);
					
					builder.addField("Other Stats", Utils.veryLongNumberDisplay(user.getInt("count_rank_ss")) + " SS • " + 
											  		Utils.veryLongNumberDisplay(user.getInt("count_rank_ssh")) + " SSH\n" + 
											  		Utils.veryLongNumberDisplay(user.getInt("count_rank_s")) + " S • " +
											  		Utils.veryLongNumberDisplay(user.getInt("count_rank_sh")) + " SH\n" +
											  		Utils.veryLongNumberDisplay(user.getInt("count_rank_a")) + " A\n" +
											  		Utils.veryLongNumberDisplay(htmlUser.getInt("replays_watched")) + " Replays Watched\n" +
											  		Utils.veryLongNumberDisplay(htmlUser.getInt("kudosu_earned")) + " Kudosu Earned", 
											  		true);
					
					builder.addField("Top Play", "[" + topPlay.getFormattedTitle() + "](https://osu.ppy.sh/b/" + topPlay.getBeatmapId() + ") " + 
											  	 topPlay.getModDisplay() + "\n" + Utils.df(topPlay.getAccuracy()) + "% • " +
												 (!topPlay.isPerfect() ? topPlay.getCombo() + "x" + 
														 (!topPlay.hasMapCombo() ? "" : " / " + topPlay.getMaxCombo() + "x") : "FC") +
												 " • " + topPlay.getFormattedRank() + " rank" +
												 (topPlay.getFormattedRank().contains("SS") ? "" : " • " + topPlay.getFullHitText()) + 
												 "\n**" + topPlayObj.getDouble("pp") + "pp**", 
												 false);
					
					builder.addField("Top Play Analysis", analyseTopPlays(((JSONArray) topPlaysObj), Utils.stringToInt(finalMode)), false);
					
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
	
	public static String analyseTopPlays(JSONArray topPlays, int mode){
		Map<Mods, Integer> modCount = new HashMap<>();
		Map<Mods, Double> modPP = new HashMap<>();
		int fcCount = 0;
		
		for(int i = 0; i < topPlays.length(); i++){
			JSONObject play = topPlays.getJSONObject(i);
			TrackedPlay topPlay = new TrackedPlay(play, mode);
			double pp = play.getDouble("pp");
			double weighted = pp * Math.pow(0.95, i);
			List<Mods> mods = topPlay.getMods();
			
			if(topPlay.getRawMods() == 0) mods.add(Mods.None);
			
			for(Mods mod : mods){
				if(modCount.containsKey(mod))
					modCount.put(mod, modCount.get(mod) + 1);
				else modCount.put(mod, 1);
				
				if(modPP.containsKey(mod))
					modPP.put(mod, modPP.get(mod) + weighted);
				else modPP.put(mod, weighted);
			}
				
			if(mode == 0){
				if(topPlay.isPerfect()) fcCount++;
				else if(topPlay.getRank().equalsIgnoreCase("S") || topPlay.getRank().equalsIgnoreCase("SH")){
					if(!topPlay.isMapLoaded()) topPlay.loadMap();
					
					if(topPlay.hasMapCombo()){
						int totalMistimes = topPlay.getHundreds() + topPlay.getFifties();
						int missedCombo = topPlay.getMaxCombo() - topPlay.getCombo();
						
						if(totalMistimes >= missedCombo) fcCount++;
					}
				}
			}
		}
		
		String display = "";
		
		if(mode == 0)
			display += fcCount + " FCs • " + (100 - fcCount) + " chokes\n";
		
		int modCountSize = modCount.size();
		for(int i = 0; i < modCountSize; i++){
			int count = modCount.values().stream().max(new Comparator<Integer>(){
				@Override
				public int compare(Integer o1, Integer o2){
					return Integer.compare(o1, o2);
				}
			}).orElse(-1);
			
			if(count == -1) break;
			
			Mods mod = null;
			
			for(Mods toFind : modCount.keySet())
				if(modCount.get(toFind) == count)
					mod = toFind;
			
			if(mod != null){
				display += mod.getShortName() + " • " + count + " play" + (count > 1 ? "s" : "") + " • " + Utils.df(modPP.get(mod)) + " weighted pp\n";
				modCount.remove(mod);
			}
		}
		
		return display;
	}
	
}
