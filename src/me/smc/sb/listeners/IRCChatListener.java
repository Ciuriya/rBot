package me.smc.sb.listeners;

import java.util.logging.Level;

import org.pircbotx.PircBotX;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.PrivateMessageEvent;

import me.smc.sb.irccommands.IRCCommand;
import me.smc.sb.main.Main;
import me.smc.sb.utils.Log;
import me.smc.sb.utils.Utils;

public class IRCChatListener extends ListenerAdapter<PircBotX>{

	public static boolean yieldPMs = false;
	
	@Override
	public void onPrivateMessage(PrivateMessageEvent<PircBotX> e){
		String message = e.getMessage();
		Log.logger.log(Level.INFO, "PM/" + e.getUser().getNick() + ": " + message);
		if(yieldPMs) Utils.infoBypass(Main.api.getUserById("91302128328392704").getPrivateChannel(), "PM/" + e.getUser().getNick() + ": " + message);
		if(message.startsWith("!")) IRCCommand.handleCommand(null, e, null, message.substring(1));
	}
	
	@Override
	public void onMessage(MessageEvent<PircBotX> e){
		String message = e.getMessage();
		Log.logger.log(Level.INFO, "IRC/" + e.getUser().getNick() + ": " + message);	
		if(message.startsWith("!")) IRCCommand.handleCommand(e, null, null, message.substring(1));
	}

}
