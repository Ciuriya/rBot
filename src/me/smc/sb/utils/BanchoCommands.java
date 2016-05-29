package me.smc.sb.utils;

public enum BanchoCommands{

	MP,
	REPORT,
	ROLL,
	FAQ,
	REQUEST,
	STATS,
	WHERE;
	
	public static boolean isBanchoCommand(String name){
		for(BanchoCommands cmd : BanchoCommands.values())
			if(name.equalsIgnoreCase(cmd.name()))
				return true;
		
		return false;
	}
	
}
