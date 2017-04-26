package me.smc.sb.irccommands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.pircbotx.PircBotX;
import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.PrivateMessageEvent;

import me.smc.sb.multi.Match;
import me.smc.sb.multi.Player;
import me.smc.sb.multi.Team;
import me.smc.sb.multi.Tournament;
import me.smc.sb.perm.Permissions;
import me.smc.sb.utils.Utils;

public class ScanCheatersCommand extends IRCCommand{
	
	public ScanCheatersCommand(){
		super("Scans registered players to try and identify potential cheaters.",
			  "<tournament name> <players who are playing only (true/false)> ",
			  Permissions.IRC_BOT_ADMIN,
			  "scancheaters");
	}

	@Override
	public String onCommand(MessageEvent<PircBotX> e, PrivateMessageEvent<PircBotX> pe, String discord, String[] args){
		if(discord == null || e != null || pe != null) return "You can only use this command from discord!";
		
		String argCheck = Utils.checkArguments(args, 2);
		if(argCheck.length() > 0) return argCheck;
		
		String tournamentName = "";
		
		for(int i = 0; i < args.length - 1; i++) tournamentName += args[i] + " ";
		Tournament t = Tournament.getTournament(tournamentName.substring(0, tournamentName.length() - 1));
		
		if(t == null) return "Invalid tournament!";
		if(!args[args.length - 1].equalsIgnoreCase("true") && !args[args.length - 1].equalsIgnoreCase("false")){
			return "Players who are playing only value has to be a boolean (true or false)!";
		}

		boolean playingOnly = Boolean.parseBoolean(args[args.length - 1]);
		List<Team> playingTeams = new ArrayList<>();
		
		if(playingOnly)
			for(Match match : t.getMatches()){
				playingTeams.add(match.getFirstTeam());
				playingTeams.add(match.getSecondTeam());
			}
		
		Thread thread = new Thread(new Runnable(){
			public void run(){
				String msg = "```diff\n! --[Sharpest Rank Increases]--";
				
				Map<Float, String> sharpRankIncreases = new HashMap<>();
				Map<Float, String> largestPPGaps = new HashMap<>();
				
				for(Team team : t.getTeams()){
					if(playingOnly && !playingTeams.contains(team)) continue;
					for(Player player : team.getPlayers()){
						String[] general = Utils.getHTMLCode("https://osu.ppy.sh/pages/include/profile-general.php?u=" + Utils.getOsuPlayerId(player.getName()) + "&m=" + t.getMode());
						String[] leader = new String[]{};
						
						if(general.length > 10){
							leader = Utils.getHTMLCode("https://osu.ppy.sh/pages/include/profile-leader.php?u=" + Utils.getOsuPlayerId(player.getName()) + "&m=" + t.getMode());
							String[] lowerPage = Utils.getHTMLCode("https://osu.ppy.sh/pages/include/profile-leader.php?u=" + Utils.getOsuPlayerId(player.getName()) + "&m=" + t.getMode() + "&pp=1");
							leader = Stream.concat(Arrays.stream(leader), Arrays.stream(lowerPage)).toArray(String[]::new);
						}
						
						int[] rankIncrease = calculateRankIncrease(general);
						
						if(rankIncrease.length == 3){
							int minRank = rankIncrease[0];     // best rank
							int maxRank = rankIncrease[1];     // shittiest rank
							int largestDiff = rankIncrease[2]; // biggest absolute difference of rank between 2 days
							float percentIncrease = ((float) maxRank / (float) minRank) * 100 - 100;
							float largestDiffPercent = ((float) largestDiff / (float) maxRank) * 100;
							
							if(percentIncrease >= 50 || largestDiffPercent >= 20){
								sharpRankIncreases.put(percentIncrease + largestDiffPercent, "- " + player.getName() + " - " + 
																		Utils.veryLongNumberDisplay(maxRank) + " to " + Utils.veryLongNumberDisplay(minRank) +
																		"\n+ " + Utils.df(percentIncrease, 2) + "% total increase | " + 
																		Utils.df(largestDiffPercent, 2) + "% maximal daily increase");
							}
							
							if(leader.length > 25){
								int[] ppGap = findPPGap(leader);
								
								if(ppGap.length == 2){
									int minPP = ppGap[0];
									int maxPP = ppGap[1];
									float percentPPDiff = ((float) maxPP / (float) minPP) * 100;
									
									if(percentPPDiff >= 200){
										largestPPGaps.put(percentPPDiff, "- " + player.getName() + " - " + 
																		 Utils.veryLongNumberDisplay(minPP) + " to " + Utils.veryLongNumberDisplay(maxPP) +
																		 "\n+ " + Utils.df(percentPPDiff, 2) + "% difference between lowest and highest play");
									}
								}
							}
						}

						Utils.sleep(2500);
					}
				}
				
				Object[] objRIArray = sharpRankIncreases.keySet().toArray();
				Object[] objPPGArray = largestPPGaps.keySet().toArray();
				Arrays.sort(objRIArray);
				Arrays.sort(objPPGArray);
				
				for(int i = objRIArray.length - 1; i >= 0; i--){
					float totalPercents = (float) objRIArray[i];
					
					msg += "\n\n" + sharpRankIncreases.get(totalPercents);
				}
				
				msg += "\n\n! --[Largest PP Gaps In Tops]--";
				
				for(int i = objPPGArray.length - 1; i >= 0; i--){
					float percentDiff = (float) objPPGArray[i];
					
					msg += "\n\n" + largestPPGaps.get(percentDiff);
				}
				
				msg += "```";
				
				if(msg.length() > 2000){
					int max = (int) Math.ceil((double) msg.length() / 1996.0);
					
					for(int i = 0; i < max; i++){
						String message = "";
						
						if(i != 0) message = "```diff\n";
						
						if(i != max - 1) message += msg.substring(i * 1996, (i + 1) * 1996) + "```";
						else message += msg.substring(i * 1996, msg.length());
						
						Utils.info(e, pe, discord, message);
					}
				}else Utils.info(e, pe, discord, msg);
			}
		});
		
		thread.start();
		
		return "";
	}
	
	private int[] calculateRankIncrease(String[] html){
		List<String> valueLine = Utils.getNextLineCodeFromLink(html, 4, "function data");
		
		if(valueLine.size() > 0){
			String line = valueLine.get(0).substring(valueLine.get(0).indexOf("[[") + 1);
			int minRank = 0;
			int maxRank = 0;
			int lastRank = 0;
			int largestDiff = 0;
			
			for(String dailyStat : line.split("\\[")){
				try{
					int rank = Utils.stringToInt(dailyStat.split(",")[1].split("\\]")[0].substring(1));
					if(rank == -1) throw new Exception();
					
					if(minRank == 0 || rank < minRank) minRank = rank;
					if(rank > maxRank) maxRank = rank;
					
					int diff = lastRank - rank;
					
					if(lastRank == 0) diff = 0;
					if(diff > largestDiff) largestDiff = diff;
					
					lastRank = rank;
				}catch(Exception ex){}
			}
			
			return new int[]{minRank, maxRank, largestDiff};
		}
		
		return new int[]{};
	}
	
	private int[] findPPGap(String[] html){
		List<String> ppLines = Utils.getAllLinesFromLink(html, 13, "id='performance-");
		
		if(ppLines.size() > 0){
			int lowestPlay = 0;
			int highestPlay = 0;
			
			for(String play : ppLines){
				try{
					int pp = Utils.stringToInt(play.split(">")[1].split("pp")[0]);
					if(pp == -1) throw new Exception();
					
					if(lowestPlay == 0 || pp < lowestPlay) lowestPlay = pp;
					if(pp > highestPlay) highestPlay = pp;
				}catch(Exception ex){}
			}
			
			return new int[]{lowestPlay, highestPlay};
		}
		
		return new int[]{};
	}

}
