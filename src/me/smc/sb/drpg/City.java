package me.smc.sb.drpg;

import java.util.ArrayList;
import java.util.List;

public class City{

	private int id;
	private String name;
	private String desc;
	private Territory territory;
	public static List<City> cities = new ArrayList<>();
	
	public City(int id){
		this.id = id;
		
		load();
	}
	
	public City(int id, String name, String desc, int territoryId){
		this.id = id;
		this.name = name;
		this.desc = desc; //can be null
		this.territory = Territory.getTerritory(territoryId);
		
		if(id == -1) //-1 for adding
			insert();
		
		cities.add(this);
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
	
	public Territory getTerritory(){
		return territory;
	}
	
	public static City getCity(int id){
		return cities.stream().filter(c -> c.id == id).findFirst().orElse(null);
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
