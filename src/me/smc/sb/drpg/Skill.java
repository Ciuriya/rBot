package me.smc.sb.drpg;

import java.util.ArrayList;
import java.util.List;

public abstract class Skill{

	protected String name;
	protected boolean battleOnly;
	public static List<Skill> registered = new ArrayList<>();
	
	public Skill(String name, boolean battleOnly){
		this.name = name;
		this.battleOnly = battleOnly;
		
		registered.add(this);
	}
	
	public String getName(){
		return name;
	}
	
	public boolean isBattleOnly(){
		return battleOnly;
	}
	
	public static Skill getSkill(String name){
		return registered.stream().filter(s -> s.name.equalsIgnoreCase(name)).findFirst().orElse(null);
	}
	
	//if your skill is passive, don't put anything in activeUse and vice-versa
	//this is how it works
	
	//activated by player
	public abstract void activeUse();
	
	//every action (turn change, tile movement, day change)
	public abstract void passiveUse();
}
