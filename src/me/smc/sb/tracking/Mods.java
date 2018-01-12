package me.smc.sb.tracking;

import java.util.ArrayList;
import java.util.List;

public enum Mods{
	
	None(0, "NM"), 
	NoFail(1, "NF"), 
	Easy(2, "EZ"),
	NoVideo(4, "NV"),
	Hidden(8, "HD"), 
	HardRock(16, "HR"), 
	SuddenDeath(32, "SD"), 
	DoubleTime(64, "DT"),
	Relax(128, "RL"), 
	HalfTime(256, "HT"), 
	Nightcore(512, "NC"), 
	Flashlight(1024, "FL"), 
	Autoplay(2048, "AU"),
	SpunOut(4096, "SO"), 
	Autopilot(8192, "AP"),
	Perfect(16384, "PF"),
	Key4(32768, "4K"),
	Key5(65536, "5K"),
	Key6(131072, "6K"),
	Key7(262144, "7K"),
	Key8(524288, "8K"),
	FadeIn(1048576, "FI"),
	Random(2097152, "RA"),
	LastMod(4194304, "LM"),
	Key9(16777216, "9K"),
	Key10(33554432, "10K"),
	Key1(67108864, "1K"),
	Key3(134217728, "3K"),
	Key2(268435456, "2K");
	
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

	public static int getMods(String sMods){
		int bits = 0;
		
		for(String sMod : sMods.split(","))
			for(Mods mod : Mods.values())
				if(mod.getShortName().equalsIgnoreCase(sMod) ||
					mod.name().equalsIgnoreCase(sMod))
					bits += mod.getBit();
			
		
		return bits;
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
	
	public static int getMods(List<Mods> mods){
		int modsUsed = 0;
		
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
	
	public static Mods getMod(int bit){
		for(Mods mod : Mods.values())
			if(mod.getBit() == bit) return mod;
		
		return Mods.None;
	}
}
