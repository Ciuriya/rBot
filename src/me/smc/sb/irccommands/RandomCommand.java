package me.smc.sb.irccommands;

import java.util.HashMap;
import java.util.Map;

import org.pircbotx.PircBotX;
import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.PrivateMessageEvent;

import me.smc.sb.main.Main;
import me.smc.sb.multi.Game;
import me.smc.sb.utils.Utils;

public class RandomCommand extends IRCCommand{

	public static Map<String, Game> waitingForRolls;
	
	public RandomCommand(){
		super("Rolls a random number from 1 to 100.",
			  " ",
			  null,
			  "random");
		waitingForRolls = new HashMap<String, Game>();
	}

	@Override
	public void onCommand(MessageEvent<PircBotX> e, PrivateMessageEvent<PircBotX> pe, String discord, String[] args){
		String userName = "";
		
		if(discord == null) userName = Utils.toUser(e, pe);
		else userName = Main.api.getUserById(discord).getUsername();
		
		int random = Utils.fetchRandom(1, 100);
		Utils.info(e, pe, discord, userName + " rolled " + random + "!");
		
		if(waitingForRolls.containsKey(userName.replaceAll(" ", "_")) && pe == null && discord == null){
			Game game = waitingForRolls.get(userName.replaceAll(" ", "_"));
			waitingForRolls.remove(userName.replaceAll(" ", "_"));
			game.acceptRoll(userName.replaceAll(" ", "_"), random);
		}
	}
	
}
