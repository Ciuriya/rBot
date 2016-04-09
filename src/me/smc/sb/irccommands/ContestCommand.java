package me.smc.sb.irccommands;

import java.util.ArrayList;
import java.util.List;

import org.pircbotx.PircBotX;
import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.PrivateMessageEvent;

import me.smc.sb.multi.Game;
import me.smc.sb.utils.Utils;

public class ContestCommand extends IRCCommand{
	
	public static List<Game> gamesAllowedToContest;
	
	public ContestCommand(){
		super("Allows teams to reverse the score of the last pick.",
			  " ",
			  null,
			  "contest");
		gamesAllowedToContest = new ArrayList<>();
	}
	
	@Override
	public String onCommand(MessageEvent<PircBotX> e, PrivateMessageEvent<PircBotX> pe, String discord, String[] args){
		if(e == null || discord != null || pe != null) return "You cannot contest a score in here!";
		
		String userName = Utils.toUser(e, pe);
		
		if(!gamesAllowedToContest.isEmpty())
			for(Game game : gamesAllowedToContest)
				if(game.verifyPlayer(userName.replaceAll(" ", "_"))){
					game.acceptContest(userName);
					return "";
				}
		
		return "You cannot contest right now.";
	}
	
}
