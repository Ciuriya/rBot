package me.smc.sb.drpg;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Inventory{

	private int id;
	private float maxWeight;
	private EQuest quest;
	private Entity entity;
	private Guild guild;
	private List<Item> contained;
	public static List<Inventory> inventories = new ArrayList<>();
	
	public Inventory(int id){
		this.id = id;
		
		load();
	}
	
	public Inventory(int id, float maxWeight, int questId, int entityId, int guildId){
		this.id = id;
		this.maxWeight = maxWeight;
		
		if(questId != -1) quest = EQuest.getQuest(questId);
		if(entityId != -1 && questId == -1) entity = Entity.getEntity(entityId);
		if(guildId != -1 && entityId == -1 && questId == -1) guild = Guild.getGuild(guildId);
		
		contained = new ArrayList<>();
		Item.items.stream().filter(i -> i.getInventory().id == id).forEach(i -> contained.add(i));
		
		if(id == -1) //-1 for adding
			insert();
		
		inventories.add(this);
	}
	
	public int getId(){
		return id;
	}
	
	public float getMaxWeight(){
		return maxWeight;
	}
	
	public Object getHolder(){
		if(quest != null) return quest;
		else if(entity != null) return entity;
		else return guild;
	}
	
	public EQuest getQuest(){
		return quest;
	}
	
	public Entity getEntity(){
		return entity;
	}
	
	public Guild getGuild(){
		return guild;
	}
	
	public List<Item> getItems(){
		return contained;
	}
	
	public List<Item> getEquippedItems(){
		return contained.stream().filter(i -> i.isEquipped()).collect(Collectors.toList());
	}
	
	public static Inventory getInventory(int id){
		return inventories.stream().filter(i -> i.id == id).findFirst().orElse(null);
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
