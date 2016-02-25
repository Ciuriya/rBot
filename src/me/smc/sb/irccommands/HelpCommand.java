package me.smc.sb.irccommands;

import org.pircbotx.PircBotX;
import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.PrivateMessageEvent;

import me.smc.sb.perm.Permissions;
import me.smc.sb.utils.Utils;

public class HelpCommand extends IRCCommand{

	public HelpCommand(){
		super("Brings up a list of all commands!",
			  "",
			  null,
			  "help", "?");
	}

	public void onCommand(MessageEvent<PircBotX> e, PrivateMessageEvent<PircBotX> pe, String discord, String[] args){
		String msg = "Commands";
		if(discord != null) msg = "```" + msg;
		
		for(IRCCommand ic : IRCCommand.commands){
			if(!Permissions.hasPerm(Utils.toUser(e, pe), ic.getPerm())) continue;
			
			if(discord != null) msg += "\n\n";
			else msg += "=";
			
			for(String name : ic.getNames())
				msg += "!" + name + " | ";
			msg = msg.substring(0, msg.length() - 2) + ic.getUsage() +  "- " + ic.getDescription();
		}
		
		if(discord == null){
			int count = 0;
			for(String part : msg.split("=")){
				if(part.isEmpty()) continue;
				if(count >= 5){
					Utils.info(e, pe, discord, "And " + (msg.split("=").length - count) + " more commands!");
					break;
				}
				Utils.info(e, pe, discord, part);
				count++;
			}
		}else Utils.info(e, pe, discord, msg + "```");
	}

}
