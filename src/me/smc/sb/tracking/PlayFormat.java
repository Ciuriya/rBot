package me.smc.sb.tracking;

import java.util.ArrayList;
import java.util.List;

import me.smc.sb.playformats.DefaultPlayFormat;
import me.smc.sb.playformats.EmbedPlayFormat;

public abstract class PlayFormat{

	private String name;
	public static List<PlayFormat> registeredFormats = new ArrayList<>();
	
	public PlayFormat(String name){
		this.name = name;
		
		registeredFormats.add(this);
	}
	
	public String getName(){
		return name;
	}
	
	public abstract void send(TrackingGuild guild, TrackedPlay play, TrackedPlayer player);
	
	public static PlayFormat get(String name){
		if(!registeredFormats.isEmpty()){
			for(PlayFormat format : registeredFormats){
				if(format.getName().equalsIgnoreCase(name)){
					return format;
				}
			}
		}
		
		return registeredFormats.get(0);
	}
	
	public static void loadFormats(){
		new DefaultPlayFormat();
		new EmbedPlayFormat();
	}
	
}