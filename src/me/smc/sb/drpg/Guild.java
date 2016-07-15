package me.smc.sb.drpg;

import java.util.ArrayList;
import java.util.List;

public class Guild{

	private int id;
	private String name;
	private String icon;
	private String desc;
	private Faction faction;
	public static List<Guild> guilds = new ArrayList<>();
	
	public Guild(int id){
		this.id = id;
		
		load();
	}
	
	public Guild(int id, String name, String icon, String desc, int factionId){
		this.id = id;
		this.name = name;
		this.icon = icon;
		this.desc = desc; //can be null
		if(factionId != -1) faction = Faction.getFaction(factionId);
		
		//add entities and make sure to unlink in entity delete
		
		if(id == -1) //-1 for adding
			insert();
		
		guilds.add(this);
	}
	
	public int getId(){
		return id;
	}
	
	public String getName(){
		return name;
	}
	
	public String getIcon(){
		return icon;
	}
	
	public String getDesc(){
		return desc;
	}
	
	public Faction getFaction(){
		return faction;
	}
	
	public static Guild getGuild(int id){
		return guilds.stream().filter(g -> g.id == id).findFirst().orElse(null);
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