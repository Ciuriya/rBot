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
	private Map<String, Object> configValues;
	
	public Tournament(String name){
		this(name, true);
	}
	
	public Tournament(String name, boolean append){
		configValues = new HashMap<>();
		configValues.put("name", name);
		
		load();
		
		if(append) save(true);
		
		tournaments.add(this);
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
		return (ArrayList<String>) configValues.get(key);
	}
	
	public void set(String key, Object value){
		configValues.put(key, value);
	}
	
	public Configuration getConfig(){
		return new Configuration(new File("tournament-" + get("name") + ".txt"));
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
		if(append) new Configuration(new File("tournaments.txt")).appendToStringList("tournaments", get("name"), true);
		
		Configuration config = getConfig();
		
		for(String key : configValues.keySet()){
			Object value = configValues.get(key);
			
			if(value instanceof ArrayList)
				config.writeStringList("conf-" + key, (ArrayList<String>) value, true);
			else config.writeValue("conf-" + key, value);
		}
	}
	
	public static void loadTournaments(){
		tournaments = new ArrayList<>();
		List<String> savedTournaments = new Configuration(new File("tournaments.txt")).getStringList("tournaments");

		if(!savedTournaments.isEmpty())
			for(String sTournament : savedTournaments){
				Tournament tournament = new Tournament(sTournament, false);

				/*tournament.loadPools();
				tournament.loadTeams();
				tournament.loadMatches();
				
				if(!tournament.getConfig().getStringList("tournament-admins").isEmpty()){
					tournament.setMatchAdmins(tournament.getConfig().getStringList("tournament-admins"));
				}*/
			}	
	}
}
