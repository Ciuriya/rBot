package me.smc.sb.tourney;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.stream.Collectors;

import me.smc.sb.tourney.Map;
import me.smc.sb.utils.Configuration;
import me.smc.sb.utils.Utils;

public class MapPool{

	private int poolNum; // tournament number, to order
	private String sheetUrl;
	private Tournament tournament;
	private LinkedList<Map> maps;
	public static List<MapPool> pools = new ArrayList<>();
	
	public MapPool(Tournament t){
		this(t, t.incrementPoolCount(), true);
	}
	
	public MapPool(Tournament t, int poolNum, boolean append){
		this.tournament = t;
		this.poolNum = poolNum;
		this.sheetUrl = "";
		maps = new LinkedList<>();
		
		save(append);
		
		pools.add(this);
	}
	
	public int getPoolNum(){
		return poolNum;
	}
	
	public Tournament getTournament(){
		return tournament;
	}
	
	public List<Map> getMaps(){
		return maps;
	}
	
	public int getMapId(Map map){
		int id = 1;
		
		for(Map m : maps){
			if(m.equals(map)) return id;
			
			id++;
		}
		
		return 0;
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
	
	public Map findMap(String url){
		for(Map map : maps)
			if(map.getURL().equalsIgnoreCase(url))
				return map;
		
		return null;
	}
	
	public static MapPool getPool(Tournament t, int poolNum){
		if(!pools.isEmpty())
			for(MapPool pool : pools)
				if(pool.getPoolNum() == poolNum && pool.getTournament().get("name").equalsIgnoreCase(t.get("name")))
					return pool;
		
		return null;
	}
	
	public static List<MapPool> getPools(Tournament t){
		return pools.stream().filter(p -> p.getTournament().get("name").equalsIgnoreCase(t.get("name"))).collect(Collectors.toList());
	}
	
	public static void removePool(Tournament t, int poolNum){
		MapPool pool = getPool(t, poolNum);
		
		pools.remove(pool);
		pool.delete();
	}
	
	public void save(boolean append){
		Configuration config = tournament.getConfig();
		
		if(append) tournament.appendToStringList("pools", "" + poolNum);
		
		if(!maps.isEmpty()){
			ArrayList<String> exportedMaps = new ArrayList<>();
			
			for(Map map : maps)
				exportedMaps.add(map.export());
			
			config.writeStringList("pool-" + poolNum + "-maps", exportedMaps, false);
		}
		
		if(sheetUrl != "") config.writeValue("pool-" + poolNum + "-sheet", sheetUrl);
	}
	
	public void delete(){
		Configuration config = tournament.getConfig();
		
		tournament.removeFromStringList("pools", "" + poolNum);
		config.deleteKey("pool-" + poolNum + "-maps");
		config.deleteKey("pool-" + poolNum + "-sheet");
	}
	
	public static void loadPools(Tournament t){
		ArrayList<String> pools = t.getStringList("pools");
		
		if(!pools.isEmpty()){
			Configuration config = t.getConfig();
			
			for(String poolNum : pools){
				MapPool pool = new MapPool(t, Utils.stringToInt(poolNum), false);
				
				List<String> maps = config.getStringList("pool-" + poolNum + "-maps");
				
				if(maps.size() != 0)
					for(String map : maps)
						pool.addMap(new Map(map, pool));
				
				if(config.getValue("pool-" + poolNum + "-sheet") != "") 
					pool.setSheetUrl(config.getValue("pool-" + poolNum + "-sheet"));
				
				new Timer().schedule(new TimerTask(){
					public void run(){
						pool.save(false); // for bloodcat link loading
					}
				}, 45000);
			}
		}
	}
}
