package me.smc.sb.drpg;

import java.util.ArrayList;
import java.util.List;

public class EQuest{

	private int id;
	private Inventory inv;
	private Quest baseQuest;
	private EQuest required;
	private Entity entity;
	public static List<EQuest> quests = new ArrayList<>();
	
	public EQuest(int id){
		this.id = id;
		
		load();
	}
	
	public EQuest(int id, String name, int inventoryId, int questId, int entityId){
		this.id = id;
		baseQuest = Quest.getQuest(name);
		if(inventoryId != -1) inv = Inventory.getInventory(inventoryId);
		if(questId != -1) required = getQuest(questId);
		entity = Entity.getEntity(entityId);
		
		if(id == -1) //-1 for adding
			insert();
		
		quests.add(this);
	}
	
	public int getId(){
		return id;
	}
	
	public String getName(){
		return baseQuest.getName();
	}
	
	public Inventory getInventory(){
		return inv;
	}
	
	public EQuest getPrerequisite(){
		return required;
	}
	
	public Entity getEntity(){
		return entity;
	}
	
	public static EQuest getQuest(int id){
		return quests.stream().filter(q -> q.id == id).findFirst().orElse(null);
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
