package me.smc.sb.parsable;

import java.util.ArrayList;
import java.util.List;

public abstract class ParsableValue{

	public static List<ParsableValue> registeredParsables = new ArrayList<>();
	
	protected String value;
	
	public ParsableValue(String value){
		this.value = value;
		registeredParsables.add(this);
	}
	
	public String getValue(){
		return value;
	}
	
	public static String parseFullText(String text){
		String updated = text;
		
		for(ParsableValue value : registeredParsables)
			if(updated.contains(value.value))
				updated = value.parse(updated);
		
		return updated;
	}
	
	public abstract String parse(String text);
	
	public static void init(){
		new DiscordLinkParsable();
		new RunningMatchesParsable();
		new ServerCountParsable();
		new TrackedUniqueParsable();
		new TrackRefreshParsable();
		new UptimeParsable();
	}
}
