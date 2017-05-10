package me.smc.sb.parsable;

import me.smc.sb.main.Main;
import me.smc.sb.utils.Utils;

public class UptimeParsable extends ParsableValue{
	
	public UptimeParsable(){
		super("{uptime}");
	}

	@Override
	public String parse(String text){
		return text.replaceAll(value.replace("{", "\\{"), Utils.toDuration(System.currentTimeMillis() - Main.bootTime));
	}
}
