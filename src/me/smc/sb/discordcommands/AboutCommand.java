package me.smc.sb.discordcommands;

import me.smc.sb.utils.Utils;
import net.dv8tion.jda.events.message.MessageReceivedEvent;

public class AboutCommand extends GlobalCommand{
	
	public AboutCommand(){
		super(null, 
			  " - Learn about this bot!", 
			  "{prefix}about\nThis command sends an informative message about this bot!", 
			  true, 
			  "about");
	}

	@Override
	public void onCommand(MessageReceivedEvent e, String[] args){
		Utils.deleteMessage(e.getChannel(), e.getMessage());
		Utils.info(e.getChannel(), "Hello!\nI am a bot made by Smc.\nI am used to create commands for now, but there are future features planned.");
	}
	
}
