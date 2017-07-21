package me.smc.sb.irccommands;

import java.util.ArrayList;
import java.util.List;

import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.PrivateMessageEvent;

import me.smc.sb.tourney.PlayingTeam;
import me.smc.sb.utils.Utils;

public class RandomCommand extends IRCCommand{

	public static List<PlayingTeam> waitingForRolls;
	
	public RandomCommand(){
		super("Rolls a random number from 1 to 100 (or even a desired maximum).",
			  " ",
			  null,
			  true,
			  "random", "dice", "toss", "flip", "r");
		waitingForRolls = new ArrayList<>();
	}

	@Override
	public String onCommand(MessageEvent e, PrivateMessageEvent pe, String discord, String[] args){
		String userName = Utils.toUser(e, pe);
		int max = 100;
		int random = 0;
		
		if(args.length > 0 && Utils.stringToInt(args[0]) >= 1)
			max = Utils.stringToInt(args[0]);
		
		if(!waitingForRolls.isEmpty() && !Utils.isTwitch(e))
			for(PlayingTeam team : waitingForRolls){
				if(team.getTeam().has(userName)){
					max = 100;
					random = Utils.fetchRandom(1, max);
					Utils.info(e, pe, discord, userName.replaceAll("_", " ") + " rolled " + random + "!");
					team.setRoll(random);
					
					return "";
				}
			}
		
		if(random == 0) random = Utils.fetchRandom(1, max);
		
		Utils.info(e, pe, discord, userName.replaceAll("_", " ") + " rolled " + random + "!");
				
		return "";
	}
	
}
