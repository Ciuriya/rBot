package me.smc.sb.drpg;

import java.util.ArrayList;
import java.util.List;

import javafx.util.Pair;

public class Battle{

	private int id;
	private Entity attacker;
	private Entity attacked;
	private Party attackingParty;
	private Party attackedParty;
	private List<Integer> entitiesFighting;
	public static List<Battle> battles = new ArrayList<>();
	
	public Battle(int id){
		this.id = id;
		
		load();
	}
	
	//ONLY TWO SIDES CAN FIGHT
	//make sure you only use two of the ids (apart from the battle id), the others should be -1
	public Battle(int id, int attackerId, int attackedId, int attackingPartyId, int attackedPartyId){
		this.id = id;
		if(attackerId != -1) this.attacker = Entity.getEntity(attackerId);
		if(attackedId != -1) this.attacked = Entity.getEntity(attackedId);
		if(attackingPartyId != -1) this.attackingParty = Party.getParty(attackingPartyId);
		if(attackedPartyId != -1) this.attackedParty = Party.getParty(attackedPartyId);
		
		entitiesFighting = new ArrayList<>();
		Pair<Object, Object> sides = getValidSides();
		
		addToFightingEntities(sides.getKey());
		addToFightingEntities(sides.getValue());
		
		if(id == -1) //-1 for adding
			insert();
		
		battles.add(this);
	}
	
	public int getId(){
		return id;
	}
	
	public Entity getAttacker(){
		return attacker;
	}
	
	public Entity getAttacked(){
		return attacked;
	}
	
	public Party getAttackingParty(){
		return attackingParty;
	}
	
	public Party getAttackedParty(){
		return attackedParty;
	}
	
	public Pair<Object, Object> getValidSides(){
		Object first = null, second = null;
		int left = 2;
		
		if(attacker != null){
			first = attacker;
			left--;
		}
		
		if(attacked != null){
			if(left == 1) second = attacked;
			else first = attacked;
			
			left--;
		}
		
		if(attackingParty != null && left != 0){
			if(left == 1) second = attackingParty;
			else first = attackingParty;
			
			left--;
		}
		
		if(attackedParty != null && left != 0){
			if(left == 1) second = attackedParty;
			else first = attackedParty;
			
			left--;
		}
		
		return new Pair<>(first, second);
	}
	
	public List<Integer> getFightingEntities(){
		return entitiesFighting;
	}
	
	private void addToFightingEntities(Object side){
		if(side instanceof Entity) entitiesFighting.add(((Entity) side).getId());
		else entitiesFighting.addAll(((Party) side).getMembers());
	}
	
	public static Battle getBattle(int id){
		return battles.stream().filter(b -> b.id == id).findFirst().orElse(null);
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
