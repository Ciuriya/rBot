package me.smc.sb.drpg;

import java.util.ArrayList;
import java.util.List;

public class Building{

	private int id;
	private float size;
	private String name;
	private String desc;
	private City city;
	private Entity entity;
	private Territory territory;
	private Guild guild;
	private Inventory inv;
	public static List<Building> buildings = new ArrayList<>();
	
	public Building(int id){
		this.id = id;
		
		load();
	}
	
	public Building(int id, float size, String name, String desc, int cityId,
					int entityId, int territoryId, int guildId, int inventoryId){
		this.id = id;
		this.size = size;
		this.name = name;
		this.desc = desc; //can be null
		if(cityId != -1) city = City.getCity(cityId);
		if(entityId != -1) entity = Entity.getEntity(entityId);
		if(territoryId != -1 && cityId == -1) territory = Territory.getTerritory(territoryId);
		if(guildId != -1 && entityId == -1) guild = Guild.getGuild(guildId);
		inv = Inventory.getInventory(inventoryId);
		
		if(id == -1) //-1 for adding
			insert();
		
		buildings.add(this);
	}
	
	public int getId(){
		return id;
	}
	
	public float getSize(){
		return size;
	}
	
	public String getName(){
		return name;
	}
	
	public String getDescription(){
		return desc;
	}
	
	public Object getOwner(){
		if(entity != null) return entity;
		else return guild;
	}
	
	public Object getParentLand(){
		if(city != null) return city;
		else return territory;
	}
	
	public City getCity(){
		return city;
	}
	
	public Entity getEntity(){
		return entity;
	}
	
	public Territory getTerritory(){
		return territory;
	}
	
	public Guild getGuild(){
		return guild;
	}
	
	public Inventory getInventory(){
		return inv;
	}
	
	public static Building getBuilding(int id){
		return buildings.stream().filter(b -> b.id == id).findFirst().orElse(null);
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
