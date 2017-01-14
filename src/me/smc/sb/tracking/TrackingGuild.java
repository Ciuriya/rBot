package me.smc.sb.tracking;

import net.dv8tion.jda.core.entities.TextChannel;

public class TrackingGuild{

	private String guildId;
	private TextChannel trackUpdateChannel;
	
	public TrackingGuild(String guildId){
		this.guildId = guildId;
		
		load();
	}
	
	public String getGuildId(){
		return guildId;
	}
	
	public void load(){
		
	}
}
