package me.smc.sb.irccommands;

import java.util.ArrayList;
import java.util.List;

import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.PrivateMessageEvent;

import me.smc.sb.tourney.PlayingTeam;

public class PassTurnCommand extends IRCCommand{

	public static List<PlayingTeam> passingTeams;
	
	public PassTurnCommand(){
		super("Lets you surrender the starting turn to the other team.",
			  " ",
			  null,
			  "pass");
		passingTeams = new ArrayList<>();
	}

	@Override
	public String onCommand(MessageEvent e, PrivateMessageEvent pe, String discord, String[] args){
		if(e == null || discord != null || pe != null) return "You cannot pass the starting turn in here!";
		
		String userName = e.getUser().getNick();
		PlayingTeam passed = null;
		
		if(!passingTeams.isEmpty())
			for(PlayingTeam team : passingTeams)
				if(team.getTeam().has(userName)){
					team.getGame().switchNextTeam();
					team.getGame().getBanchoHandle().sendMessage("Passed!", false);
					passed = team;
				}
		
		if(passed != null) passingTeams.remove(passed);
		
		return "";
	}
	
}