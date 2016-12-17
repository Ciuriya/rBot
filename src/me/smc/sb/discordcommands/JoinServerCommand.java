package me.smc.sb.discordcommands;

import me.smc.sb.utils.Utils;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

public class JoinServerCommand extends GlobalCommand{

	public JoinServerCommand(){
		super(null, 
			  " - Replies with the link to add the bot to a server.", 
			  "{prefix}joinserver\nThis command lets you have the link to invite the bot to a server\n\n" +
			  "----------\nUsage\n----------\n{prefix}joinserver - Yields the link to the bot's authorization page\n\n" + 
			  "----------\nAliases\n----------\nThere are no aliases.",  
			  true, 
			  "joinserver");
	}

	@Override
	public void onCommand(MessageReceivedEvent e, String[] args){
		Utils.infoBypass(e.getChannel(), "https://discordapp.com/oauth2/authorize?&client_id=168498858937024512&scope=bot&permissions=0");
	}

}
