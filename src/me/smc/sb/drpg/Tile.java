package me.smc.sb.drpg;

import java.util.ArrayList;
import java.util.List;

public class Tile{

	private int id;
	private int x;
	private int y;
	private float size;
	private float difficulty;
	private float encounterChance;
	private Territory territory;
	public static List<Tile> tiles = new ArrayList<>();
	
	public Tile(int id){
		this.id = id;
		
		load();
	}
	
	public Tile(int id, int x, int y, float size, float difficulty, float encounterChance, int territoryId){
		this.id = id;
		this.x = x;
		this.y = y;
		this.size = size;
		this.difficulty = difficulty;
		this.encounterChance = encounterChance;
		this.territory = Territory.getTerritory(territoryId);
		
		if(id == -1) //-1 for adding
			insert();
		
		tiles.add(this);
	}
	
	public int getId(){
		return id;
	}
	
	public int getX(){
		return x;
	}
	
	public int getY(){
		return y;
	}
	
	public float getSize(){
		return size;
	}
	
	public float getDifficulty(){
		return difficulty;
	}
	
	public float getEncounterChance(){
		return encounterChance;
	}
	
	public Territory getTerritory(){
		return territory;
	}
	
	public static Tile getTile(int id){
		return tiles.stream().filter(t -> t.id == id).findFirst().orElse(null);
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
