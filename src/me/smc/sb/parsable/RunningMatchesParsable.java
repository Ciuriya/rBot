package me.smc.sb.parsable;

import me.smc.sb.tourney.Match;

public class RunningMatchesParsable extends ParsableValue{

	public RunningMatchesParsable(){
		super("{running-matches}");
	}

	@Override
	public String parse(String text) {
		return text.replaceAll(value.replace("{", "\\{"), Match.runningMatches + "");
	}
	
}
