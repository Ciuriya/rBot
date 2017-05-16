package me.smc.sb.polls;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import me.smc.sb.main.Main;
import me.smc.sb.utils.Configuration;
import me.smc.sb.utils.Utils;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.TextChannel;

public class Poll{ // make sure there's no - in name

	public static List<Poll> polls = new ArrayList<>();
	
	private String name;
	private Guild guild;
	private long expirationTime;
	private List<Option> options;
	private Timer expiryTimer;
	
	public Poll(Guild guild, String name){
		this.name = name;
		this.guild = guild;
		this.options = new ArrayList<>();
		
		load();
		startExpiryTimer();
		polls.add(this);
	}
	
	public Poll(Guild guild, String name, long expirationTime, List<String> options){
		this.name = name;
		this.expirationTime = expirationTime;
		this.options = new ArrayList<>();
		this.guild = guild;
		
		for(String option : options)
			this.options.add(new Option(option));
		
		save();
		startExpiryTimer();
		polls.add(this);
	}
	
	public String getName(){
		return name;
	}
	
	public List<Option> getOptions(){
		return options;
	}
	
	public Guild getGuild(){
		return guild;
	}
	
	public long getExpirationTime(){
		return expirationTime;
	}
	
	public static TextChannel getResultChannel(Guild guild){
		Configuration config = Main.serverConfigs.get(guild.getId());
		
		String channelId = config.getValue("poll-result-channel");
		
		if(channelId.length() > 0)
			return Main.api.getTextChannelById(channelId);
		
		return guild.getPublicChannel();
	}
	
	public static void setResultChannel(TextChannel channel){
		Configuration config = Main.serverConfigs.get(channel.getGuild().getId());
		
		config.writeValue("poll-result-channel", channel.getId());
	}
	
	public void postResults(){
		EmbedBuilder embed = new EmbedBuilder();
		
		embed.setColor(Color.WHITE);
		embed.setTitle(name + " results", ""); // maybe gen charts later
		
		Collections.sort(options, new Comparator<Option>(){
			@Override
			public int compare(Option o1, Option o2) {
				int votes1 = o1.getVotes().size();
				int votes2 = o2.getVotes().size();
				
				if(votes1 > votes2)
					return 1;
				else if(votes2 > votes1)
					return -1;
				else
					return 0;
			}
		});
		
		for(Option option : options)
			embed.addField(option.getName(), option.getVotes().size() + " votes", true);
		
		Utils.infoBypass(getResultChannel(guild), embed.build());
	}
	
	public void startExpiryTimer(){
		long delay = expirationTime - System.currentTimeMillis();
		
		if(delay <= 0){
			postResults();
			delete();
			
			return;
		}
		
		expiryTimer = new Timer();
		expiryTimer.schedule(new TimerTask(){
			public void run(){
				postResults();
				delete();
			}
		}, delay);
	}
	
	public void save(){
		Configuration config = Main.serverConfigs.get(guild.getId());
		
		if(!config.getStringList("polls").contains(name))
			config.appendToStringList("polls", name, true);
		
		if(expirationTime > 0)
			config.writeValue("poll-" + name + "-expire", expirationTime);
		
		ArrayList<String> exportedOptions = new ArrayList<>();
		
		for(Option option : options){
			exportedOptions.add(option.getName());
			
			config.writeStringList("poll-" + name + "-" + option + "-votes", option.getVotes(), false);
		}
		
		config.writeStringList("poll-" + name + "-options", exportedOptions, false);
	}
	
	public void load(){
		Configuration config = Main.serverConfigs.get(guild.getId());
		
		this.expirationTime = config.getLong("poll-" + name + "-expire");
		
		ArrayList<String> exportedOptions = config.getStringList("poll-" + name + "-options");
		
		if(exportedOptions.size() > 0)
			for(String exported : exportedOptions){
				ArrayList<String> votes = config.getStringList("poll-" + name + "-" + exported + "-votes");
				
				options.add(new Option(exported, votes));
			}
	}
	
	public void delete(){
		Configuration config = Main.serverConfigs.get(guild.getId());
		
		config.removeFromStringList("polls", name, true);
		config.deleteKey("poll-" + name + "-expire");
		
		ArrayList<String> exportedOptions = config.getStringList("poll-" + name + "-options");
		
		if(exportedOptions.size() > 0)
			for(String exported : exportedOptions)
				config.deleteKey("poll-" + name + "-" + exported + "-votes");
			
		config.deleteKey("poll-" + name + "-options");
		
		if(expiryTimer != null) expiryTimer.cancel();
	}
}
