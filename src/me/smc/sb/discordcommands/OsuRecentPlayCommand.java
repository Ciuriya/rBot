package me.smc.sb.discordcommands;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import me.smc.sb.main.Main;
import me.smc.sb.tracking.CustomDate;
import me.smc.sb.tracking.OsuRecentPlaysRequest;
import me.smc.sb.tracking.OsuRequest;
import me.smc.sb.tracking.OsuUserRequest;
import me.smc.sb.tracking.RecentPlay;
import me.smc.sb.tracking.RequestTypes;
import me.smc.sb.tracking.TrackedPlay;
import me.smc.sb.tracking.TrackedPlayer;
import me.smc.sb.tracking.TrackingUtils;
import me.smc.sb.utils.Utils;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

public class OsuRecentPlayCommand extends GlobalCommand{

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
		
		OsuRequest recentPlaysRequest = new OsuRecentPlaysRequest(RequestTypes.API, "" + osuProfile, "" + 0);
		Object recentPlaysObj = Main.hybridRegulator.sendRequest(recentPlaysRequest);
		
		if(recentPlaysObj != null && recentPlaysObj instanceof JSONArray){
			JSONArray jsonResponse = (JSONArray) recentPlaysObj;
			
			if(jsonResponse.length() == 0) {
				Utils.info(e.getChannel(), "This player has no recent plays!");
				return;
			}
			
			JSONObject jsonObj = jsonResponse.getJSONObject(0);
			TrackedPlay play = new TrackedPlay(jsonObj, 0);
			
			play.loadMap();
			play.loadPP();
			
			String name = Utils.getOsuPlayerName(Utils.stringToInt(osuProfile), true);
			TrackedPlayer player = TrackedPlayer.get(Utils.stringToInt(osuProfile), 0);
			String rankDisplay = "";
			
			if(player != null) {
				rankDisplay = " • #" + Utils.veryLongNumberDisplay(player.getRank());
				
				if(player.getCountryRank() > 0 && player.getCountry() != null)
					rankDisplay += " (" + player.getCountry() + " #" + Utils.veryLongNumberDisplay(player.getCountryRank()) + ")";
			}
			
			EmbedBuilder builder = new EmbedBuilder();
			double fcPercentage = 1.0;
			
			if(play.hasCombo() && play.hasMapCombo())
				fcPercentage = (double) play.getCombo() / (double) play.getMaxCombo();
			
			builder.setColor(Utils.getPercentageColor(fcPercentage));
			builder.setThumbnail("http://b.ppy.sh/thumb/" + play.getBeatmapSetId() + "l.jpg");
			builder.setAuthor(name + " • " + play.getMode() + rankDisplay, "https://osu.ppy.sh/u/" + osuProfile, "https://a.ppy.sh/" + osuProfile);
			
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
				
				if(play.getRawMode() == 0 && play.getPPForFC() > 0.0 && !play.isPerfect() && play.getPP() != play.getPPForFC())
					ppText += " (" + Utils.df(play.getPPForFC(), 2) + "pp for FC)";
			}
			
			if(play.getMapCompletion() > 0 && play.getMapCompletion() < 100)
				mapCompletionText = "**" + play.getMapCompletion() + "%** completion";
			
			OsuRequest userRequest = new OsuUserRequest(RequestTypes.API, osuProfile, "0");
			Object userObj = Main.hybridRegulator.sendRequest(userRequest);
			JSONObject jsonUser = null;
			
			if(userObj != null && userObj instanceof JSONObject)
				jsonUser = (JSONObject) userObj;
			
			// to compare fetched plays with these to find out if it the fetched play got a map leaderboard spot
			DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			List<RecentPlay> recentPlays = TrackingUtils.fetchPlayerRecentPlays(jsonUser.getJSONArray("events"), new CustomDate(formatter.format(new Date(0))));
			
			if(recentPlays.size() > 0) {
				for(RecentPlay rPlay : recentPlays) {
					System.out.println("rPlay id " + rPlay.getBeatmapId() + " play id " + play.getBeatmapId() + " valid: " + rPlay.isDateValid(play.getDate(), play.getTotalLength()) + " rank " + rPlay.getRank());
					if(rPlay.getBeatmapId() == play.getBeatmapId() && rPlay.isDateValid(play.getDate(), play.getTotalLength()) && rPlay.getRank() > 0){
						mapRankText = "**#" + rPlay.getRank() + " on map**";
						break;
					}
				}
			}
			
			builder.appendDescription("▸ " + accText + " • " + comboText + (ppText.length() > 0 ? " • " + ppText : "") +
										  			  "\n▸ " + (hitText.length() > 0 ? hitText + " • " : "") + rankText + " • " + scoreText  + 
										  			  (mapCompletionText.length() > 0 ? " • " + mapCompletionText : "") + (mapRankText.length() > 0 ? " • " + mapRankText : ""));
			
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
			
			Utils.infoBypass(e.getChannel(), builder.build());
		}
	}
}
