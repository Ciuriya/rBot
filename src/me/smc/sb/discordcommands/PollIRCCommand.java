package me.smc.sb.discordcommands;

import org.pircbotx.Channel;

import me.itsghost.jdiscord.events.UserChatEvent;
import me.smc.sb.main.Main;
import me.smc.sb.perm.Permissions;
import me.smc.sb.utils.Utils;

public class PollIRCCommand extends GlobalCommand{

	public PollIRCCommand(){
		super(Permissions.IRC_BOT_ADMIN, 
			  " - Checks irc bot status", 
			  "{prefix}pollIRC\nThis command reports the irc bot's status.\n\n" +
			  "----------\nUsage\n----------\n{prefix}pollIRC - Reports irc bot's status\n\n" +
			  "----------\nAliases\n----------\nThere are no aliases.",  
			  true,
			  "pollIRC");
	}

	@Override
	public void onCommand(UserChatEvent e, String[] args){
		e.getMsg().deleteMessage();
		if(Main.ircBot == null) Utils.info(e.getGroup(), "The IRC bot object is null! Make sure to tell Smc!");
		else{
			String msg = "Status: " + (Main.ircBot.isConnected() ? "Connected" : "Disconnected");
			
			if(Main.ircBot.isConnected())
				if(!Main.ircBot.getUserBot().getChannels().isEmpty()){
					msg += "\nConnected channels: ";
					for(Channel channel : Main.ircBot.getUserBot().getChannels())
						msg += channel.getName() + ", ";
					msg = msg.substring(0, msg.length() - 2);
				}	
			
			Utils.info(e.getGroup(), msg);	
		}
	}
	
}
