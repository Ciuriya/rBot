package me.smc.sb.drpg;

import me.smc.sb.utils.Utils;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

public class RPGHelpGuide{

	private MessageReceivedEvent event;
	
	public RPGHelpGuide(MessageReceivedEvent e, String arg){
		event = e;
		
		handle(arg);
	}
	
	private void handle(String arg){
		if(arg.length() == 0){
			generalHelp();
			return;
		}
	}
	
	private void generalHelp(){
		StringBuilder builder = new StringBuilder();
		
		builder.append("```This RPG can be played via PM or a normal text channel.\n\n" +
					   "If using a text channel, it is recommended to install the RPG feature to the channel.\n" +
					   "It removes the need to use the rpg command every time as it adds 'rpg' to every command sent.\n" +
					   "Example: from '~/rpg help' to '~/help', note that this removes regular rBot usage from the channel.\n" +
					   "To install it, simply use '~/rpg install' in the text channel of your choice (a separate channel is recommended).\n\n" +
					   "This is an expansive RPG and as such, this guide holds multiple sections outlining every feature available.\n\n" +
					   "Want to play? Use '~/rpg tutorial'\n" +
					   "Simply want to contact the developer? You can PM Smc or seek him out in this server: https://discord.gg/FCV3jS9\n\n" +
					   "New sections will be added here as they are released, stay tuned! (there will be a changelog link here eventually)```");
		
		Utils.infoBypass(event.getAuthor().getPrivateChannel(), builder.toString());
	}
	
}
