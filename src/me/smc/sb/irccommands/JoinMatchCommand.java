package me.smc.sb.irccommands;

import java.util.HashMap;
import java.util.Map;

import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.PrivateMessageEvent;

import me.smc.sb.multi.Game;

public class JoinMatchCommand extends IRCCommand{

	public static Map<String, Game> gameInvites;
	
	public JoinMatchCommand(){
		super("Joins the tournament game you were invited to.",
			  " ",
			  null,
			  "join");
		gameInvites = new HashMap<>();
	}
	
	@Override
	public String onCommand(MessageEvent e, PrivateMessageEvent pe, String discord, String[] args){
		if(pe == null || discord != null || e != null) return "You were not invited to any game!";
		
		if(!gameInvites.isEmpty())
			for(String invited : gameInvites.keySet())
				if(invited.equalsIgnoreCase(pe.getUser().getNick().replaceAll(" ", "_"))){
					gameInvites.get(invited).acceptInvite(invited.replaceAll("_", " "));
					return "";
				}
		
		return "You were not invited to any game!";
	}
	
}
