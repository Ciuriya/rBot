package me.smc.sb.drpg;

import java.util.ArrayList;
import java.util.List;

public class EEffect{

	private int id;
	private Effect baseEffect;
	private float power;
	private int turnsLeft;
	private Entity entity;
	private Item item;
	public static List<EEffect> effects = new ArrayList<>();
	
	public EEffect(int id){
		this.id = id;
		
		load();
	}
	
	//you can only have either the entity ID or the item ID, put the one you don't use at -1
	//item ID is to identify that an item has this effect
	//entity ID is to identify that an entity has an effect on him
	public EEffect(int id, String name, float power, int turns, int entityId, int itemId){
		this.id = id;
		baseEffect = Effect.getEffect(name);
		this.power = power;
		this.turnsLeft = turns;
		if(entityId != -1) entity = Entity.getEntity(entityId);
		else if(itemId != -1) item = Item.getItem(itemId);
		
		if(id == -1) //-1 for adding
			insert();
		
		effects.add(this);
	}
	
	public int getId(){
		return id;
	}
	
	public String getName(){
		return baseEffect.getName();
	}
	
	public Effect getBaseEffect(){
		return baseEffect;
	}
	
	public float getPower(){
		return power;
	}
	
	public int getTurnsLeft(){
		return turnsLeft;
	}
	
	public Entity getEntity(){
		return entity;
	}
	
	public Item getItem(){
		return item;
	}
	
	public static EEffect getEffect(int id){
		return effects.stream().filter(s -> s.id == id).findFirst().orElse(null);
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
