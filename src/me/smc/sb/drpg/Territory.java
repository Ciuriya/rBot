package me.smc.sb.drpg;

import java.util.ArrayList;
import java.util.List;

public class Territory{

	private int id;
	private String name;
	private boolean controllable;
	private String desc;
	private Guild guild;
	private List<Tile> tiles;
	public static List<Territory> territories = new ArrayList<>();
	
	public Territory(int id){
		this.id = id;
		
		load();
	}
	
	public Territory(int id, String name, boolean controllable, String desc, int guildId){	
		this.id = id;
		this.name = name;
		this.controllable = controllable;
		this.desc = desc; //can be null
		if(guildId != -1 && controllable) guild = Guild.getGuild(guildId);
		
		tiles = new ArrayList<>();
		Tile.tiles.stream().filter(t -> t.getTerritory().id == id).forEach(t -> tiles.add(t));
		
		if(id == -1) //-1 for adding
			insert();
		
		territories.add(this);
	}
	
	public int getId(){
		return id;
	}
	
	public String getName(){
		return name;
	}
	
	public boolean isControllable(){
		return controllable;
	}
	
	public String getDescription(){
		return desc;
	}
	
	public Guild getGuild(){
		return guild;
	}
	
	public List<Tile> getTiles(){
		return tiles;
	}
	
	public static Territory getTerritory(int id){
		return territories.stream().filter(t -> t.id == id).findFirst().orElse(null);
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
