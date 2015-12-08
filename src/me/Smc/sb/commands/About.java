package me.Smc.sb.commands;

import me.Smc.sb.utils.Utils;
import me.itsghost.jdiscord.events.UserChatEvent;

public class About{

	public static void execute(UserChatEvent e, boolean dm){
		e.getMsg().deleteMessage();
		if(!dm) Utils.info(e.getGroup(), e.getUser().getUser(), " Hello!\nI am a bot made by Smc.\nI am used to create commands for now, but there are future features planned.");
		else Utils.info(e.getGroup(), "Hello!\nI am a bot made by Smc.\nI am used to create commands for now, but there are future features planned.");
	}
	
}
