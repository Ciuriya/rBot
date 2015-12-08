package me.Smc.sb.commands;

import me.Smc.sb.main.Main;
import me.Smc.sb.perm.Permissions;
import me.Smc.sb.utils.Utils;
import me.itsghost.jdiscord.events.UserChatEvent;

public class Stop{

	public static void execute(UserChatEvent e, boolean dm){
		e.getMsg().deleteMessage();
		if(!Permissions.hasPerm(e.getUser(), Permissions.BOT_ADMIN)) return;
		String msg = e.getMsg().getMessage();
		int retCode = 1;
		if(msg.split(" ").length >= 2) retCode = Utils.stringToInt(msg.split(" ")[1]);
		if(!dm) Utils.info(e.getGroup(), e.getUser().getUser(), " has" + getMessageBasedOnCode(retCode));
		else Utils.info(e.getGroup(), "You have" + getMessageBasedOnCode(retCode));
		Main.stop(retCode);
	}
	
	private static String getMessageBasedOnCode(int retCode){
		switch(retCode){
			case 2: return " requested a restart!";
			case 3: return " requested a bot update!";
			default: return " requested a shutdown!";
		}
	}
	
}
