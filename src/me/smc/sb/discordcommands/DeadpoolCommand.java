package me.smc.sb.discordcommands;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;

import org.json.JSONArray;
import org.json.JSONObject;

import me.smc.sb.deadpool.DeadpoolReport;
import me.smc.sb.deadpool.DeadpoolUser;
import me.smc.sb.main.Main;
import me.smc.sb.utils.Configuration;
import me.smc.sb.utils.Log;
import me.smc.sb.utils.Utils;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

public class DeadpoolCommand extends GlobalCommand{
	
	public static String apiKey = "";
	public static String wclogsURL = "https://www.warcraftlogs.com:443/v1/";
	private static Timer fightRefresher;
	private static List<DeadpoolReport> reports;
	
	public DeadpoolCommand(){
		super(null, 
			  " - Allows votes on first deaths in WoW raids.", 
			  "{prefix}deadpool\nThis command lets you attempt to predict the first death of a WoW raid pull.\n\n" +
			  "----------\nUsage\n----------\n{prefix}deadpool start {report link} - Starts the deadpool in the current channel.\n" + 
			  "{prefix}deadpool v {player} - Votes for the specified player.\n{prefix}deadpool leaderboard - Shows who's won the most in the latest report.\n\n" +
			  "----------\nAliases\n----------\n{prefix}dp",
			  false, 
			  "deadpool", "dp");
		
		apiKey = new Configuration(new File("login.txt")).getValue("wclogsApi");
		reports = new ArrayList<>();
		startFightRefresher();
	}

	@Override
	public void onCommand(MessageReceivedEvent e, String[] args){
		Utils.deleteMessage(e.getChannel(), e.getMessage());
		if(!Utils.checkArguments(e, args, 1)) return;
		
		for(int i = 0; i < args.length; i++)
			if(args[i].equalsIgnoreCase("start") && args.length > 1) start(e, args[1]);
			else if(args[i].toLowerCase().startsWith("v") && args.length > 1) vote(e, args[1]);
			else if(args[i].equalsIgnoreCase("leaderboard")) leaderboard(e);
	}
	
	private void start(MessageReceivedEvent e, String reportLink){
		String reportCode = reportLink.split("reports/")[1];
		
		if(reportCode.contains("#")) reportCode = reportCode.split("#")[0];
		
		DeadpoolReport report = new DeadpoolReport(reportCode, e.getTextChannel());
		reports.add(report);
		
		Utils.info(e.getChannel(), "Deadpool started by " + e.getAuthor().getAsMention() + "" +
								   "\nUse **" + Main.getCommandPrefix(e.getGuild().getId()) + "dp v {player}** to vote on each pull!\n" +
								   "You can use **" + Main.getCommandPrefix(e.getGuild().getId()) + "dp leaderboard** to view the current standings!");
	}
	
	private void vote(MessageReceivedEvent e, String player){
		DeadpoolReport report = getReport(e);
		
		if(report == null){
			Utils.info(e.getChannel(), "No deadpools currently running!\nTo start one, use **" + Main.getCommandPrefix(e.getGuild().getId()) + "dp start {report link}**");
			return;
		}
		
		List<DeadpoolUser> voters = report.getVoters();
		DeadpoolUser prevVoter = voters.stream().filter(v -> Utils.levenshteinDistance(v.getCurrentVote(), player) <= player.length() / 2 + 1).findFirst().orElse(null);
		
		if(prevVoter != null && !prevVoter.getUser().getId().equalsIgnoreCase(e.getAuthor().getId())){
			Utils.info(e.getChannel(), e.getAuthor().getAsMention() + " Someone else has already voted for **" + player + "**");
			return;
		}
		
		DeadpoolUser voter = voters.stream().filter(v -> v.getUser().getId().equalsIgnoreCase(e.getAuthor().getId())).findFirst().orElse(null);
		
		if(voter == null){
			voter = new DeadpoolUser(e.getAuthor());
			voters.add(voter);
		}
		
		voter.setVote(player);
		
		Utils.info(e.getChannel(), e.getAuthor().getAsMention() + " voted for **" + player + "**");
	}
	
	private void leaderboard(MessageReceivedEvent e){
		String leaderboardMessage = "```diff\n- Deadpool Leaderboard -";
		DeadpoolReport report = getReport(e);
		
		if(report == null){
			Utils.info(e.getChannel(), "No deadpools currently running!\nTo start one, use **" + Main.getCommandPrefix(e.getGuild().getId()) + "dp start {report link}**");
			return;
		}
		
		List<DeadpoolUser> voters = report.getVoters();
		voters.sort(new Comparator<DeadpoolUser>(){
			@Override
			public int compare(DeadpoolUser o1, DeadpoolUser o2){
				if(o1.getPoints() == o2.getPoints()) return 0;
				
				return o1.getPoints() > o2.getPoints() ? -1 : 1;
			}
		});
		
		for(DeadpoolUser voter : voters)
			leaderboardMessage += "\n+ " + voter.getUser().getName() + " with " + voter.getPoints() + " points";
		
		Utils.info(e.getChannel(), leaderboardMessage + "```");
	}
	
	private DeadpoolReport getReport(MessageReceivedEvent e){
		for(DeadpoolReport listedReport : reports){
			if(listedReport.getReportChannel().getGuild().getId().equalsIgnoreCase(e.getGuild().getId()) &&
			   listedReport.getReportChannel().getId().equalsIgnoreCase(e.getTextChannel().getId())){
				return listedReport;
			}
		}
		
		return null;
	}
	
	private void startFightRefresher(){
		fightRefresher = new Timer();
		fightRefresher.scheduleAtFixedRate(new TimerTask(){
			public void run(){
				for(DeadpoolReport report : new ArrayList<>(reports)){
					if(report.isExpired()){
						reports.remove(report);
						continue;
					}
					
					try{
						String fightsPost = Utils.sendPost(wclogsURL, "report/fights/" + report.getId() + "?api_key=" + apiKey);
						
						if(fightsPost.equals("") || !fightsPost.contains("{")) continue;
						
						JSONObject fightsJSON = new JSONObject("{ " + fightsPost + " }");
						JSONArray fights = fightsJSON.getJSONArray("fights");
						JSONObject lastFight = fights.getJSONObject(fights.length() - 1);
						long startTime = lastFight.getLong("start_time");
						long endTime = lastFight.getLong("end_time");
						
						if(lastFight.getInt("id") <= report.getLastFightId()) continue;
						
						report.setLastActivity();
						
						String deathsPost = Utils.sendPost(wclogsURL, "report/tables/deaths/" + report.getId() + "?start=" + startTime +
																	  "&end=" + endTime + "&hostility=0&api_key=" + apiKey);
						
						if(deathsPost.equals("") || !deathsPost.contains("{")) continue;
						
						JSONObject deathsJSON = new JSONObject("{ " + deathsPost + " }");
						JSONArray deathsList = deathsJSON.getJSONArray("entries");
						List<DeadpoolUser> fightVoters = report.getVoters();
						int correctGuesses = 0;
						
						report.setLastFightId(lastFight.getInt("id"));
						
						if(fightVoters.isEmpty() || !fightVoters.stream().anyMatch(v -> v.getCurrentVote().length() > 0)) continue;
						
						String fightEndMessage = "```diff\n- Fight #" + lastFight.getInt("id") + " winners -";
						
						for(int i = 0; i < deathsList.length(); i++){
							String deathName = deathsList.getJSONObject(i).getString("name");
							DeadpoolUser closestUser = null;
							int closestDistance = 100;
							
							for(DeadpoolUser voter : fightVoters){
								if(voter.getCurrentVote().length() <= 0) continue;
								
								String vote = voter.getCurrentVote();
								
								if(voter.votedAfterFightStart(endTime - startTime))
									vote = voter.getPreviousVote();
								
								int distance = Utils.levenshteinDistance(vote, deathName);
								
								if(distance < closestDistance && distance >= 0 && distance <= deathName.length() / 2 + 1){
									closestUser = voter;
									closestDistance = distance;
								}
							}
							
							if(closestUser != null){
								closestUser.addPoints(3 - correctGuesses);
								correctGuesses++;
								
								fightEndMessage += "\n+ " + closestUser.getUser().getName() + " voted on " + deathName + 
												   " (" + closestUser.getPoints() + ")";
								
								if(correctGuesses >= 3) break;
							}
						}
						
						for(DeadpoolUser voter : report.getVoters()) voter.clearVote();
						
						Utils.info(report.getReportChannel(), fightEndMessage + "```");
					}catch(Exception e){
						Log.logger.log(Level.SEVERE, e.getMessage(), e);
					}
				}
			}
		}, 5000, 5000);
	}
}
