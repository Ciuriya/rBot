package me.smc.sb.playformats;

import me.smc.sb.tracking.CustomDate;
import me.smc.sb.tracking.PlayFormat;
import me.smc.sb.tracking.TrackedPlay;
import me.smc.sb.tracking.TrackedPlayer;
import me.smc.sb.tracking.TrackingGuild;
import me.smc.sb.tracking.TrackingUtils;
import me.smc.sb.utils.Utils;
import net.dv8tion.jda.api.EmbedBuilder;

public class EmbedPlayFormat extends PlayFormat{

	public EmbedPlayFormat(){
		super("embed");
	}

	@Override
	public void send(TrackingGuild guild, TrackedPlay play, TrackedPlayer player){
		EmbedBuilder builder = new EmbedBuilder();
		
		double fcPercentage = 1.0;
		
		if(play.hasCombo() && play.hasMapCombo())
			fcPercentage = (double) play.getCombo() / (double) play.getMaxCombo();
		
		builder.setColor(Utils.getPercentageColor(fcPercentage));
		
		builder.setThumbnail("http://b.ppy.sh/thumb/" + play.getBeatmapSetId() + "l.jpg");
		
		builder.setAuthor(player.getUsername() + " • " + play.getMode() + " • " + play.getDate().getDate() + " UTC", 
						  "https://osu.ppy.sh/users/" + player.getUserId(), 
						  "https://a.ppy.sh/" + player.getUserId());
		
		builder.setTitle(TrackingUtils.escapeCharacters(play.getArtist() + " - " + play.getTitle() + 
						 " [" + play.getDifficulty() + "] " + play.getModDisplay() + "\n"), 
						 "http://osu.ppy.sh/b/" + play.getBeatmapId());
		
		builder.addField("Score", play.getScore(), true);
		
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
		
		if(play.getPP() > 0.0){
			String ppText = "**";
			
			if(play.isPersonalBest()) ppText += "";
			else ppText += "~";
			
			ppText += Utils.df(play.getPP(), 2) + "pp**";
			
			if(play.getRawMode() == 0 && play.getPPForFC() > 0.0){
				ppText += "\n*" + play.getAimPP() + " aim* • *" + play.getSpeedPP() + " speed* • *" + play.getAccPP() + " acc*";
				
				if(!play.isPerfect() && play.getPP() != play.getPPForFC() && play.getPP() < play.getPPForFC())
					ppText += "\n" + Utils.df(play.getPPForFC(), 2) + "pp for FC";
			}
			
			if(play.isPersonalBest())
				ppText += "\n**#" + play.getPersonalBestCount() + "** personal best";
			
			if(play.getMapRank() > 0)
				ppText += "\n**#" + play.getMapRank() + "** on map";
			
			builder.addField("Performance Points", ppText, true);
		}
		
		String playerStatsText = player.getPP() + "pp";
		
		if(Math.abs(play.getPPChange()) >= 0.01)
			playerStatsText += " (**" + play.getFormattedPPChange() + "**)";
		
		playerStatsText += "\n#" + Utils.veryLongNumberDisplay(player.getRank());
		
		if(Math.abs(play.getRankChange()) >= 1)
			playerStatsText += " (**" + play.getFormattedRankChange() + "**)";
		
		playerStatsText += "\n#" + Utils.veryLongNumberDisplay(player.getCountryRank()) + " " + play.getCountry();
		
		if(Math.abs(play.getCountryRankChange()) >= 1)
			playerStatsText += " (**" + play.getFormattedCountryRankChange() + "**)";
		
		builder.addField("Player Stats", playerStatsText, true);
		
		String beatmapInfoText = "CS **" + Utils.df(play.getCircleSize(), 2) + 
								 "** • AR **" + Utils.df(play.getApproachRate(), 2) +
								 "** • OD **" + Utils.df(play.getOverallDifficulty(), 2) +
								 "** • HP **" + Utils.df(play.getHPDrain(), 2) +
								 "** • **" + Utils.df(play.getStarRating(), 2) + "**\u2605";
		
		beatmapInfoText += "\n**" + play.getFormattedTotalLength() + "** (**" + play.getFormattedDrainLength() + "** drain)" +
						   " • **" + Utils.df(play.getBPM(), 2) + "**bpm";
		
		beatmapInfoText += "\nDownload from [osu](https://osu.ppy.sh/d/" + play.getBeatmapSetId() + ")" +
						   " - [no video](https://osu.ppy.sh/d/" + play.getBeatmapSetId() + "n)" +
						   " • [bloodcat](https://bloodcat.com/osu/s/" + play.getBeatmapSetId() + ")";
		
		builder.addField("Beatmap Information", beatmapInfoText, true);
		
		CustomDate lastUpdateDate = play.getLastUpdateDate();
		builder.setFooter("Mapset by " + play.getCreator() + " • " + play.getRankedStatus() + (lastUpdateDate != null ? " at " + lastUpdateDate.getDate() + " UTC" : ""),
						  "http://b.ppy.sh/thumb/" + play.getBeatmapSetId() + "l.jpg");
		
		Utils.info(guild.getChannel(player), builder.build());
	}

}
