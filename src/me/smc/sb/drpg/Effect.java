package me.smc.sb.drpg;

import java.util.ArrayList;
import java.util.List;

public abstract class Effect{

	protected String name;
	protected EffectType type;
	public static List<Effect> registered = new ArrayList<>();
	
	public Effect(String name, String type){
		this.name = name;
		this.type = EffectType.valueOf(type.toUpperCase());
		
		registered.add(this);
	}
	
	public String getName(){
		return name;
	}
	
	public EffectType getType(){
		return type;
	}
	
	public static Effect getEffect(String name){
		return registered.stream().filter(s -> s.name.equalsIgnoreCase(name)).findFirst().orElse(null);
	}
	
	//Gonna fill in eventually
	public abstract void run();
	
}
