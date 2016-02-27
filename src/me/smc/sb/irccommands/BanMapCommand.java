package me.smc.sb.irccommands;

import java.util.HashMap;
import java.util.Map;

import org.pircbotx.PircBotX;
import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.PrivateMessageEvent;

import me.smc.sb.multi.Game;
import me.smc.sb.multi.Team;
import me.smc.sb.utils.Utils;

public class BanMapCommand extends IRCCommand{

	public static Map<Team, Game> banningTeams;
	
	public BanMapCommand(){
		super("Bans a map from the tournament game.",
			  "<map url OR map #> ",
			  null,
			  "ban");
		banningTeams = new HashMap<Team, Game>();
	}

	@Override
	public void onCommand(MessageEvent<PircBotX> e, PrivateMessageEvent<PircBotX> pe, String discord, String[] args){
		if(!Utils.checkArguments(e, pe, discord, args, 1)) return;
		if(e == null || discord != null || pe != null){Utils.info(e, pe, discord, "You cannot select a map in here!"); return;}
		
		String userName = e.getUser().getNick();
		
		if(!banningTeams.isEmpty())
			for(Team team : banningTeams.keySet())
				if(team.getPlayers().get(0).getName().replaceAll(" ", "_").equalsIgnoreCase(userName.replaceAll(" ", "_"))){
					banningTeams.get(team).acceptBan(args[0]);
					return;
				}
						
	}
	
}
