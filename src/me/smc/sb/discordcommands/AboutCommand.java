package me.smc.sb.discordcommands;

import me.smc.sb.utils.Utils;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

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
		Utils.info(e.getChannel(), "Hello!\nI am a bot made by Smc.\nI am used to create commands, play music, report osu! plays/stats and more!\n" +
								   "Use ~/help to see all available commands! (only those with message management permissions can see them all)");
	}
	
}
