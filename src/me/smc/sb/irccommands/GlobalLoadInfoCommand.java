package me.smc.sb.irccommands;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.PrivateMessageEvent;

import me.smc.sb.main.Main;
import me.smc.sb.perm.Permissions;
import me.smc.sb.tourney.Match;
import me.smc.sb.tourney.Tournament;
import me.smc.sb.utils.Utils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;

public class GlobalLoadInfoCommand extends IRCCommand{
	
	public GlobalLoadInfoCommand(){
		super("Lists info about the tourney load on bot",
			  " ",
			  Permissions.BOT_ADMIN,
			  "loadinfo");
	}
	
	@Override
	public String onCommand(MessageEvent e, PrivateMessageEvent pe, String discord, String[] args){
		if(discord == null || e != null || pe != null) return "You can only use this command from discord!";
		
		Map<String, Integer> loads = new HashMap<>();
		
		for(Tournament t : Tournament.tournaments)
			for(Match m : Match.getMatches(t)){
				try{
					String date = Utils.toDate(m.getTime()).split(":")[0];
					int count = 0;
					
					if(m.getTime() < Utils.getCurrentTimeUTC()) continue;
					if(date.startsWith("19")) continue;
					if(loads.containsKey(date)) count = loads.get(date);
					
					count++;
					loads.put(date, count);
				}catch(Exception ex){}
			}
		
		String perHourDate = "";
		int perHourCount = 0;
		String nextMatches = "";
		int nmCount = 0;
		
		for(String hour : loads.keySet())
			if(loads.get(hour) > perHourCount){
				perHourCount = loads.get(hour);
				perHourDate = hour;
			}
		
		for(Match m : Match.matches.stream().sorted(new Comparator<Match>(){
			@Override
			public int compare(Match o1, Match o2){
				return Long.compare(o1.getTime(), o2.getTime());
			}
		}).collect(Collectors.toList())){
			if(nmCount >= 10) break;
			if(nextMatches.length() != 0) nextMatches += "\n\n";
			
			nextMatches += m.getLobbyName() + "\n" + Utils.toDate(m.getTime());
			
			nmCount++;
		}
		
		EmbedBuilder builder = new EmbedBuilder();
		
		builder.setTitle("rBot Tournament Load Information");
		builder.addField("Most Matches Per Hour", perHourDate + " UTC\n" + perHourCount + " matches", true);
		builder.addField("Upcoming Matches", nextMatches, true);
		
		Utils.infoBypass(Main.api.getChannelById(MessageChannelUnion.class, discord), builder.build());
		
		return "";
	}
}
