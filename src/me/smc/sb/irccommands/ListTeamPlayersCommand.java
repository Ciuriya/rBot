package me.smc.sb.irccommands;

import org.pircbotx.PircBotX;
import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.PrivateMessageEvent;

import me.smc.sb.multi.Player;
import me.smc.sb.multi.Team;
import me.smc.sb.multi.Tournament;
import me.smc.sb.perm.Permissions;
import me.smc.sb.utils.Utils;

public class ListTeamPlayersCommand extends IRCCommand{

	public ListTeamPlayersCommand(){
		super("Lists all players on a team.",
			  "<tournament name> {team name} ",
			  Permissions.IRC_BOT_ADMIN,
			  "teamplayerlist");
	}
	
	@Override
	public void onCommand(MessageEvent<PircBotX> e, PrivateMessageEvent<PircBotX> pe, String discord, String[] args){
		if(!Utils.checkArguments(e, pe, discord, args, 2)) return;
		
		String validation = Utils.validateTournamentAndTeam(e, pe, discord, args);
		if(validation.length() == 0) return;
		
		Team team = Tournament.getTournament(validation.split("\\|")[1]).getTeam(validation.split("\\|")[0]);
		
		if(team == null)
			Utils.info(e, pe, discord, "Could not find team!");
		else{
			String msg = "Team " + team.getTeamName() + " in " + Tournament.getTournament(validation.split("\\|")[1]).getName();
			if(discord != null) msg = "```" + msg + "\n";
			else msg += "=";
			
			for(Player player : team.getPlayers()){
				msg += player.getName();
				if(discord != null) msg += "\n";
				else msg += "=";
			}
			
			if(discord == null)
				for(String part : msg.split("=")){
					if(part.isEmpty()) continue;
					Utils.info(e, pe, discord, part);
				}
			else Utils.info(e, pe, discord, msg + "```");
		}
	}
	
}
