package me.smc.sb.parsable;

public class DiscordLinkParsable extends ParsableValue{

	public DiscordLinkParsable(){
		super("{discord-link}");
	}

	@Override
	public String parse(String text) {
		return text.replaceAll(value.replace("{", "\\{"), "http://discord.gg/0phGqtqLYwSzCdwn");
	}
	
}
