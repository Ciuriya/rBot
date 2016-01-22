package me.smc.sb.irccommands;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import org.pircbotx.PircBotX;
import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.PrivateMessageEvent;

import me.smc.sb.main.Main;
import me.smc.sb.utils.Log;

public abstract class IRCCommand{

	private final String[] names;
	private final String description;
	public static List<IRCCommand> commands;
	
	public IRCCommand(String description, String...names){
		this.names = names;
		this.description = description;
	}
	
	public String getDescription(){
		return description;
	}
	
	public String[] getNames(){
		return names;
	}
	
	public boolean isName(String name){
		for(String n : names)
			if(n.equalsIgnoreCase(name))
				return true;
		return false;
	}
	
	public static void handleCommand(MessageEvent<PircBotX> e, PrivateMessageEvent<PircBotX> pe, String msg){
		if(pe != null)
		try{
			Main.ircBot.sendIRC().joinChannel(pe.getUser().getNick());
		}catch(Exception ex){
			Log.logger.log(Level.INFO, "Could not join channel " + pe.getUser().getNick());
		}
		
		String[] split = msg.split(" ");
		for(IRCCommand ic : commands)
			if(ic.isName(split[0])){
				String[] args = msg.replace(split[0] + " ", "").split(" ");
				if(!msg.contains(" ")) args = new String[]{};
				ic.onCommand(e, pe, args);
				return;
			}
		if(e != null){
			e.respond("This is not a command!");
			e.respond("Use !help if you are lost!");	
		}else{
			Main.ircBot.sendIRC().message(pe.getUser().getNick(), "This is not a command!");
			Main.ircBot.sendIRC().message(pe.getUser().getNick(), "Use !help if you are lost!");	
		}
	}
	
	public static void registerCommands(){
		commands = new ArrayList<IRCCommand>();
		commands.add(new HelpCommand());
	}
	
	public abstract void onCommand(MessageEvent<PircBotX> e, PrivateMessageEvent<PircBotX> pe, String[] args);
	
}
