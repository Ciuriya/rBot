package me.smc.sb.utils;

import me.smc.sb.parsable.ParsableValue;

public enum DiscordStatuses{

	UPTIME("for {uptime}"),
	SERVERS("in {server-count} servers"),
	TRACKED("with {tracked-unique} osu! players"),
	REFRESH("osu!track every {track-refresh}"),
	TOURNEY_MATCHES("in {running-matches} osu! matches", "{running-matches}", 1),
	DISCORD_INVITE("in {discord-link}"),
	HELP("use ~/help for info!");
	
	String text;
	ParsableValue minParsable;
	int min;
	
	DiscordStatuses(String text){
		this.text = text;
	}
	
	DiscordStatuses(String text, String minParsable, int min){
		this.text = text;
		this.minParsable = ParsableValue.registeredParsables.stream().filter(pv -> pv.getValue().equals(minParsable)).findFirst().orElse(null);
		this.min = min;
	}
	
	public boolean canParse(){
		if(minParsable != null)
			return Utils.stringToInt(minParsable.parse(minParsable.getValue())) >= min;
			
		return true;
	}
	
	public String getStatus(){
		return ParsableValue.parseFullText(text);
	}
}
