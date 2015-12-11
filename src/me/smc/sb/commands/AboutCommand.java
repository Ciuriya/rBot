package me.smc.sb.commands;

import me.itsghost.jdiscord.events.UserChatEvent;
import me.smc.sb.utils.Utils;

public class AboutCommand extends GlobalCommand{
	
	public AboutCommand(){
		super(null, 
			  " - Learn about this bot!", 
			  "{prefix}about\nThis command sends an informative message about this bot!", 
			  true, 
			  "about");
	}

	@Override
	public void onCommand(UserChatEvent e, String[] args){
		e.getMsg().deleteMessage();
		Utils.info(e.getGroup(), "Hello!\nI am a bot made by Smc.\nI am used to create commands for now, but there are future features planned.");
	}
	
}
