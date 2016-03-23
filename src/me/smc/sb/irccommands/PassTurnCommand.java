package me.smc.sb.irccommands;

import java.util.HashMap;
import java.util.HashSet;

import org.pircbotx.PircBotX;
import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.PrivateMessageEvent;

import me.smc.sb.multi.Game;
import me.smc.sb.multi.Player;
import me.smc.sb.multi.Team;

public class PassTurnCommand extends IRCCommand{

	public static HashMap<Team, Game> passingTeams;
	
	public PassTurnCommand(){
		super("Lets you surrender the starting turn to the other team.",
			  " ",
			  null,
			  "pass");
		passingTeams = new HashMap<>();
	}

	@Override
	public String onCommand(MessageEvent<PircBotX> e, PrivateMessageEvent<PircBotX> pe, String discord, String[] args){
		if(e == null || discord != null || pe != null) return "You cannot pass the starting turn in here!";
		
		String userName = e.getUser().getNick();
		
		if(!passingTeams.isEmpty())
			for(Team team : new HashSet<>(passingTeams.keySet()))
				for(Player player : team.getPlayers())
					if(player.getName().replaceAll(" ", "_").equalsIgnoreCase(userName.replaceAll(" ", "_"))){
						passingTeams.get(team).passFirstTurn();
						passingTeams.remove(team);
						return "";
					}
		
		return "";
	}
	
}