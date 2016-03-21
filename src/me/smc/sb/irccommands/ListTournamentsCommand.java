package me.smc.sb.irccommands;

import org.pircbotx.PircBotX;
import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.PrivateMessageEvent;

import me.smc.sb.multi.Tournament;
import me.smc.sb.perm.Permissions;
import me.smc.sb.utils.Utils;

public class ListTournamentsCommand extends IRCCommand{

	public ListTournamentsCommand(){
		super("Lists all tournaments.",
			  " ",
			  Permissions.IRC_BOT_ADMIN,
			  "tournamentlist");
	}
	
	@Override
	public String onCommand(MessageEvent<PircBotX> e, PrivateMessageEvent<PircBotX> pe, String discord, String[] args){
		String msg = "Tournaments";
		if(discord != null) msg = "```" + msg + "\n";
		else msg += "=";
		
		for(Tournament tournament : Tournament.tournaments){
			msg += tournament.getName();
			if(discord != null) msg += "\n";
			else msg += "=";
		}
		
		if(discord == null){
			String built = "";
			for(String part : msg.split("=")){
				if(part.isEmpty()) continue;
				if(e == null && pe == null) built += part + "\n";
				else Utils.info(e, pe, discord, part);
			}
			
			if(built.length() > 0) return built.substring(0, built.length() - 1);
		}else return msg + "```";
		
		return "";
	}
}
