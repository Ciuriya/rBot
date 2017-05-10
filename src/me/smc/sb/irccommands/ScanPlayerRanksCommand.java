package me.smc.sb.irccommands;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;
import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.PrivateMessageEvent;

import me.smc.sb.discordcommands.OsuStatsCommand;
import me.smc.sb.multi.Match;
import me.smc.sb.multi.Player;
import me.smc.sb.multi.Team;
import me.smc.sb.multi.Tournament;
import me.smc.sb.perm.Permissions;
import me.smc.sb.utils.Utils;

public class ScanPlayerRanksCommand extends IRCCommand{

	public ScanPlayerRanksCommand(){
		super("Scans registered players to make sure they are within the rank bracket.",
			  "<tournament name> <lower bound> <upper bound> <players who are playing only (true/false)> ",
			  Permissions.IRC_BOT_ADMIN,
			  "scanplayers");
	}

	@Override
	public String onCommand(MessageEvent e, PrivateMessageEvent pe, String discord, String[] args){
		if(discord == null || e != null || pe != null) return "You can only use this command from discord!";
		
		String argCheck = Utils.checkArguments(args, 4);
		if(argCheck.length() > 0) return argCheck;
		
		String tournamentName = "";
		
		for(int i = 0; i < args.length - 3; i++) tournamentName += args[i] + " ";
		Tournament t = Tournament.getTournament(tournamentName.substring(0, tournamentName.length() - 1));
		
		if(t == null) return "Invalid tournament!";
		if(Utils.stringToInt(args[args.length - 3]) == -1) return "Lower bound has to be a number!";
		if(Utils.stringToInt(args[args.length - 2]) == -1) return "Upper bound has to be a number!";
		if(!args[args.length - 1].equalsIgnoreCase("true") && !args[args.length - 1].equalsIgnoreCase("false")){
			return "Players who are playing only value has to be a boolean (true or false)!";
		}
		
		boolean playingOnly = Boolean.parseBoolean(args[args.length - 1]);
		Map<Player, Integer> ranks = new HashMap<>();
		List<Team> playingTeams = new ArrayList<>();
		
		if(playingOnly)
			for(Match match : t.getMatches()){
				playingTeams.add(match.getFirstTeam());
				playingTeams.add(match.getSecondTeam());
			}
		
		Thread thread = new Thread(new Runnable(){
			public void run(){
				String msg = "```";
				
				for(Team team : t.getTeams()){
					if(playingOnly && !playingTeams.contains(team)) continue;
					for(Player player : team.getPlayers()){
						ranks.put(player, findPlayerRank(player));
						Utils.sleep(2500);
					}
				}
				
				double averageRank = 0.0;
				int lowestRank = 10000000;
				int highestRank = 0;
				int count = 0;
				
				for(int rank : ranks.values())
					if(rank != -1){
						averageRank += rank;
						count += 1;
						
						if(rank < lowestRank) lowestRank = rank;
						if(rank > highestRank) highestRank = rank;
					}
				
				averageRank /= (double) count;
				
				msg += "Lowest Rank: " + Utils.df(lowestRank) + 
						"\nHighest Rank: " + Utils.df(highestRank) + 
						"\nAverage Rank: " + Utils.df(averageRank) + 
						"\nTotal valid players scanned: " + count + 
						"\n\nPlayers outside of rank bracket:\n";
				
				List<Player> banned = new ArrayList<>();
				int lowerBound = Utils.stringToInt(args[args.length - 3]);
				int upperBound = Utils.stringToInt(args[args.length - 2]);
				
				for(Player player : ranks.keySet()){
					int rank = ranks.get(player);
					if(rank == -1){
						banned.add(player);
						continue;
					}
					
					if(rank < lowerBound || rank > upperBound)
						msg += player.getName() + " - #" + rank + "\n";
				}
				
				if(!banned.isEmpty()){
					msg += "\nUnknown/banned players:\n";
					
					for(Player player : banned)
						msg += player.getName() + "\n";
				}
				
				msg += "```";
				
				if(msg.length() > 2000){
					int max = (int) Math.ceil((double) msg.length() / 1996.0);
					
					for(int i = 0; i < max; i++){
						String message = "";
						
						if(i != 0) message = "```";
						
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
	
	private int findPlayerRank(Player player){
		try{
			String post = Utils.sendPost("https://osu.ppy.sh/api/", "get_user?k=" + OsuStatsCommand.apiKey + "&u=" + player.getName().replaceAll(" ", "%20") + "&m=0&type=string&event_days=1");
			if(post == "" || !post.contains("{")) return -1;
			
			JSONObject jsonResponse = new JSONObject(post);
			return jsonResponse.getInt("pp_rank");	
		}catch(Exception ex){
			return -1;
		}
	}
	
}
