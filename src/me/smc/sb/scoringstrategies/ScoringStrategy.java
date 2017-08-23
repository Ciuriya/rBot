package me.smc.sb.scoringstrategies;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

import me.smc.sb.tourney.BanchoHandler;
import me.smc.sb.tracking.TrackedPlay;

public interface ScoringStrategy{
	
	public static ScoringStrategy findStrategy(String name){
		switch(name.toLowerCase()){
			case "smtscore": return new SMTScoringStrategy();
			case "default": case "regular":
			default: return new DefaultScoringStrategy();
		}
	}
	
	public static String getStrategyName(ScoringStrategy strategy){
		switch(strategy.getClass().getSimpleName()){
			case "SMTScoringStrategy": return "smtscore";
			case "DefaultScoringStrategy":
			default: return "default";
		}
	}
	
	public long calculateScore(String player, TrackedPlay play, boolean scorev2, BanchoHandler handle);
	
	default URLConnection establishConnection(String url){
		URLConnection connection = null;
		
		try{
			connection = new URL(url).openConnection();
		}catch(Exception e){
			e.printStackTrace();
		}
		
        connection.setDoInput(true);
        connection.setRequestProperty("User-Agent", "Mozilla/5.0");
        
        return connection;
	}
	
	default List<String> fetchOsuFile(int beatmapId){
		List<String> lines = new ArrayList<>();
		URLConnection connection = establishConnection("https://osu.ppy.sh/osu/" + beatmapId);
		
        try{
			InputStream in = connection.getInputStream();
			BufferedReader inputStream = new BufferedReader(new InputStreamReader(in));
			String line = null;
			
			while((line = inputStream.readLine()) != null)
				lines.add(line);
			
			inputStream.close();
        }catch(Exception ex){
        	ex.printStackTrace();
        }
        
        return lines;
	}
}
