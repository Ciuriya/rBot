package me.Smc.sb.commands;

import me.Smc.sb.main.Main;
import me.Smc.sb.utils.Configuration;
import me.itsghost.jdiscord.events.UserChatEvent;

public class Silent{

	public static void execute(UserChatEvent e, boolean silent){
		e.getMsg().deleteMessage();
		Configuration cfg = Main.serverConfigs.get(e.getServer().getId());
		cfg.writeValue("silent", silent);
	}
	
}
