package me.smc.sb.drpg;

import me.smc.sb.utils.Utils;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

public class RPGTutorial{

	public RPGTutorial(MessageReceivedEvent e){
		Utils.info(e.getAuthor().getPrivateChannel(), "Coming soon!");
		//add user to db if not in
	}
	
}
