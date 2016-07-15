package me.smc.sb.drpg;

import java.util.ArrayList;
import java.util.List;

public abstract class Specialization{

	protected String name;
	protected Class parentClass;
	protected Specialization parentSpec;
	public static List<Specialization> registered = new ArrayList<>();
	
	public Specialization(String name, String parentClass, String parentSpec){
		this.name = name;
		this.parentClass = Class.getClass(parentClass);
		if(parentSpec.length() > 0) this.parentSpec = getSpecialization(parentSpec);
		
		this.parentClass.addSpecialization(this);
		
		registered.add(this);
	}
	
	public String getName(){
		return name;
	}
	
	public Class getParentClass(){
		return parentClass;
	}
	
	public Specialization getParentSpec(){
		return parentSpec;
	}
	
	//0 = none
	public int getAmountOfRequiredSpecs(){
		return parentSpec == null ? 0 : parentSpec.getAmountOfRequiredSpecs() + 1;
	}
	
	public static Specialization getSpecialization(String name){
		return registered.stream().filter(s -> s.name.equalsIgnoreCase(name)).findFirst().orElse(null);
	}
	
}
