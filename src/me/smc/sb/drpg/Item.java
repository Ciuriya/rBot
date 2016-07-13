package me.smc.sb.drpg;

import java.util.ArrayList;
import java.util.List;

import me.smc.sb.utils.Utils;

public class Item{

	private int id;
	private String name;
	private float value;
	private float weight;
	private float amount;
	private int rarity;
	private boolean sellable;
	private boolean buyable;
	private float effectiveRange;
	private List<EEffect> effects;
	private float lowerIVbound;
	private float upperIVbound;
	private boolean equipped;
	private int equipSlot;
	private float dropChance;
	private Inventory inv;
	public static List<Item> items = new ArrayList<>();
	
	public Item(int id){
		this.id = id;
		
		load();
	}
	
	public Item(int id, String name, float value, float weight, float amount, int rarity,
				boolean sellable, boolean buyable, float range, String impactValues, 
				boolean equipped, int equipSlot, float dropChance, int inventoryId){
		this.id = id;
		this.name = name;
		this.value = value;
		this.weight = weight;
		this.amount = amount;
		this.rarity = rarity;
		this.sellable = sellable;
		this.buyable = buyable;
		
		if(range != -1) effectiveRange = range;
		else range = 0;
		
		if(impactValues.length() > 0){
			lowerIVbound = (float) Utils.stringToDouble(impactValues.split(" ")[0]);
			upperIVbound = (float) Utils.stringToDouble(impactValues.split(" ")[1]);
		}else{
			lowerIVbound = 0;
			upperIVbound = 0;
		}
		
		this.equipped = equipped;
		this.equipSlot = equipSlot;
		this.dropChance = dropChance;
		inv = Inventory.getInventory(inventoryId);
		
		effects = new ArrayList<>();
		EEffect.effects.stream().filter(e -> e.getItem() != null && e.getItem().id == id).forEach(e -> effects.add(e));
		
		if(id == -1) //-1 for adding
			insert();
		
		items.add(this);
	}
	
	public int getId(){
		return id;
	}
	
	public String getName(){
		return name;
	}
	
	public float getValue(){
		return value;
	}
	
	public float getWeight(){
		return weight;
	}
	
	public float getAmount(){
		return amount;
	}
	
	public int getRarity(){
		return rarity;
	}
	
	public boolean isSellable(){
		return sellable;
	}
	
	public boolean isBuyable(){
		return buyable;
	}
	
	public float getRange(){
		return effectiveRange;
	}
	
	public List<EEffect> getEffects(){
		return effects;
	}
	
	public float rollImpactValue(){
		return Utils.fetchRandom(lowerIVbound, upperIVbound);
	}
	
	public boolean isEquipped(){
		return equipped;
	}
	
	public int getEquipSlot(){
		return equipSlot;
	}
	
	public float getDropChance(){
		return dropChance;
	}
	
	public Inventory getInventory(){
		return inv;
	}
	
	public static Item getItem(int id){
		return items.stream().filter(i -> i.id == id).findFirst().orElse(null);
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
