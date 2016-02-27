package me.smc.sb.multi;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import me.smc.sb.utils.Configuration;

public class MapPool{

	private int poolNum;
	private String sheetUrl;
	private Tournament tournament;
	private LinkedList<Map> maps;
	
	public MapPool(Tournament t){
		this(t, t.incrementPoolCount(), true);
	}
	
	public MapPool(Tournament t, int poolNum, boolean append){
		this.tournament = t;
		this.poolNum = poolNum;
		this.sheetUrl = "";
		maps = new LinkedList<>();
		
		save(append);
		t.addPool(this);
	}
	
	public int getPoolNum(){
		return poolNum;
	}
	
	public List<Map> getMaps(){
		return maps;
	}
	
	public String getSheetUrl(){
		return sheetUrl;
	}
	
	public void setSheetUrl(String sheetUrl){
		this.sheetUrl = sheetUrl;
	}
	
	public void addMap(Map map){
		if(!mapExists(map)) maps.add(map);
	}
	
	public boolean mapExists(Map map){
		int bId = map.getBeatmapID();
		for(Map m : maps)
			if(m.getBeatmapID() == bId)
				return true;
		return false;
	}
	
	public Map findTiebreaker(){
		for(Map map : maps)
			if(map.getCategory() == 5)
				return map;
		return maps.getLast();
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
		config.deleteKey("pool-" + poolNum + "-sheet");
	}
	
	public void save(boolean append){
		Configuration config = tournament.getConfig();
		if(append) config.appendToStringList("pools", String.valueOf(poolNum), true);
		
		if(!maps.isEmpty()){
			ArrayList<String> exportedMaps = new ArrayList<>();
			for(Map map : maps)
				exportedMaps.add(map.export());
			
			config.writeStringList("pool-" + poolNum + "-maps", exportedMaps, false);
		}
		
		config.writeValue("pool-" + poolNum + "-sheet", sheetUrl);
	}
	
}
