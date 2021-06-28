package me.smc.sb.parsable;

public class DiscordLinkParsable extends ParsableValue{

	public DiscordLinkParsable(){
		super("{discord-link}");
	}

	@Override
	public String parse(String text) {
		return text.replaceAll(value.replace("{", "\\{"), "https://discord.gg/V4Z4VqV2Yw");
	}
	
}
