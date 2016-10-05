package me.smc.sb.irccommands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;import java.util.stream.Collector;

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
			  "<tournament name> <leniency (0-100, 0 being super strict)> <players who are playing only (true/false)> ",
			  Permissions.IRC_BOT_ADMIN,
			  "scancheaters");
	}

	@Override
	public String onCommand(MessageEvent<PircBotX> e, PrivateMessageEvent<PircBotX> pe, String discord, String[] args){
		if(discord == null || e != null || pe != null) return "You can only use this command from discord!";
		
		String argCheck = Utils.checkArguments(args, 3);
		if(argCheck.length() > 0) return argCheck;
		
		String tournamentName = "";
		
		for(int i = 0; i < args.length - 2; i++) tournamentName += args[i] + " ";
		Tournament t = Tournament.getTournament(tournamentName.substring(0, tournamentName.length() - 1));
		
		if(t == null) return "Invalid tournament!";
		if(Utils.stringToInt(args[args.length - 2]) == -1) return "Leniency has to be a number!";
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
				String msg = "```diff \n+ Sharp Rank Increases\n\n";
				
				Map<Integer, String> sharpRankIncreases = new HashMap<>();
				
				for(Team team : t.getTeams()){
					if(playingOnly && !playingTeams.contains(team)) continue;
					for(Player player : team.getPlayers()){
						String[] html = Utils.getHTMLCode("https://osu.ppy.sh/pages/include/profile-history.php?u=" + Utils.getOsuPlayerId(player.getName()) + "&m=" + t.getMode());
						int rankIncrease = findAverageRankIncrease(html, player);
						//check cheats
						Utils.sleep(2500);
					}
				}
				
				Object[] objRIArray = sharpRankIncreases.keySet().toArray();
				Arrays.sort(objRIArray);
				
				for(Object objRankIncrease : objRIArray){
					int rankIncrease = (int) objRankIncrease;
					
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
	
	private int findAverageRankIncrease(String[] html, Player player){
		return -1;
	}

}
