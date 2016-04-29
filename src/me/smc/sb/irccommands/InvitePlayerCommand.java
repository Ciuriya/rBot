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

public class InvitePlayerCommand extends IRCCommand{

	public static Map<Team, Game> allowedInviters;
	
	public InvitePlayerCommand(){
		super("Lets you invite a player to your tournament match (provided he is in your team)",
			  "<player name> ",
			  null,
			  "invite");
		allowedInviters = new HashMap<>();
	}

	@Override
	public String onCommand(MessageEvent<PircBotX> e, PrivateMessageEvent<PircBotX> pe, String discord, String[] args){
		String argCheck = Utils.checkArguments(args, 1);
		if(argCheck.length() > 0) return argCheck;
		
		if(e == null || discord != null || pe != null) return "You cannot invite a player from here!";
		
		String playerName = "";
		
		for(String arg : args) playerName += arg + " ";
		playerName = playerName.substring(0, playerName.length() - 1);
		
		for(Team team : allowedInviters.keySet()){
			boolean senderAllowed = false, receiverAllowed = false;
			
			for(Player pl : team.getPlayers())
				if(pl.getName().replaceAll(" ", "_").equalsIgnoreCase(e.getUser().getNick().replaceAll(" ", "_")))
					senderAllowed = true;
				else if(pl.getName().replaceAll(" ", "_").equalsIgnoreCase(playerName.replaceAll(" ", "_")))
					receiverAllowed = true;
			
			if(senderAllowed && receiverAllowed)
				allowedInviters.get(team).invitePlayer(playerName.replaceAll(" ", "_"));
		}
		
		return "";
	}
	
}
