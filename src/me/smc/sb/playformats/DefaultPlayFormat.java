package me.smc.sb.playformats;

import me.smc.sb.tracking.PlayFormat;
import me.smc.sb.tracking.TrackedPlay;
import me.smc.sb.tracking.TrackedPlayer;
import me.smc.sb.tracking.TrackingGuild;
import me.smc.sb.tracking.TrackingUtils;
import me.smc.sb.utils.Utils;

public class DefaultPlayFormat extends PlayFormat{

	public DefaultPlayFormat(){
		super("default");
	}

	@Override
	public void send(TrackingGuild guild, TrackedPlay play, TrackedPlayer player){
		String text = "";
		
		// - Auto - | Standard | 2017-04-16 15:28:31 UTC
		text += "\n\n**" + player.getUsername() + " | " + play.getMode() + " | **__**" + play.getDate().getDate() + " UTC**__\n\n";
		
		// Halozy - eliminate anthem [Demarcation]
		String modHighlight = (play.getMods().size() > 0 ? "**" : "");
		text += TrackingUtils.escapeCharacters(play.getArtist() + " - " + play.getTitle() + 
				" [" + play.getDifficulty() + "] " + modHighlight + play.getModDisplay() + modHighlight + "\n");
		
		// 99.6% • 9x100 
		String fullHitText = play.getFullHitText();
		String accText = Utils.df(play.getAccuracy()) + "%";
		
		if(fullHitText.length() > 0)
			accText += " • " + fullHitText;
		
		text += accText + "\n";
		
		// 94,441,230 � 2055/2056 � S rank � #9 on map
		String playInfoText = play.getScore() + " • ";
		
		if(play.isPerfect())
			playInfoText += "FC (" + play.getCombo() + "x)";
		else{
			playInfoText += play.getCombo();
			
			if(play.hasMapCombo())
				playInfoText += "/" + play.getMaxCombo();
			else playInfoText += "x";
		}
		
		playInfoText += " • " + play.getFormattedRank() + " rank";
		
		if(play.getMapRank() > 0)
			playInfoText += " • **#" + play.getMapRank() + "** on map";
		
		text += playInfoText;
		        
		// 280.75pp (280.88pp for FC)
		String pbUnderline = play.isPersonalBest() ? "__" : "";
		String ppText = "";
		
		if(play.getPP() > 0.0){
			ppText += "\n**";
			
			if(play.isPersonalBest())
				ppText += pbUnderline;
			else ppText += "~";
			
			ppText += Utils.df(play.getPP(), 2) + "pp" + pbUnderline + "**";
			
			if(play.getRawMode() == 0 && play.getPPForFC() > 0.0){
				if(!play.isPerfect() && play.getPP() != play.getPPForFC())
					ppText += " (" + Utils.df(play.getPPForFC(), 2) + "pp for FC)";
				
				ppText += "\n*" + play.getAimPP() + " aim* • *" + play.getSpeedPP() + " speed* • *" + play.getAccPP() + " acc*";
			}
		}
		
		text += ppText;
		        
		// 6557.62pp (+0.28pp) � #30 personal best
		// #1,834 (-1) � #77 CA (0)
		String playerRankChangesText = "\n\n";
		
		if(Math.abs(play.getPPChange()) >= 0.01){
			playerRankChangesText += player.getPP() + "pp (**" + play.getFormattedPPChange() + "**)";
			
			if(play.isPersonalBest())
				playerRankChangesText += " • **#" + play.getPersonalBestCount() + "** personal best\n";
			else playerRankChangesText += "\n";
		}
		
		if(Math.abs(play.getRankChange()) >= 1){
			playerRankChangesText += "#" + Utils.veryLongNumberDisplay(player.getRank()) + " (**" + play.getFormattedRankChange() + "**) • #" +
									 Utils.veryLongNumberDisplay(player.getCountryRank()) + " " + play.getCountry() + 
									 " (**" + play.getFormattedCountryRankChange() + "**)\n\n";
		}else if(Math.abs(play.getPPChange()) >= 0.01 && play.getPP() > 0.0)
			playerRankChangesText += "\n";
		
		text += playerRankChangesText;
				
		// Map � http://osu.ppy.sh/b/923985 � Ranked
		text += "Map • <http://osu.ppy.sh/b/" + play.getBeatmapId() + "> • " + play.getRankedStatus() + "\n";
		
		// - Auto - � http://osu.ppy.sh/u/4891293
		text += player.getUsername() + " • <http://osu.ppy.sh/u/" + player.getUserId() + ">\n";
		
		// BG � http://b.ppy.sh/thumb/428052l.jpg
		text += "BG • http://b.ppy.sh/thumb/" + play.getBeatmapSetId() + "l.jpg";
		
		Utils.info(guild.getChannel(player), text);
	}

}
