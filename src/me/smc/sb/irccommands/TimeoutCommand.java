package me.smc.sb.irccommands;

import java.util.ArrayList;
import java.util.List;

import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.PrivateMessageEvent;

import me.smc.sb.tourney.Game;
import me.smc.sb.utils.Utils;

public class TimeoutCommand extends IRCCommand{

	public static List<Game> gamesAllowedToTimeout;
	
	public TimeoutCommand(){
		super("Allows teams to take a small break during a match.",
			  " ",
			  null,
			  "timeout", "time", "break");
		
		gamesAllowedToTimeout = new ArrayList<>();
	}
	
	@Override
	public String onCommand(MessageEvent e, PrivateMessageEvent pe, String discord, String[] args){		
		if(e == null || discord != null || pe != null) return "You cannot timeout in here!";
		
		String userName = Utils.toUser(e, pe);
		
		if(!gamesAllowedToTimeout.isEmpty())
			for(Game game : gamesAllowedToTimeout)
				if(game.getLobbyManager().verify(userName)){
					game.timeout(userName);
					
					return "";
				}
		
		return "You cannot timeout right now.";
	}
	
}
