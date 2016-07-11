package me.smc.sb.multi;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;

import com.jcabi.jdbc.JdbcSession;
import com.jcabi.jdbc.Outcome;

import me.smc.sb.main.Main;
import me.smc.sb.utils.Configuration;
import me.smc.sb.utils.Log;

public class MapPool{

	private int poolNum; //tournament number, to order
	private int poolId;
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
		//saveSQL(true);
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
	
	public Map findMap(String url){
		for(Map map : maps)
			if(map.getURL().equalsIgnoreCase(url))
				return map;
		return null;
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
		
		if(sheetUrl != "") config.writeValue("pool-" + poolNum + "-sheet", sheetUrl);
	}
	
	public void saveSQL(boolean add){
		try{
			if(add){
				new JdbcSession(Main.tourneySQL)
				.sql("INSERT INTO MapPool (sheet_url, id_tournament)" +
				     "VALUES (?, ?)")
				.set(sheetUrl)
				.set(tournament.getTournamentId())
				.insert(Outcome.VOID);
			}else{
				//new JdbcSession(Main.sqlConnection)
				//.sql("UPDATE MapPool " +
				//	 "SET sheet_url='?' " +
				//	 "WHERE name='?'")
				//.set(scoreV2 ? 1 : 0)
				//.set(pickWaitTime)
				//.set(name)
				//.update(Outcome.VOID);
			}
		}catch(Exception e){
			Log.logger.log(Level.SEVERE, e.getMessage(), e);
		}
	}
	
}
