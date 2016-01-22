package me.smc.sb.irccommands;

import org.pircbotx.PircBotX;
import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.PrivateMessageEvent;

import me.smc.sb.main.Main;

public class HelpCommand extends IRCCommand{

	public HelpCommand(){
		super("This command brings up a list of all commands!",
			  "help", "?");
	}

	public void onCommand(MessageEvent<PircBotX> e, PrivateMessageEvent<PircBotX> pe, String[] args){
		String msg = "Commands";
		
		for(IRCCommand ic : IRCCommand.commands){
			for(String name : ic.getNames())
				msg += "!" + name + " | ";
			msg = "\n- " + msg.substring(0, msg.length() - 2) + "- " + ic.getDescription();
		}
		
		for(String part : msg.split("\n")){
			if(part.isEmpty()) continue;
			if(pe != null) Main.ircBot.sendIRC().message(pe.getUser().getNick(), part);
			else e.respond(part);
		}
		
	}

}
