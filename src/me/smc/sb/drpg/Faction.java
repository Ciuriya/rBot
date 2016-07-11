package me.smc.sb.drpg;

import java.util.ArrayList;
import java.util.List;

public class Faction{

	private int id;
	private String name;
	private String desc;
	private Guild controllingGuild;
	public static List<Faction> factions = new ArrayList<>();
	
	public Faction(int id){
		this.id = id;
		
		load();
	}
	
	public Faction(int id, String name, String desc, int guildId){
		this.id = id;
		this.name = name;
		this.desc = desc; //can be null
		this.controllingGuild = Guild.getGuild(guildId);
		
		if(id == -1) //-1 for adding
			insert();
		
		factions.add(this);
	}
	
	public int getId(){
		return id;
	}
	
	public String getName(){
		return name;
	}
	
	public String getDescription(){
		return desc;
	}
	
	public Guild getControllingGuild(){
		return controllingGuild;
	}
	
	public static Faction getFaction(int id){
		return factions.stream().filter(f -> f.id == id).findFirst().orElse(null);
	}
	
	private void insert(){
		
	}
	
	public void save(){
		
	}
	
	public void delete(){
		
	}
	
	private void load(){
		
	}
	
}
