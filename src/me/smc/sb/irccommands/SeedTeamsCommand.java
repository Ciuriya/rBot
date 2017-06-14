package me.smc.sb.irccommands;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.PrivateMessageEvent;

import me.smc.sb.multi.Player;
import me.smc.sb.multi.Team;
import me.smc.sb.multi.Tournament;
import me.smc.sb.perm.Permissions;
import me.smc.sb.utils.Log;
import me.smc.sb.utils.Utils;

public class SeedTeamsCommand extends IRCCommand{

	public SeedTeamsCommand(){
		super("Sorts out all teams from highest to lowest average rank.",
			  "<tournament name> <top x per team>",
			  Permissions.TOURNEY_ADMIN,
			  "seedteams");
	}
	
	@Override
	public String onCommand(MessageEvent e, PrivateMessageEvent pe, String discord, String[] args){
		String argCheck = Utils.checkArguments(args, 1);
		if(argCheck.length() > 0) return argCheck;
		
		String tournamentName = "";
		
		for(int i = 0; i < args.length - 1; i++) tournamentName += args[i] + " ";
		Tournament t = Tournament.getTournament(tournamentName.substring(0, tournamentName.length() - 1));
		
		if(t == null) return "Invalid tournament!";
		if(t.getTeams().size() < 1) return "Invalid amount of teams!";
		
		String user = Utils.toUser(e, pe);
		
		if(t.isAdmin(user)){
			if(Utils.stringToInt(args[args.length - 1]) == -1) return "Top X per team must be a number";
			int topX = Utils.stringToInt(args[args.length - 1]);
			
			Thread thread = new Thread(new Runnable(){
				public void run(){
					Map<Float, Team> teams = new HashMap<>();
					
					for(Team team : t.getTeams()){
						List<Integer> ranks = new ArrayList<>();
						
						for(Player player : team.getPlayers()){
							int rank = Utils.getOsuPlayerRank(player.getName(), t.getMode(), true);

							if(rank == -1){
								int retries = 5; //retries left
								
								while(retries > 0 && rank == -1){
									rank = Utils.getOsuPlayerRank(player.getName(), t.getMode(), true);
									
									retries--;
									
									Utils.sleep(250);
								}
							}

							if(rank != -1) ranks.add(rank);
							
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
						
						teams.put(average, team);
					}
					
					String msg = "```diff\n!== [Team averages in " + t.getName() + "] ==!\n";
					
					String lastSign = "+";
					
					for(int i = 0; i < t.getTeams().size(); i++){
						float avg = teams.keySet().stream().max(new Comparator<Float>(){
							@Override
							public int compare(Float o1, Float o2){
								return Float.compare(o2, o1);
							}
						}).orElse(0f);
						
						String temp = "";
						
						try{
							temp = "\n" + lastSign + " " + teams.get(avg).getTeamName() + " - #" + Utils.df(avg, 0);
						}catch(Exception e){
							temp = "\n" + lastSign + " | An error occured! | - #" + Utils.df(avg, 0);
						}
						
						msg += temp;
						
						if(lastSign.equals("+")) lastSign = "-";
						else lastSign = "+";
						
						teams.remove(avg);
					}
					
					Utils.info(e, pe, discord, msg + "```");
				}
			});
			thread.start();
		}

		return "";
	}
}
