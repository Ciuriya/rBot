package me.smc.sb.irccommands;

import java.util.LinkedList;

import org.pircbotx.PircBotX;
import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.PrivateMessageEvent;

import me.smc.sb.multi.Player;
import me.smc.sb.multi.Tournament;
import me.smc.sb.perm.Permissions;
import me.smc.sb.utils.Utils;

public class SetTeamPlayersCommand extends IRCCommand{

	public SetTeamPlayersCommand(){
		super("Sets a team's players.",
			  "<tournament name> {<team name>} captain,player2,player3...",
			  Permissions.IRC_BOT_ADMIN,
			  "teamsetplayers");
	}

	@Override
	public void onCommand(MessageEvent<PircBotX> e, PrivateMessageEvent<PircBotX> pe, String discord, String[] args){
		if(!Utils.checkArguments(e, pe, discord, args, 3)) return;
		
		String validation = Utils.validateTournamentAndTeam(e, pe, discord, args);
		if(validation.length() == 0) return;
		
		String msg = "";
		for(String arg : args) msg += arg + " ";
		msg = msg.substring(0, msg.length() - 1).split("}")[1].substring(1);
		
		if(!msg.contains(",")){Utils.info(e, pe, discord, "You need more players!"); return;}
		
		LinkedList<Player> players = new LinkedList<>();
		
		for(String p : msg.split(","))
			players.add(new Player(p));
		
		Tournament t = Tournament.getTournament(validation.split("\\|")[1]);
		t.getTeam(validation.split("\\|")[0]).setPlayers(players);
		t.getTeam(validation.split("\\|")[0]).save(false);
		Utils.info(e, pe, discord, "Set players to the " + validation.split("\\|")[0] + " team!");
	}
	
}
