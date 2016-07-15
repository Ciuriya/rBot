package me.smc.sb.drpg;

import java.util.ArrayList;
import java.util.List;

public abstract class Race{
	
	protected String name;
	protected float strBonus;
	protected float endBonus;
	protected float dexBonus;
	protected float intelBonus;
	protected float wisBonus;
	protected float chaBonus;
	public static List<Race> registered = new ArrayList<>();
	
	public Race(String name){
		this.name = name;
		
		registered.add(this);
	}
	
	public String getName(){
		return name;
	}
	
	public float getStrBonus(){
		return strBonus;
	}
	
	public float getEndBonus(){
		return endBonus;
	}
	
	public float getDexBonus(){
		return dexBonus;
	}
	
	public float getIntBonus(){
		return intelBonus;
	}
	
	public float getWisBonus(){
		return wisBonus;
	}
	
	public float getChaBonus(){
		return chaBonus;
	}
	
	public static Race getRace(String name){
		return registered.stream().filter(r -> r.name.equalsIgnoreCase(name)).findFirst().orElse(null);
	}
	
}