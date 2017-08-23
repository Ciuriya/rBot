package me.smc.sb.discordcommands;

import org.json.JSONArray;
import org.json.JSONObject;

import me.smc.sb.main.Main;
import me.smc.sb.perm.Permissions;
import me.smc.sb.scoringstrategies.ScoringStrategy;
import me.smc.sb.tracking.CustomDate;
import me.smc.sb.tracking.OsuRecentPlaysRequest;
import me.smc.sb.tracking.OsuRequest;
import me.smc.sb.tracking.RequestTypes;
import me.smc.sb.tracking.TrackedPlay;
import me.smc.sb.tracking.TrackingUtils;
import me.smc.sb.utils.Utils;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

public class SMTScoreRecentCalculationCommand extends GlobalCommand{

	public SMTScoreRecentCalculationCommand(){
		super(Permissions.IRC_BOT_ADMIN, 
			  " - Calculates the osu!player's latest score using SMT Score.", 
			  "{prefix}smtscore\nThis command calculates the player's latest score using custom scoring\n\n" +
			  "----------\nUsage\n----------\n{prefix}smtscore {osu!player} - Calculates the player's latest score using custom scoring\n\n" + 
		      "----------\nAliases\n----------\nThere are no aliases.",  
			  true, 
			  "smtscore");
	}

	@Override
	public void onCommand(MessageReceivedEvent e, String[] args){
		try{
			e.getMessage().delete().complete();
		}catch(Exception ex){}
		
		if(!Utils.checkArguments(e, args, 1)) return;
		
		String userId = Utils.getOsuPlayerId(args[0], true);
		OsuRequest request = new OsuRecentPlaysRequest(RequestTypes.API, userId, "0", "50");
		Object response = Main.hybridRegulator.sendRequest(request, true);
		
		if(response != null && response instanceof JSONArray){
			JSONArray recentPlays = (JSONArray) response;
			TrackedPlay play = null;
			
			for(int i = recentPlays.length() - 1; i >= 0; i--){	
				JSONObject jsonObj = recentPlays.getJSONObject(i);
				
				if(jsonObj.getString("rank").equalsIgnoreCase("F")) continue;
				
				if(play == null || new CustomDate(TrackingUtils.osuDateToCurrentDate(jsonObj.getString("date"))).after(play.getDate()))
					play = new TrackedPlay(jsonObj, 0);
			}
			
			if(play == null){
				Utils.info(e.getChannel(), "This player did not submit any plays in the last 24 hours!");
				
				return;
			}
			
			play.loadMap();
			
			long score = ScoringStrategy.findStrategy("smtscore").calculateScore(null, play, false, null);
			
			// shamelessly stolen from my embed playformat, this is temporary anyway
			EmbedBuilder builder = new EmbedBuilder();
			
			double fcPercentage = 1.0;
			
			if(play.hasCombo() && play.hasMapCombo())
				fcPercentage = (double) play.getCombo() / (double) play.getMaxCombo();
			
			builder.setColor(Utils.getPercentageColor(fcPercentage));
			
			builder.setThumbnail("http://b.ppy.sh/thumb/" + play.getBeatmapSetId() + "l.jpg");
			
			builder.setAuthor(args[0] + " • " + play.getMode() + " • " + play.getDate().getDate() + " UTC", 
							  "https://osu.ppy.sh/u/" + userId, 
							  "https://a.ppy.sh/" + userId);
			
			builder.setTitle(TrackingUtils.escapeCharacters(play.getArtist() + " - " + play.getTitle() + 
							 " [" + play.getDifficulty() + "] " + play.getModDisplay() + "\n"), 
							 "http://osu.ppy.sh/b/" + play.getBeatmapId());
			
			builder.addField("Score", "Total: " + Utils.veryLongNumberDisplay(score) + "\n" +
									  "Combo: " + Utils.veryLongNumberDisplay(Utils.df(Double.parseDouble(play.playGet("combo_score")))) + "\n" +
									  "Accuracy: " + Utils.veryLongNumberDisplay(Utils.df(Double.parseDouble(play.playGet("acc_score")))), true);
			
			String rankText = play.getFormattedRank();
			
			builder.addField("Rank", rankText, true);
			
			String fullHitText = play.getFullHitText();
			String accText = Utils.df(play.getAccuracy()) + "%";
			
			if(fullHitText.length() > 0)
				accText += "\n" + fullHitText;
			
			builder.addField("Accuracy", accText, true);
			
			String comboText = "";
			
			if(play.isPerfect())
				comboText += "FC (" + Utils.veryLongNumberDisplay(play.getCombo()) + "x)";
			else{
				comboText += Utils.veryLongNumberDisplay(play.getCombo()) + "x";
				
				if(play.hasMapCombo())
					comboText += " / " + Utils.veryLongNumberDisplay(play.getMaxCombo()) + "x";
			}
			
			builder.addField("Combo", comboText, true);
			
			String beatmapInfoText = "CS **" + Utils.df(play.getCircleSize(), 2) + 
									 "** • AR **" + Utils.df(play.getApproachRate(), 2) +
									 "** • OD **" + Utils.df(play.getOverallDifficulty(), 2) +
									 "** • HP **" + Utils.df(play.getHPDrain(), 2) +
									 "** • **" + Utils.df(play.getStarRating(), 2) + "**\u2605";

			beatmapInfoText += "\n**" + play.getFormattedTotalLength() + "** (**" + play.getFormattedDrainLength() + "** drain)" +
							   " • **" + Utils.df(play.getBPM(), 2) + "**bpm";
			
			beatmapInfoText += "\nDownload from [osu](https://osu.ppy.sh/d/" + play.getBeatmapSetId() + ")" +
							   " - [no video](https://osu.ppy.sh/d/" + play.getBeatmapSetId() + "n)" +
							   " • [osu!direct](osu://b/" + play.getBeatmapId() + ")" +
							   " • [bloodcat](https://bloodcat.com/osu/s/" + play.getBeatmapSetId() + ")";
			
			builder.addField("Beatmap Information", beatmapInfoText, true);
			
			builder.setFooter("Mapset by " + play.getCreator() + " • " + play.getRankedStatus() + " at " + play.getLastUpdateDate().getDate() + " UTC",
						  	  "http://b.ppy.sh/thumb/" + play.getBeatmapSetId() + "l.jpg");
			
			Utils.info(e.getChannel(), builder.build());
		}else Utils.info(e.getChannel(), "Could not contact the osu!servers to fetch this player's plays!");
	}
}
