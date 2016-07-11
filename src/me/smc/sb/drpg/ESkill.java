package me.smc.sb.drpg;

import java.util.ArrayList;
import java.util.List;

public class ESkill{

	private int id;
	private Skill baseSkill;
	private float exp;
	private Entity entity;
	public static List<ESkill> skills = new ArrayList<>();
	
	public ESkill(int id){
		this.id = id;
		
		load();
	}
	
	public ESkill(int id, String name, float exp, int entityId){
		this.id = id;
		baseSkill = Skill.getSkill(name);
		this.exp = exp;
		this.entity = Entity.getEntity(entityId);
		
		if(id == -1) //-1 for adding
			insert();
		
		skills.add(this);
	}
	
	public int getId(){
		return id;
	}
	
	public String getName(){
		return baseSkill.getName();
	}
	
	public Skill getBaseSkill(){
		return baseSkill;
	}
	
	public float getExp(){
		return exp;
	}
	
	public Entity getEntity(){
		return entity;
	}
	
	public static ESkill getSkill(int id){
		return skills.stream().filter(s -> s.id == id).findFirst().orElse(null);
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
