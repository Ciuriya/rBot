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
		text += Utils.df(play.getAccuracy()) + "%" + (fullHitText.length() > 0 ? " • " + fullHitText : "") + "\n";
		
		// 94,441,230 • 2055/2056 • S rank • #9 on map
		text += play.getScore() + " • " + (play.isPerfect() ? "FC (" + play.getCombo() + "x)" :
										   play.getCombo() + (!play.hasMapCombo() ? "x" : "/" + play.getMaxCombo())) +
				" • " + play.getFormattedRank() + " rank" + (play.getMapRank() > 0 ? " • **#" + play.getMapRank() + "** on map" : "");
		        
		// 280.75pp (280.88pp for FC)
		String pbUnderline = play.isPersonalBest() ? "__" : "";
		text += play.getPP() > 0.0 ? "\n**" + (play.isPersonalBest() ? pbUnderline : "~") + Utils.df(play.getPP(), 2) + "pp" + pbUnderline + "**" + 
				(play.getRawMode() == 0 && play.getPPForFC() > 0.0 ? " (" + Utils.df(play.getPPForFC(), 2) + "pp for FC)" : "") : "";
		        
		// 6557.62pp (+0.28pp) • #30 personal best
		// #1,834 (-1) • #77 CA (0)
		text += "\n\n" + (Math.abs(play.getPPChange()) >= 0.01 ? player.getPP() + "pp (**" + play.getFormattedPPChange() + "**)" + 
				(play.isPersonalBest() ? " • **#" + play.getPersonalBestCount() + "** personal best\n" : "\n") : "") + 
				(Math.abs(play.getRankChange()) >= 1 ? "#" + Utils.veryLongNumberDisplay(player.getRank()) + " (**" + play.getFormattedRankChange() + "**) • #" +
				Utils.veryLongNumberDisplay(player.getCountryRank()) + " " + play.getCountry() + " (**" + play.getFormattedCountryRankChange() + "**)\n\n" :
				(Math.abs(play.getPPChange()) >= 0.01 && play.getPP() > 0.0 ? "\n" : ""));
				
		// Map • http://osu.ppy.sh/b/923985 • Ranked
		text += "Map • <http://osu.ppy.sh/b/" + play.getBeatmapId() + "> • " + play.getRankedStatus() + "\n";
		
		// - Auto - • http://osu.ppy.sh/u/4891293
		text += player.getUsername() + " • <http://osu.ppy.sh/u/" + player.getUserId() + ">\n";
		
		// BG • http://b.ppy.sh/thumb/428052l.jpg
		text += "BG • http://b.ppy.sh/thumb/" + play.getBeatmapSetId() + "l.jpg";
		
		Utils.info(guild.getChannel(player), text);
	}

}
