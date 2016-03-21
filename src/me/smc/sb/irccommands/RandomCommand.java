package me.smc.sb.irccommands;

import java.util.HashMap;
import java.util.Map;

import org.pircbotx.PircBotX;
import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.PrivateMessageEvent;

import me.smc.sb.multi.Game;
import me.smc.sb.multi.Player;
import me.smc.sb.multi.Team;
import me.smc.sb.utils.Utils;

public class RandomCommand extends IRCCommand{

	public static Map<Team, Game> waitingForRolls;
	
	public RandomCommand(){
		super("Rolls a random number from 1 to 100.",
			  " ",
			  null,
			  "random");
		waitingForRolls = new HashMap<>();
	}

	@Override
	public String onCommand(MessageEvent<PircBotX> e, PrivateMessageEvent<PircBotX> pe, String discord, String[] args){
		String userName = Utils.toUser(e, pe);
		
		int random = Utils.fetchRandom(1, 100);
		Utils.info(e, pe, discord, userName + " rolled " + random + "!");
		
		if(!waitingForRolls.isEmpty())
			for(Team team : waitingForRolls.keySet())
				for(Player pl : team.getPlayers())
					if(pl.getName().replaceAll(" ", "_").equalsIgnoreCase(userName.replaceAll(" ", "_"))){
						waitingForRolls.get(team).acceptRoll(userName.replaceAll(" ", "_"), random);
						return "";
					}
		
		return "";
	}
	
}
