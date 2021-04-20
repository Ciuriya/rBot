package me.smc.sb.discordcommands;

import org.pircbotx.Channel;
import org.pircbotx.PircBotX;

import me.smc.sb.main.Main;
import me.smc.sb.perm.Permissions;
import me.smc.sb.utils.Utils;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

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
		String msg = "IRC\n";
		
		msg += scanBot(Main.ircBot) + "\n\nTwitch\n";
		msg += scanBot(Main.twitchBot);

		Utils.info(e.getChannel(), msg);
	}
	
	private String scanBot(PircBotX bot){
		if(bot == null) return "This bot object is null!";
		else{
			String msg = "Status: " + (bot.isConnected() ? "Connected" : "Disconnected");
			
			if(bot.isConnected())
				if(!bot.getUserBot().getChannels().isEmpty()){
					msg += "\nConnected channels: ";
					
					for(Channel channel : bot.getUserBot().getChannels())
						msg += channel.getName() + ", ";
					
					msg = msg.substring(0, msg.length() - 2);
				}	
			
			return msg;
		}
	}
	
}
