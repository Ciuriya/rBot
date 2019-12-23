package me.smc.sb.discordcommands;

import java.awt.Color;

import org.json.JSONArray;
import org.json.JSONObject;

import me.smc.sb.main.Main;
import me.smc.sb.tracking.OsuRequest;
import me.smc.sb.tracking.OsuScoresRequest;
import me.smc.sb.tracking.OsuUserRequest;
import me.smc.sb.tracking.RequestTypes;
import me.smc.sb.tracking.TrackedPlay;
import me.smc.sb.tracking.TrackingUtils;
import me.smc.sb.utils.Utils;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

public class OsuScoresCommand extends GlobalCommand{

	public OsuScoresCommand() {
		super(null, 
				" - Shows your plays on a certain map", 
				"{prefix}osuscores\nThis command lets you see an osu! player's latest play\n\n" +
				"----------\nUsage\n----------\n{prefix}osuscores (map link) - Shows your plays on the map\n" + 
				"{prefix}osuscore - Shows your plays on the map in relation to the last posted map\n\n" +
			    "----------\nAliases\n----------\n{prefix}scores", 
			    true, 
				"osuscores", "scores", "compare");
	}

	@Override
	public void onCommand(MessageReceivedEvent e, String[] args) {
		Utils.deleteMessage(e.getChannel(), e.getMessage());
		
		String osuProfile = OsuSetProfileCommand.config.getValue(e.getAuthor().getId());
		
		if(osuProfile.length() == 0) {
			Utils.info(e.getChannel(), "Your osu! profile is not set! Use " + Main.getCommandPrefix(e.getGuild().getId()) + "osuset {player}");
			return;
		}
		
		boolean hasRecent = false;
		
		if(OsuRecentPlayCommand.latestRecents.containsKey(e.getChannel().getId())) hasRecent = true;
		
		if(!hasRecent && (args.length == 0 || !args[0].contains("ppy.sh"))){
			Utils.info(e.getChannel(), "You have not provided a beatmap link! Use " + Main.getCommandPrefix(e.getGuild().getId()) + "scores {map link}");
			return;
		}
		
		String setId = "";
		String beatmapId = "";
		
		if(args.length > 0) {
			if(args[0].contains("osu.ppy.sh/beatmapsets/")) {
				String[] split = args[0].split("\\/");
				
				beatmapId = split[split.length - 1];
				
				if(args[0].contains("/#"))
					setId = split[split.length - 3];
				else setId = split[split.length - 2].replace("#osu", "").replace("#taiko", "").replace("#fruits", "").replace("#mania", "");
			}else{
				Utils.info(e.getChannel(), "Please use the new site's links!");
				return;
			}
		}else{
			String recent = OsuRecentPlayCommand.latestRecents.get(e.getChannel().getId());
			
			beatmapId = recent.split("-")[0];
			setId = recent.split("-")[1];
		}
		
		OsuRequest scoresRequest = new OsuScoresRequest(RequestTypes.API, beatmapId, "" + osuProfile, "" + 0, "id");
		Object scoresObj = Main.hybridRegulator.sendRequest(scoresRequest, true);
		
		if(scoresObj != null && scoresObj instanceof JSONArray){
			JSONArray jsonResponse = (JSONArray) scoresObj;
			
			if(jsonResponse.length() == 0) {
				Utils.info(e.getChannel(), "This player has no scores on this map!");
				return;
			}
			
			JSONObject jsonUser = null;
			OsuRequest userRequest = new OsuUserRequest(RequestTypes.API, "" + osuProfile, "0");
			Object userObj = Main.hybridRegulator.sendRequest(userRequest, true);
			
			if(userObj != null && userObj instanceof JSONObject)
				jsonUser = (JSONObject) userObj;
			
			EmbedBuilder builder = new EmbedBuilder();
			
			builder.setColor(Color.CYAN);
			builder.setThumbnail("http://b.ppy.sh/thumb/" + setId + "l.jpg");
			builder.setAuthor("Top Scores for " + jsonUser.getString("username") + " (#" + jsonUser.getInt("pp_rank") + ")", 
							  "https://osu.ppy.sh/users/" + osuProfile, "https://a.ppy.sh/" + osuProfile);
			
			boolean infoSet = false;
			String beatmapInfoText = "";
			
			for(int i = 0; i < jsonResponse.length(); i++){
				JSONObject jsonObj = jsonResponse.getJSONObject(i);
				TrackedPlay play = new TrackedPlay(jsonObj, 0);
				
				play.loadMap(Utils.stringToInt(beatmapId));
				play.loadPP();
				
				if(!infoSet) {
					builder.setTitle(TrackingUtils.escapeCharacters(play.getArtist() + " - " + play.getTitle() + 
									 " [" + play.getDifficulty() + "] "), 
									 "http://osu.ppy.sh/b/" + play.getBeatmapId());
					
					beatmapInfoText = "\n\nCS **" + Utils.df(play.getBaseCircleSize(), 2) + 
									  "** • AR **" + Utils.df(play.getBaseApproachRate(), 2) +
									  "** • OD **" + Utils.df(play.getBaseOverallDifficulty(), 2) +
									  "** • HP **" + Utils.df(play.getBaseHPDrain(), 2) +
									  "** • **" + Utils.df(play.getBaseStarRating(), 2) + "**\u2605";

					beatmapInfoText += "\n**" + play.getFormattedBaseTotalLength() + "** (**" + play.getFormattedBaseDrainLength() + "** drain)" + 
									   " • **" + Utils.df(play.getBaseBPM(), 2) + "**bpm";
					
					builder.setFooter("Mapset by " + play.getCreator() + " • " + play.getRankedStatus() + " at " + play.getLastUpdateDate().getDate() + " UTC",
							  					"http://b.ppy.sh/thumb/" + setId + "l.jpg");
					
					infoSet = true;
				}
				
				String rankText = play.getFormattedRank();
				String comboText = "";
				String ppText = "";
				String accText = Utils.df(play.getAccuracy()) + "%";
				String hitText = play.getFullHitText();
				String scoreText = play.getScore();
				String modText = play.getModDisplay().replace("+", "");
				
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
				
				if(modText.length() == 0) modText = "No Mod";
				
				modText = "**" + modText + "** • " + Utils.df(play.getStarRating(), 2) + "\u2605";
				
				if(i > 0) modText = "\n" + modText;
				
				builder.appendDescription(modText +
										  "\n\u25b8 " + accText + " • " + comboText + (ppText.length() > 0 ? " • " + ppText : "") +
							  			  "\n\u25b8 " + (hitText.length() > 0 ? hitText + " • " : "") + rankText + " • " + scoreText);
			}
			
			builder.appendDescription(beatmapInfoText);
			
			OsuRecentPlayCommand.latestRecents.put(e.getChannel().getId(), beatmapId + "-" + setId);
			
			Utils.infoBypass(e.getChannel(), builder.build());
		}
	}
}
