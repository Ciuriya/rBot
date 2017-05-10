package me.smc.sb.irccommands;

import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.PrivateMessageEvent;

import me.smc.sb.perm.Permissions;
import me.smc.sb.utils.Utils;

public class HelpCommand extends IRCCommand{

	public HelpCommand(){
		super("Brings up a list of all commands!",
			  "",
			  null,
			  true,
			  "help", "?");
	}

	public String onCommand(MessageEvent e, PrivateMessageEvent pe, String discord, String[] args){
		String msg = "Commands";
		
		if(discord != null){
			msg = "```" + msg;
			
			for(IRCCommand ic : IRCCommand.commands){
				if(!Permissions.hasPerm(Utils.toUser(e, pe), ic.getPerm())) continue;
				
				msg += "\n\n";
				
				for(String name : ic.getNames())
					msg += "!" + name + " | ";
				msg = msg.substring(0, msg.length() - 2) + ic.getUsage() +  "- " + ic.getDescription();
			}
		}
		
		if(discord == null && !Utils.isTwitch(e))
			return "[http://tyjoll.com/commands.html You can find the available commands here!]";
		else if(discord == null && Utils.isTwitch(e)){
			msg += ": ";
			
			for(IRCCommand ic : IRCCommand.commands)
				if(ic.allowsTwitch() && Utils.isTwitch(e))
					msg += "!" + ic.getNames()[0] + ", ";

			Utils.info(e, pe, discord, msg.substring(0, msg.length() - 2));
		}else{
			msg += "```";
			
			if(msg.length() > 2000){
				int max = (int) Math.ceil((double) msg.length() / 2000.0);
				
				for(int i = 0; i < max; i++){
					String message = "";
					
					if(i != 0) message = "```";
					
					if(i != max - 1) message += msg.substring(i * 1990, (i + 1) * 1990) + "```";
					else message += msg.substring(i * 1990, msg.length());
					
					Utils.info(e, pe, discord, message);
				}
			}else Utils.info(e, pe, discord, msg);
		}
		
		return "";
	}

}
