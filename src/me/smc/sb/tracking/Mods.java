package me.smc.sb.tracking;

import java.util.ArrayList;
import java.util.List;

public enum Mods{
	
	None(0, "NM", true), 
	NoFail(1, "NF", false), 
	Easy(2, "EZ", true),
	TouchDevice(4, "TD", false),
	Hidden(8, "HD", true), 
	HardRock(16, "HR", true), 
	SuddenDeath(32, "SD", true), 
	DoubleTime(64, "DT", true),
	Relax(128, "RL", true), 
	HalfTime(256, "HT", true), 
	Nightcore(512, "NC", true), 
	Flashlight(1024, "FL", true), 
	Autoplay(2048, "AU", true),
	SpunOut(4096, "SO", true), 
	Autopilot(8192, "AP", true),
	Perfect(16384, "PF", false),
	Key4(32768, "4K", true),
	Key5(65536, "5K", true),
	Key6(131072, "6K", true),
	Key7(262144, "7K", true),
	Key8(524288, "8K", true),
	FadeIn(1048576, "FI", true),
	Random(2097152, "RA", true),
	Cinema(4194304, "CN", true),
	Target(8388608, "TP", true),
	Key9(16777216, "9K", true),
	Key10(33554432, "10K", true),
	Key1(67108864, "1K", true),
	Key3(134217728, "3K", true),
	Key2(268435456, "2K", true),
	ScoreV2(536870912, "V2", true),
	Mirror(1073741824, "MR", true);
	
	long bit;
	String shortName;
	boolean affectsGameplay;
	
	Mods(int bit, String shortName, boolean affectsGameplay){
		this.bit = bit;
		this.shortName = shortName;
		this.affectsGameplay = affectsGameplay;
	}
	
	public long getBit(){
		return bit;
	}
	
	public String getShortName(){
		return shortName;
	}
	
	public boolean affectsGameplay(){
		return affectsGameplay;
	}

	public static long getMods(String sMods){
		long bits = 0;
		
		for(String sMod : sMods.split(","))
			for(Mods mod : Mods.values())
				if(mod.getShortName().equalsIgnoreCase(sMod) ||
					mod.name().equalsIgnoreCase(sMod))
					bits += mod.getBit();
			
		
		return bits;
	}
	
	public static List<Mods> getMods(int modsUsed){
		List<Mods> mods = new ArrayList<>();
		long used = modsUsed;
		
		if(used == 0) return mods;
		
		for(long i = 1073741824; i >= 1; i /= 2){
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
	
	public static List<Mods> getGameplayAffectingMods(int modsUsed){
		List<Mods> mods = getMods(modsUsed);
		List<Mods> gameplayAffectingMods = new ArrayList<>();
		
		for(int i = 0; i < mods.size(); i++){
			Mods mod = mods.get(i);
			
			if(mod.affectsGameplay())
				gameplayAffectingMods.add(mod);
		}
		
		return gameplayAffectingMods;
	}
	
	public static long getMods(List<Mods> mods){
		long modsUsed = 0;
		
		for(Mods mod : mods)
			modsUsed += mod.getBit();
		
		return modsUsed;
	}
	
	public static String getModDisplay(List<Mods> mods){
		String display = "";
		
		for(Mods mod : mods)
			display = mod.getShortName() + display;
		
		return display.length() == 0 ? "" : "+" + display;
	}
	
	public static Mods getMod(long bit){
		for(Mods mod : Mods.values())
			if(mod.getBit() == bit) return mod;
		
		return Mods.None;
	}
}