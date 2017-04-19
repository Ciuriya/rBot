package me.smc.sb.tracking;

import java.util.ArrayList;
import java.util.List;

public enum Mods{
	
	None(0, "NoMod"), 
	NoFail(1, "NF"), 
	Easy(2, "EZ"), 
	Hidden(8, "HD"), 
	HardRock(16, "HR"), 
	SuddenDeath(32, "SD"), 
	DoubleTime(64, "DT"),
	Relax(128, "RL"), 
	HalfTime(256, "HT"), 
	Nightcore(512, "NC"), 
	Flashlight(1024, "FL"), 
	SpunOut(4096, "SO"), 
	Autopilot(8192, "AP"),
	Perfect(16384, "PF");
	
	int bit;
	String shortName;
	
	Mods(int bit, String shortName){
		this.bit = bit;
		this.shortName = shortName;
	}
	
	public int getBit(){
		return bit;
	}
	
	public String getShortName(){
		return shortName;
	}
	
	public static List<Mods> getMods(int modsUsed){
		List<Mods> mods = new ArrayList<>();
		int used = modsUsed;
		
		if(used == 0) return mods;
		
		for(int i = 16384; i >= 1; i /= 2){
			Mods mod = Mods.getMod(i);
			
			if(used >= i){
				mods.add(mod);
				used -= i;
			}
		}
		
		if(mods.contains(Mods.None)) mods.remove(Mods.None);
		if(mods.contains(Mods.Nightcore)) mods.remove(Mods.DoubleTime);
		if(mods.contains(Mods.Perfect)) mods.remove(Mods.SuddenDeath);
		
		return mods;
	}
	
	public static String getModDisplay(List<Mods> mods){
		String display = "";
		
		for(Mods mod : mods)
			display = mod.getShortName() + display;
		
		return display.length() == 0 ? "" : "+" + display;
	}
	
	public static Mods getMod(int bit){
		for(Mods mod : Mods.values())
			if(mod.getBit() == bit) return mod;
		
		return Mods.None;
	}
}
