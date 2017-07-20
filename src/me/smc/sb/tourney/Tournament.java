package me.smc.sb.tourney;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import me.smc.sb.utils.Configuration;
import me.smc.sb.utils.Utils;

public class Tournament{

	public static List<Tournament> tournaments;
	public Map<String, Match> conditionalTeams = new HashMap<>();
	private Map<String, Object> configValues;
	private TwitchHandler twitchHandler;
	
	public Tournament(String name){
		this(name, true);
	}
	
	public Tournament(String name, boolean append){
		configValues = new HashMap<>();
		configValues.put("name", name);
		
		load();
		
		if(append) save(true);
		
		tournaments.add(this);
		
		Object twitchChannel = configValues.get("twitchChannel");
		
		if(twitchChannel != null && twitchChannel instanceof String)
			twitchHandler = TwitchHandler.add((String) twitchChannel, this);
	}
	
	public String get(String key){
		return (String) configValues.get(key);
	}
	
	public int getInt(String key){
		return (Integer) configValues.get(key);
	}
	
	public double getDouble(String key){
		return (Double) configValues.get(key);
	}
	
	public boolean getBool(String key){
		return (Boolean) configValues.get(key);
	}
	
	public float getFloat(String key){
		return (Float) configValues.get(key);
	}
	
	@SuppressWarnings("unchecked")
	public ArrayList<String> getStringList(String key){
		ArrayList<String> list = (ArrayList<String>) configValues.get(key);
		
		return list == null ? new ArrayList<String>() : list;
	}
	
	public void appendToStringList(String key, String value){
		ArrayList<String> list = getStringList(key);
		
		list.add(value);
		
		set(key, value);
	}
	
	public void removeFromStringList(String key, String value){
		ArrayList<String> list = getStringList(key);
		
		list.remove(value);
		
		set(key, value);
	}
	
	@SuppressWarnings("unchecked")
	public void set(String key, Object value){
		configValues.put(key, value);
		
		Configuration config = getConfig();
		
		if(value instanceof ArrayList)
			config.writeStringList("conf-" + key, (ArrayList<String>) value, true);
		else config.writeValue("conf-" + key, value);
	}
	
	public Configuration getConfig(){
		return new Configuration(new File("tournament-" + get("name") + ".txt"));
	}
	
	protected int incrementPoolCount(){
		int lastPool = getInt("last-pool");
		
		set("last-pool", lastPool + 1);
		
		return lastPool + 1;
	}
	
	protected int incrementMatchCount(){
		int lastMatch = getInt("last-match");
		
		set("last-match", lastMatch + 1);
		
		return lastMatch + 1;
	}
	
	public TwitchHandler getTwitchHandler(){
		return twitchHandler;
	}
	
	public long getTempLobbyDecayTime(){
		Object decayTime = configValues.get("tempLobbyDecayTime");
		
		return decayTime == null ? 0 : (long) decayTime;
	}
	
	public void setTempLobbyDecayTime(){
		configValues.put("tempLobbyDecayTime", System.currentTimeMillis() + 300000l); // 5 minutes later
	}
	
	public static Tournament getTournament(String name){
		return getTournament(name, 0);
	}
	
	public static Tournament getTournament(String name, int next){
		int nextSelected = next;
		
		if(!tournaments.isEmpty())
			for(Tournament t : tournaments)
				if(t.get("name").equalsIgnoreCase(name)){
					if(nextSelected > 0){
						nextSelected--;
						continue;
					}
					
					return t;
				}else if(t.get("displayName").equalsIgnoreCase(name)){
					if(nextSelected > 0){
						nextSelected--;
						continue;
					}
					
					return t;
				}
		
		return null;
	}
	
	public void load(){
		Configuration config = getConfig();
		
		for(String line : config.getLines()){
			if(line.startsWith("conf-")){
				String key = line.substring(0, line.indexOf(":")).replace("conf-", "");
				
				if(line.endsWith(":list"))
					configValues.put(key, "conf-" + config.getStringList(key));
				else{
					String value = config.getValue(key);
					
					if(Utils.stringToInt(value) != -1)
						configValues.put(key, Utils.stringToInt(value));
					else configValues.put(key, value);
				}
			}
		}
	}
	
	@SuppressWarnings("unchecked")
	public void save(boolean append){
		if(append){
			setDefaults();
			new Configuration(new File("tournaments.txt")).appendToStringList("tournaments", get("name"), true);
		}
		
		Configuration config = getConfig();
		
		for(String key : configValues.keySet()){
			Object value = configValues.get(key);
			
			if(value instanceof ArrayList)
				config.writeStringList("conf-" + key, (ArrayList<String>) value, true);
			else config.writeValue("conf-" + key, value);
		}
	}
	
	public void delete(){
		Configuration config = new Configuration(new File("tournaments.txt"));
		ArrayList<String> savedTournaments = config.getStringList("tournaments");
		
		if(!savedTournaments.isEmpty()){
			savedTournaments.remove(get("name"));
			config.writeStringList("tournaments", savedTournaments, true);
		}
		
		getConfig().delete();
	}
	
	public void setDefaults(){
		configValues.put("displayName", get("name"));
		configValues.put("twitchChannel", "");
		configValues.put("scoreV2", true);
		configValues.put("pickWaitTime", 180);
		configValues.put("banWaitTime", 120);
		configValues.put("readyWaitTime", 120);
		configValues.put("type", 0);
		configValues.put("mode", 0);
		configValues.put("resultDiscord", "");
		configValues.put("alertDiscord", "");
		configValues.put("alertMessage", "");
		configValues.put("lowerRankBound", 0);
		configValues.put("upperRankBound", 0);
		configValues.put("skipWarmups", true);
		configValues.put("pickStrategy", "regular");
		configValues.put("rematchesAllowed", 1);
		configValues.put("usingTourneyServer", true);
		configValues.put("usingConfirms", true);
		configValues.put("usingMapStats", true);
		configValues.put("targetRankLowerBound", 0);
		configValues.put("targetRankUpperBound", 0);
		configValues.put("matchSize", 6);
		configValues.put("usingDQs", false);
		configValues.put("dqTime", 150);
		configValues.put("usingBans", true);
		configValues.put("banCount", 1);
		configValues.put("gracePeriodTime", 600);
		
		ArrayList<String> freemodMods = new ArrayList<>();
		freemodMods.add("HD");
		freemodMods.add("HR");
		freemodMods.add("FL");
		freemodMods.add("EZ");
		
		configValues.put("allowedFreemodMods", freemodMods);
		configValues.put("warmupLength", 270);
		configValues.put("warmupCount", 1);
		configValues.put("rematchCutoff", 30);
		configValues.put("tournament-admins", new ArrayList<>());
	}
	
	public static void loadTournaments(){
		tournaments = new ArrayList<>();
		List<String> savedTournaments = new Configuration(new File("tournaments.txt")).getStringList("tournaments");

		if(!savedTournaments.isEmpty())
			for(String sTournament : savedTournaments){
				Tournament tournament = new Tournament(sTournament, false);

				MapPool.loadPools(tournament);
				Team.loadTeams(tournament);
				Match.loadMatches(tournament);
			}	
	}
}
