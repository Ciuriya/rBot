package me.Smc.sb.commands;

import me.itsghost.jdiscord.events.UserChatEvent;

public class Suggestions{

	//new and old and clear suggestions in code block
	//pages?
	
	public static void execute(UserChatEvent e, String msg, boolean dm){
		if(!dm) return;
		e.getMsg().deleteMessage();
		String uID = e.getUser().getUser().getId();
		if(!uID.equalsIgnoreCase("91302128328392704")) return;
		//Configuration cfg = new Configuration(new File("suggestions.txt"));
		/*List<String> suggestions = new ArrayList<String>();
		switch(msg.split(" ")[0].toLowerCase()){
			case "old": suggestions = cfg.getStringList("old-suggestions"); break;
			default: suggestions = cfg.getStringList("suggestions"); break;
		}*/
		
	}
	
}
