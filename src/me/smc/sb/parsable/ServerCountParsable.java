package me.smc.sb.parsable;

import me.smc.sb.main.Main;

public class ServerCountParsable extends ParsableValue{

	public ServerCountParsable(){
		super("{server-count}");
	}

	@Override
	public String parse(String text) {
		return text.replaceAll(value.replace("{", "\\{"), Main.api.getGuilds().size() + "");
	}
	
}
