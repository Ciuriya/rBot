package me.smc.sb.discordcommands;

import org.pircbotx.Channel;

import me.smc.sb.main.Main;
import me.smc.sb.perm.Permissions;
import me.smc.sb.utils.Utils;
import net.dv8tion.jda.events.message.MessageReceivedEvent;

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
	public void onCommand(MessageReceivedEvent e, String[] args){
		Utils.deleteMessage(e.getChannel(), e.getMessage());
		if(Main.ircBot == null) Utils.info(e.getChannel(), "The IRC bot object is null! Make sure to tell Smc!");
		else{
			String msg = "Status: " + (Main.ircBot.isConnected() ? "Connected" : "Disconnected");
			
			if(Main.ircBot.isConnected())
				if(!Main.ircBot.getUserBot().getChannels().isEmpty()){
					msg += "\nConnected channels: ";
					for(Channel channel : Main.ircBot.getUserBot().getChannels())
						msg += channel.getName() + ", ";
					msg = msg.substring(0, msg.length() - 2);
				}	
			
			Utils.info(e.getChannel(), msg);	
		}
	}
	
}
