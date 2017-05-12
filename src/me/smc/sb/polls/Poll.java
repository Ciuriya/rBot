package me.smc.sb.polls;

import java.util.ArrayList;
import java.util.List;

import me.smc.sb.main.Main;
import me.smc.sb.utils.Configuration;
import net.dv8tion.jda.core.entities.Guild;

public class Poll{

	public static List<Poll> polls = new ArrayList<>();
	
	private String name;
	private Guild guild;
	private long expirationTime;
	private List<Option> options;
	
	public Poll(Guild guild, String name, long expirationTime, List<String> options){
		this.name = name;
		this.expirationTime = expirationTime;
		this.options = new ArrayList<>();
		this.guild = guild;
		
		for(String option : options)
			this.options.add(new Option(option));
		
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
	
	public void save(){
		Configuration config = Main.serverConfigs.get(guild.getId());
		//config.appendToStringList(key, val, sort);
	}
}
