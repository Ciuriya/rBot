package me.smc.sb.parsable;

import me.smc.sb.tracking.TrackedPlayer;

public class TrackedUniqueParsable extends ParsableValue{
	
	public TrackedUniqueParsable(){
		super("{tracked-unique}");
	}

	@Override
	public String parse(String text){
		return text.replaceAll(value.replace("{", "\\{"), TrackedPlayer.registeredPlayers.size() + "");
	}
}
