package me.smc.sb.discordcommands;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

import me.smc.sb.main.Main;
import me.smc.sb.tracking.CustomDate;
import me.smc.sb.tracking.OsuRecentPlaysRequest;
import me.smc.sb.tracking.OsuRequest;
import me.smc.sb.tracking.OsuTopPlaysRequest;
import me.smc.sb.tracking.OsuUserRequest;
import me.smc.sb.tracking.RecentPlay;
import me.smc.sb.tracking.RequestTypes;
import me.smc.sb.tracking.TrackedPlay;
import me.smc.sb.tracking.TrackedPlayer;
import me.smc.sb.tracking.TrackingUtils;
import me.smc.sb.utils.Utils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class OsuRecentPlayCommand extends GlobalCommand{
	
	public static Map<String, String> latestRecents = new HashMap<>();

	public OsuRecentPlayCommand() {
		super(null, 
				" - Shows your latest play", 
				"{prefix}osurecent\nThis command lets you see an osu! player's latest play\n\n" +
				"----------\nUsage\n----------\n{prefix}osurecent (player) - Shows your (or the player's) latest play\n\n" + 
			    "----------\nAliases\n----------\n{prefix}recent", 
			    true, 
				"osurecent", "recent");
	}

	@Override
	public void onCommand(MessageReceivedEvent e, String[] args) {
		Utils.deleteMessage(e.getChannel(), e.getMessage());
		
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
		
		OsuRequest recentPlaysRequest = new OsuRecentPlaysRequest(RequestTypes.API, "" + osuProfile, "" + 0, "id", "50");
		Object recentPlaysObj = Main.hybridRegulator.sendRequest(recentPlaysRequest, true);
		
		if(recentPlaysObj != null && recentPlaysObj instanceof JSONArray){
			JSONArray jsonResponse = (JSONArray) recentPlaysObj;
			
			if(jsonResponse.length() == 0) {
				Utils.info(e.getChannel(), "This player has no recent plays!");
				return;
			}
			
			JSONObject jsonObj = jsonResponse.getJSONObject(0);
			TrackedPlay play = new TrackedPlay(jsonObj, 0);
			int tryCount = 0;
			
			play.loadMap();
			play.loadPP();
			
			for(int i = jsonResponse.length() - 1; i >= 0; i--){
				JSONObject recentPlay = jsonResponse.getJSONObject(i);
				
				if(recentPlay.getInt("beatmap_id") == play.getBeatmapId())
					tryCount++;
			}
			
			JSONObject jsonUser = null;
			OsuRequest userRequest = new OsuUserRequest(RequestTypes.API, "" + osuProfile, "0");
			Object userObj = Main.hybridRegulator.sendRequest(userRequest, true);
			
			if(userObj != null && userObj instanceof JSONObject)
				jsonUser = (JSONObject) userObj;
			
			String name = jsonUser.getString("username");
			TrackedPlayer player = TrackedPlayer.get(jsonUser.getInt("user_id"), 0); // old data
			String rankDisplay = "";
			
			OsuRequest topPlaysRequest = new OsuTopPlaysRequest("" + osuProfile, "0", "id", "100");
			Object topPlaysObj = Main.hybridRegulator.sendRequest(topPlaysRequest, true);
			JSONArray tpJsonResponse = null;
			
			if(topPlaysObj != null && topPlaysObj instanceof JSONArray)
				tpJsonResponse = (JSONArray) topPlaysObj;
			
			for(int j = 0; j < tpJsonResponse.length(); j++){
				JSONObject topPlay = tpJsonResponse.getJSONObject(j);
				
				if(topPlay.getInt("beatmap_id") == play.getBeatmapId() &&
				   topPlay.getInt("enabled_mods") == play.getRawMods() &&
				   topPlay.getLong("score") == play.getRawScore() &&
				   TrackingUtils.getAccuracy(topPlay, 0) - play.getAccuracy() <= 0.01){
					
					if(play.getPPInfo() != null && topPlay.has("pp"))
						play.getPPInfo().setPP(topPlay.getDouble("pp"));
					
					play.setPersonalBestCount(j + 1);
					
					break;
				}		
			}
			
			if(player != null){
				int rank = player.getRank();
				boolean rankDiff = false;
				
				if((play.getDate().after(player.getLastRankUpdate()) && Math.abs(player.getRank() - player.getOldRank()) > 0 && player.getOldRank() > 0)){
					rank = player.getOldRank();
					rankDiff = true;
				}else if(Math.abs(player.getRank() - jsonUser.getInt("pp_rank")) > 0 && player.getRank() > 0) rankDiff = true;
				else if(player.getRank() == 0) rank = jsonUser.getInt("pp_rank");
				
				rankDisplay = " • #" + Utils.veryLongNumberDisplay(rank) + rankDisplay;
				
				if(rankDiff) rankDisplay += " -> #" + Utils.veryLongNumberDisplay(jsonUser.getInt("pp_rank"));
			} else rankDisplay = " • #" + Utils.veryLongNumberDisplay(jsonUser.getInt("pp_rank"));
			
			EmbedBuilder builder = new EmbedBuilder();
			double fcPercentage = 1.0;
			
			if(play.hasCombo() && play.hasMapCombo())
				fcPercentage = (double) play.getCombo() / (double) play.getMaxCombo();
			
			builder.setColor(Utils.getPercentageColor(fcPercentage));
			builder.setThumbnail("http://b.ppy.sh/thumb/" + play.getBeatmapSetId() + "l.jpg");
			builder.setAuthor(name + rankDisplay, "https://osu.ppy.sh/users/" + osuProfile, "https://a.ppy.sh/" + osuProfile);
			
			builder.setTitle(TrackingUtils.escapeCharacters(play.getArtist() + " - " + play.getTitle() + 
					 " [" + play.getDifficulty() + "] " + play.getModDisplay()), 
					 "http://osu.ppy.sh/b/" + play.getBeatmapId());
			
			String rankText = play.getFormattedRank();
			String comboText = "";
			String ppText = "";
			String accText = Utils.df(play.getAccuracy()) + "%";
			String hitText = play.getFullHitText();
			String scoreText = play.getScore();
			String mapCompletionText = "";
			String mapRankText = "";
			String pbText = "";
			String tryText = "";
			
			tryText = "**" + tryCount + getOrdinalString(tryCount) + "** tr" + (tryCount >= 50 ? "ies" : "y");
			
			if(play.isPerfect())
				comboText += "FC (" + Utils.veryLongNumberDisplay(play.getCombo()) + "x)";
			else{
				comboText += Utils.veryLongNumberDisplay(play.getCombo()) + "x";
				
				if(play.hasMapCombo())
					comboText += " / " + Utils.veryLongNumberDisplay(play.getMaxCombo()) + "x";
			}
			
			if(play.getPP() > 0.0 || play.getPPForFC() > 0.0){
				ppText +=  "**";
				
				if(play.isPersonalBest()) ppText += "";
				else ppText += "~";
				
				ppText += Utils.df(play.getPP(), 2) + "pp**";
				
				if(play.getRawMode() == 0 && play.getPPForFC() > 0.0 && !play.isPerfect() && play.getPP() != play.getPPForFC() && play.getPP() < play.getPPForFC())
					ppText += " (" + Utils.df(play.getPPForFC(), 2) + "pp for FC)";
			}
			
			if(play.getMapCompletion() > 0 && play.getMapCompletion() < 100)
				mapCompletionText = "**" + play.getMapCompletion() + "%** completion";
			
			// to compare fetched plays with these to find out if it the fetched play got a map leaderboard spot
			DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			List<RecentPlay> recentPlays = TrackingUtils.fetchPlayerRecentPlays(jsonUser.getJSONArray("events"), new CustomDate(formatter.format(new Date(0))));
			if(recentPlays.size() > 0) Collections.reverse(recentPlays);
			
			int mapRank = 0;
			RecentPlay recent = null;
			
			for(RecentPlay rPlay : recentPlays)
				if(rPlay.getBeatmapId() == play.getBeatmapId() && rPlay.isDateValid(play.getDate(), 15) && (recent == null || rPlay.getDate().after(recent.getDate()))){
					mapRank = rPlay.getRank();
					recent = rPlay;
				}
			
			if(mapRank > 0) mapRankText = "**#" + mapRank + " on map**";
			
			if(play.isPersonalBest()){
				pbText = "**#" + play.getPersonalBestCount() + "** personal best";
				
				if(player != null) {
					double oldPP = player.getPP();
					boolean ppDiff = false;
					
					if(play.getDate().after(player.getLastRankUpdate()) && Math.abs(player.getPP() - player.getOldPP()) > 0 && player.getOldPP() != 0){
						oldPP = player.getOldPP();
						ppDiff = true;
					}else if(Math.abs(player.getPP() - jsonUser.getDouble("pp_raw")) > 0 && player.getPP() > 0) ppDiff = true;
					
					if(ppDiff){
						double diff = jsonUser.getDouble("pp_raw") - oldPP;
						pbText += " (" + (diff > 0 ? "+" : "-") + Utils.df(Math.abs(diff), 2) + "pp)";
					}
				}
			}
			
			if(player != null){
				if((Math.abs(jsonUser.getDouble("pp_raw") - player.getPP()) > 0 && player.getPP() != 0) || 
				   (Math.abs(jsonUser.getInt("pp_rank") - player.getRank()) > 0 && player.getRank() != 0))
					player.setStats(jsonUser.getDouble("pp_raw") + "&r=" + jsonUser.getInt("pp_rank") + "&cr=" + jsonUser.getInt("pp_country_rank"));
			}
			
			builder.appendDescription("\u25b8 " + accText + " • " + comboText + (ppText.length() > 0 ? " • " + ppText : "") +
						  			  "\n\u25b8 " + (hitText.length() > 0 ? hitText + " • " : "") + rankText + " • " + scoreText + 
						  			  (mapCompletionText.length() > 0 ? " • " + mapCompletionText : "") + (mapRankText.length() > 0 ? " • " + mapRankText : "") +
						  			  "\n\u25b8 " + (pbText.length() > 0 ? pbText + " • " : "") + tryText);
			
			String beatmapInfoText = "\n\nCS **" + Utils.df(play.getCircleSize(), 2) + 
					 "** • AR **" + Utils.df(play.getApproachRate(), 2) +
					 "** • OD **" + Utils.df(play.getOverallDifficulty(), 2) +
					 "** • HP **" + Utils.df(play.getHPDrain(), 2) +
					 "** • **" + Utils.df(play.getStarRating(), 2) + "**\u2605";

			beatmapInfoText += "\n**" + play.getFormattedTotalLength() + "** (**" + play.getFormattedDrainLength() + "** drain)" + 
											   " • **" + Utils.df(play.getBPM(), 2) + "**bpm";
			
			builder.appendDescription(beatmapInfoText);
			
			builder.setFooter("Mapset by " + play.getCreator() + " • " + play.getRankedStatus() + " at " + play.getLastUpdateDate().getDate() + " UTC",
					  					"http://b.ppy.sh/thumb/" + play.getBeatmapSetId() + "l.jpg");
			
			latestRecents.put(e.getChannel().getId(), play.getBeatmapId() + "-" + play.getBeatmapSetId());
			
			Utils.infoBypass(e.getChannel(), builder.build());
		}
	}
	
	private String getOrdinalString(int number) {
		if(number >= 50) return "+";
		
		int digit = number % 10;
		boolean ignoreOrdinal = number - digit == 10;

		if(!ignoreOrdinal)
			switch(digit){
				case 1: return "st";
				case 2: return "nd";
				case 3: return "rd";
			}
		
		return "th";
	}
}
