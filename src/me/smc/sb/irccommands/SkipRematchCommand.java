package me.smc.sb.irccommands;

import java.util.ArrayList;
import java.util.List;

import org.pircbotx.PircBotX;
import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.PrivateMessageEvent;

import me.smc.sb.multi.Game;
import me.smc.sb.utils.Utils;

public class SkipRematchCommand extends IRCCommand{

	public static List<Game> gamesAllowedToSkip;
	
	public SkipRematchCommand(){
		super("Allows teams to skip a match's automatic rematch.",
			  " ",
			  null,
			  "skiprematch");
		gamesAllowedToSkip = new ArrayList<>();
	}
	
	@Override
	public String onCommand(MessageEvent<PircBotX> e, PrivateMessageEvent<PircBotX> pe, String discord, String[] args){
		if(e == null || discord != null || pe != null) return "You cannot skip a rematch in here!";
		
		String userName = Utils.toUser(e, pe);
		
		if(!gamesAllowedToSkip.isEmpty())
			for(Game game : gamesAllowedToSkip)
				if(game.verifyPlayer(userName.replaceAll(" ", "_"))){
					game.acceptSkipRematch(userName);
					return "";
				}
		
		return "There is no rematch happening!";
	}
	
}
