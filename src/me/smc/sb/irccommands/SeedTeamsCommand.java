package me.smc.sb.irccommands;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.json.JSONObject;
import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.PrivateMessageEvent;

import me.smc.sb.main.Main;
import me.smc.sb.perm.Permissions;
import me.smc.sb.tourney.Player;
import me.smc.sb.tourney.Team;
import me.smc.sb.tourney.Tournament;
import me.smc.sb.tracking.OsuRequest;
import me.smc.sb.tracking.OsuUserRequest;
import me.smc.sb.tracking.RequestTypes;
import me.smc.sb.utils.Log;
import me.smc.sb.utils.Utils;

public class SeedTeamsCommand extends IRCCommand{

	public SeedTeamsCommand(){
		super("Sorts out all teams from highest to lowest average rank.",
			  "<tournament name> <top x per team> <country sort?> <sheet format?>",
			  Permissions.TOURNEY_ADMIN,
			  "seedteams");
	}
	
	@Override
	public String onCommand(MessageEvent e, PrivateMessageEvent pe, String discord, String[] args){
		String argCheck = Utils.checkArguments(args, 3);
		if(argCheck.length() > 0) return argCheck;
		
		String tournamentName = "";
		
		for(int i = 0; i < args.length - 3; i++) tournamentName += args[i] + " ";
		Tournament t = Tournament.getTournament(tournamentName.substring(0, tournamentName.length() - 1));
		
		if(t == null) return "Invalid tournament!";
		if(Team.getTeams(t).size() < 1) return "Invalid amount of teams!";
		
		String user = Utils.toUser(e, pe);
		
		try{
			if(t.isAdmin(user)){
				boolean cs = false;
				boolean sf = false;
				int topX = Utils.stringToInt(args[args.length - 3]);
				
				if(args[args.length - 1].equalsIgnoreCase("true")) sf = true;
				if(args[args.length - 2].equalsIgnoreCase("true")) cs = true;
				if(Utils.stringToInt(args[args.length - 3]) == -1) return "Top X per team must be a number";
				
				JSONObject countryList = new JSONObject(new String(Files.readAllBytes(Paths.get("countries.json")))).getJSONObject("countries");
				final boolean countrySort = cs;
				final boolean sheetFormat = sf;
				
				Thread thread = new Thread(new Runnable(){
					public void run(){
						List<String> failedPlayers = new ArrayList<>();
						Map<Float, Team> teams = new HashMap<>();
						Map<Team, String> teamZoneInfo = new HashMap<>();
						Map<String, Map<Float, Team>> zoneSorting = new HashMap<>();
						
						for(Team team : Team.getTeams(t)){
							List<Integer> ranks = new ArrayList<>();
							List<String> zones = new ArrayList<>();
							
							for(Player player : team.getPlayers()){
								OsuRequest playerRequest = new OsuUserRequest(RequestTypes.API, player.getName(), "" + t.getInt("mode"), "string");
								Object playerObj = Main.hybridRegulator.sendRequest(playerRequest, true);
	
								if(playerObj == null || !(playerObj instanceof JSONObject)){
									int retries = 10; //retries left
									
									while(retries > 0 && (playerObj == null || !(playerObj instanceof JSONObject))){
										playerObj = Main.hybridRegulator.sendRequest(playerRequest, true);
										
										retries--;
										
										Utils.sleep(500);
									}
									
									if(retries == 0) failedPlayers.add(player.getName());
								}	
								
								try{
									ranks.add(((JSONObject) playerObj).getInt("pp_rank"));
									
									if(countrySort){
										String country = ((JSONObject) playerObj).getString("country");
										zones.add(countryList.getJSONObject(country).getJSONArray("timezones").getString(0).split("\\/")[0]);
									}
								}catch(Exception ex){
									ex.printStackTrace();
								}
								
								Utils.sleep(150);
							}
							
							int quantity = 0;
							float average = 0;
							
							Collections.sort(ranks);
							
							try{
								for(int i = 0; i < topX; i++)
									if(i == ranks.size()) break;
									else{
										average += ranks.get(i);
										quantity++;
									}
							}catch(Exception e){
								Log.logger.log(Level.SEVERE, e.getMessage() + " (team: " + team.getTeamName() + ")", e);
							}
							
							average /= (float) quantity;
							
							teams = new HashMap<>();
							teams.put(average, team);
							
							if(countrySort){
								Map<String, Integer> zonePopulation = new HashMap<>();
								String medianZone = "";
								int mostPlayersInZone = 0;
								String zoneInfo = "";
								
								for(String zone : zones){
									if(zonePopulation.containsKey(zone))
										zonePopulation.put(zone, zonePopulation.get(zone) + 1);
									else zonePopulation.put(zone, 1);
									
									if(mostPlayersInZone == 0){
										medianZone = zone;
										mostPlayersInZone = 1;
									}
									
									if(zonePopulation.get(zone) > mostPlayersInZone){
										medianZone = zone;
										mostPlayersInZone = zonePopulation.get(zone);
									}
								}
								
								if(sheetFormat) zoneInfo = medianZone;
								else{
									zoneInfo = "Median: " + medianZone + " | ";
									
									for(String zone : zonePopulation.keySet())
										zoneInfo += zone + ": " + zonePopulation.get(zone) + " | ";
									
									zoneInfo = zoneInfo.substring(0, zoneInfo.length() - 3);
								}

								teamZoneInfo.put(team, zoneInfo);
								
								if(!zoneSorting.containsKey(medianZone))
									zoneSorting.put(medianZone, teams);
								else{
									teams = new HashMap<>(zoneSorting.get(medianZone));
									teams.put(average, team);
									zoneSorting.put(medianZone, teams);
								}
							}
						}
						
						String msg = "```diff\n!== [Team averages in " + t.get("name") + "] ==!\n";

						try{
							if(countrySort){
								for(String zone : zoneSorting.keySet()){
									if(zone.length() > 2){
										System.out.println("sorting " + zone);
										msg = "```diff\n!== [Team averages in " + t.get("name") + " for " + zone + "] ==!\n";
										msg += sortTeams(zoneSorting.get(zone), teamZoneInfo, sheetFormat);
										postMessage(e, pe, discord, msg);
									}
								}
							}else{
								msg += sortTeams(teams, null, sheetFormat);
								postMessage(e, pe, discord, msg);
							}
						}catch(Exception ex){
							ex.printStackTrace();
						}
						
						if(failedPlayers.size() > 0){
							msg = "```diff\n!== [Failed] ==!";
							
							for(String failed : failedPlayers)
								msg += "\n- " + failed;
							
							Utils.info(e, pe, discord, msg + "```");
						}
					}
				});
				thread.start();
			}
		}catch(Exception ex){
			ex.printStackTrace();
		}

		return "";
	}
	
	private String sortTeams(Map<Float, Team> teams, Map<Team, String> teamZoneInfo, boolean sheetFormat){
		String lastSign = "+";
		String msg = "";
		int length = teams.size();
		
		for(int i = 0; i < length; i++){
			float avg = teams.keySet().stream().max(new Comparator<Float>(){
				@Override
				public int compare(Float o1, Float o2){
					return Float.compare(o2, o1);
				}
			}).orElse(0f);
			
			String temp = "";
			

			
			if(sheetFormat){
				try{
					temp = "\n" + teams.get(avg).getTeamName() + "," + Utils.df(avg, 0);
				}catch(Exception e){
					temp = "\n" + lastSign + " | An error occured! | - #" + Utils.df(avg, 0);
				}
				
				if(teamZoneInfo != null) temp += "," + teamZoneInfo.get(teams.get(avg));
			}else{
				try{
					temp = "\n" + lastSign + " " + teams.get(avg).getTeamName() + " - #" + Utils.df(avg, 0);
				}catch(Exception e){
					temp = "\n" + lastSign + " | An error occured! | - #" + Utils.df(avg, 0);
				}
				
				if(teamZoneInfo != null) temp += "\n" + lastSign + " " + teamZoneInfo.get(teams.get(avg));
			}
			
			msg += temp;
			
			if(lastSign.equals("+")) lastSign = "-";
			else lastSign = "+";
			
			teams.remove(avg);
		}
		
		return msg;
	}
	
	private void postMessage(MessageEvent e, PrivateMessageEvent pe, String discord, String message){
		String msg = message;
		
		while(msg.length() > 1800){
			String part = msg.substring(0, 1800);
			msg = msg.substring(1800);
			
			Utils.info(e, pe, discord, part + "```");
			
			msg = "```diff\n" + msg;
		}
		
		Utils.info(e, pe, discord, msg + "```");
	}
}
