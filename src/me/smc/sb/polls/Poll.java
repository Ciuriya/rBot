package me.smc.sb.polls;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import me.smc.sb.main.Main;
import me.smc.sb.utils.Configuration;
import me.smc.sb.utils.Utils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;

public class Poll{

	public static List<Poll> polls = new ArrayList<>();
	
	private String name;
	private Guild guild;
	private String author;
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
	
	public Poll(Guild guild, User author, String name, long expirationTime, List<String> options){
		this.name = name;
		this.expirationTime = expirationTime;
		this.options = new ArrayList<>();
		this.guild = guild;
		this.author = author.getId();
		
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
	
	public Option getOption(String name){
		for(Option option : options)
			if(option.getName().equalsIgnoreCase(name))
				return option;
		
		return null;
	}
	
	public boolean vote(String name, String userId){
		try{
			for(Option option : options)
				if(option.hasVoted(userId))
					option.removeVote(userId);
				
			boolean voted = getOption(name).addVote(userId);
			
			save();
			return voted;
		}catch(Exception e){
			return false;
		}
	}
	
	public Guild getGuild(){
		return guild;
	}
	
	public long getExpirationTime(){
		return expirationTime;
	}
	
	public User getAuthor(){
		return Main.api.getUserById(author);
	}
	
	public int getVoteCount(){
		int votes = 0;
		
		for(Option option : options)
			votes += option.getVotes().size();
		
		return votes;
	}
	
	public static TextChannel getResultChannel(Guild guild){
		Configuration config = Main.serverConfigs.get(guild.getId());
		
		String channelId = config.getValue("poll-result-channel");
		
		if(channelId.length() > 0)
			return Main.api.getTextChannelById(channelId);
		
		return guild.getDefaultChannel();
	}
	
	public static void setResultChannel(TextChannel channel){
		Configuration config = Main.serverConfigs.get(channel.getGuild().getId());
		
		config.writeValue("poll-result-channel", channel.getId());
	}
	
	public static Poll findPoll(String name, Guild guild){
		for(Poll poll : polls)
			if(poll.getName().equalsIgnoreCase(name) &&
			   poll.getGuild().getId().equalsIgnoreCase(guild.getId()))
				return poll;
		
		return null;
	}
	
	public static List<Poll> findPolls(Guild guild){
		List<Poll> polls = new ArrayList<>();
		
		for(Poll poll : Poll.polls)
			if(poll.getGuild().getId().equalsIgnoreCase(guild.getId()))
				polls.add(poll);
		
		return polls;
	}
	
	public void postResults(TextChannel channel){
		EmbedBuilder embed = new EmbedBuilder();
		
		TextChannel resultChannel = channel;
		if(resultChannel == null) resultChannel = getResultChannel(guild);
		
		User author = Main.api.getUserById(this.author);
		
		embed.setColor(Color.WHITE);
		embed.setAuthor("Poll | " + name + " | Results", "http://google.com", 
						author == null ? Main.api.getSelfUser().getAvatarUrl() : author.getAvatarUrl());
		
		if(expirationTime - System.currentTimeMillis() > 0)
			embed.addField("Ends in", Utils.toDuration(expirationTime - System.currentTimeMillis()), true);
		
		Collections.sort(options, new Comparator<Option>(){
			@Override
			public int compare(Option o1, Option o2) {
				int votes1 = o1.getVotes().size();
				int votes2 = o2.getVotes().size();
				
				if(votes1 < votes2)
					return 1;
				else if(votes2 < votes1)
					return -1;
				else
					return 0;
			}
		});
		
		String[][] values = new String[options.size()][2];
		
		int i = 0;
		
		for(Option option : options){
			values[i] = new String[2];
			values[i][0] = option.getName();
			values[i][1] = option.getVotes().size() + "";
			embed.addField(option.getName(), option.getVotes().size() + " vote" + (option.getVotes().size() > 1 ? "s" : ""), true);
			i++;
		}
		
		String chartUrl = Main.chartGenerator.generateChart("pie", name, getVoteCount(), values);
		
		if(chartUrl.length() > 0) embed.setImage(chartUrl);
		
		try{
			Utils.infoBypass(resultChannel, embed.build());
		}catch(Exception ex){}
	}
	
	public void startExpiryTimer(){
		if(expirationTime == 0) return;
		long delay = expirationTime - System.currentTimeMillis();
		
		if(delay <= 0){
			end(null);
			return;
		}
		
		expiryTimer = new Timer();
		expiryTimer.schedule(new TimerTask(){
			public void run(){
				end(null);
			}
		}, delay);
	}
	
	public void end(TextChannel channel){
		expirationTime = 0;
		postResults(channel);
		polls.remove(this);
		delete();
	}
	
	public void save(){
		Configuration config = Main.serverConfigs.get(guild.getId());
		
		if(!config.getStringList("polls").contains(name))
			config.appendToStringList("polls", name, true);
		
		if(expirationTime > 0)
			config.writeValue("poll-" + name + "-expire", expirationTime);
		
		config.writeValue("poll-" + name + "-author", author);
		
		ArrayList<String> exportedOptions = new ArrayList<>();
		
		for(Option option : options){
			exportedOptions.add(option.getName());
			
			config.writeStringList("poll-" + name + "-" + option.getName() + "-votes", option.getVotes(), false);
		}
		
		config.writeStringList("poll-" + name + "-options", exportedOptions, false);
	}
	
	public void load(){
		Configuration config = Main.serverConfigs.get(guild.getId());
		
		this.expirationTime = config.getLong("poll-" + name + "-expire");
		this.author = config.getValue("poll-" + name + "-author");
		
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
		config.deleteKey("poll-" + name + "-author");
		
		ArrayList<String> exportedOptions = config.getStringList("poll-" + name + "-options");
		
		if(exportedOptions.size() > 0)
			for(String exported : exportedOptions)
				config.deleteKey("poll-" + name + "-" + exported + "-votes");
			
		config.deleteKey("poll-" + name + "-options");
		
		if(expiryTimer != null) expiryTimer.cancel();
	}
	
	public static void loadPolls(Guild guild){
		Configuration config = Main.serverConfigs.get(guild.getId());
		
		List<String> pollList = config.getStringList("polls");
		
		if(pollList.size() > 0)
			for(String poll : pollList)
				new Poll(guild, poll);
	}
}
