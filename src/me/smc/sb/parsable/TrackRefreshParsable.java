package me.smc.sb.parsable;

import me.smc.sb.discordcommands.OsuTrackCommand;
import me.smc.sb.utils.Utils;

public class TrackRefreshParsable extends ParsableValue{
	
	public TrackRefreshParsable(){
		super("{track-refresh}");
	}

	@Override
	public String parse(String text){
		return text.replaceAll(value.replace("{", "\\{"), Utils.toDuration((long) OsuTrackCommand.currentRefreshRate * 1000));
	}
}
