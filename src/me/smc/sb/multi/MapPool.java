package me.smc.sb.multi;

import java.util.ArrayList;
import java.util.List;

import me.smc.sb.utils.Configuration;

public class MapPool{

	private int poolNum;
	private Tournament tournament;
	private List<Map> maps;
	
	public MapPool(Tournament t){
		this(t, t.incrementPoolCount(), true);
	}
	
	public MapPool(Tournament t, int poolNum, boolean append){
		this.tournament = t;
		this.poolNum = poolNum;
		maps = new ArrayList<>();
		
		save(append);
		t.addPool(this);
	}
	
	public int getPoolNum(){
		return poolNum;
	}
	
	public void addMap(Map map){
		maps.add(map);
	}
	
	public void removeMap(String url){
		for(Map map : new ArrayList<Map>(maps))
			if(map.getURL().equalsIgnoreCase(url))
				maps.remove(map);
	}
	
	public void delete(){
		Configuration config = tournament.getConfig();
		ArrayList<String> savedPools = config.getStringList("pools");
		
		if(!savedPools.isEmpty()){
			savedPools.remove(String.valueOf(poolNum));
			config.writeStringList("pools", savedPools, true);
		}
		
		config.deleteKey("pool-" + poolNum + "-maps");
	}
	
	public void save(boolean append){
		Configuration config = tournament.getConfig();
		if(append) config.appendToStringList("pools", String.valueOf(poolNum), true);
		
		if(!maps.isEmpty()){
			ArrayList<String> exportedMaps = new ArrayList<>();
			for(Map map : maps)
				exportedMaps.add(map.export());
			
			config.writeStringList("pool-" + poolNum + "-maps", exportedMaps, true);
		}
	}
	
}
