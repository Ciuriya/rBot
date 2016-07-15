package me.smc.sb.drpg;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public abstract class Class{

	protected String name;
	protected List<Specialization> specs;
	public static List<Class> registered = new ArrayList<>();
	
	public Class(String name){
		this.name = name;
		
		specs = new ArrayList<>();
		
		registered.add(this);
	}
	
	public String getName(){
		return name;
	}
	
	protected void addSpecialization(Specialization spec){
		specs.add(spec);
	}
	
	public List<Specialization> getSpecializations(){
		return specs;
	}
	
	public List<Specialization> findNextSetOfSpecs(Specialization spec){
		return specs.stream().filter(s -> s.parentSpec.equals(spec)).collect(Collectors.toList());
	}
	
	public static Class getClass(String name){
		return registered.stream().filter(c -> c.name.equalsIgnoreCase(name)).findFirst().orElse(null);
	}
	
}