package me.smc.sb.irccommands;

import java.util.HashMap;
import java.util.Map;

import org.pircbotx.PircBotX;
import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.PrivateMessageEvent;

import me.smc.sb.multi.Game;
import me.smc.sb.utils.Utils;

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
	public void onCommand(MessageEvent<PircBotX> e, PrivateMessageEvent<PircBotX> pe, String discord, String[] args){
		if(pe == null || discord != null || e != null){Utils.info(e, pe, discord, "You were not invited to any game!"); return;}
		
		if(!gameInvites.isEmpty())
			for(String invited : gameInvites.keySet())
				if(invited.equalsIgnoreCase(pe.getUser().getNick().replaceAll(" ", "_"))){
					gameInvites.get(invited).acceptInvite(invited.replaceAll("_", " "));
					return;
				}
		Utils.info(e, pe, discord, "You were not invited to any game!");
	}
	
}
