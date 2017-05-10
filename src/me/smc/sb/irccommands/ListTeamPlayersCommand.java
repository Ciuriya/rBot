package me.smc.sb.irccommands;

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
			  Permissions.TOURNEY_ADMIN,
			  "teamplayerlist");
	}
	
	@Override
	public String onCommand(MessageEvent e, PrivateMessageEvent pe, String discord, String[] args){
		String argCheck = Utils.checkArguments(args, 2);
		if(argCheck.length() > 0) return argCheck;
		
		String validation = Utils.validateTournamentAndTeam(e, pe, discord, args);
		if(!validation.contains("|")) return validation;
		
		Tournament t = Tournament.getTournament(validation.split("\\|")[1]);
		
		String user = Utils.toUser(e, pe);

		if(t.isAdmin(user)){
			Team team = t.getTeam(validation.split("\\|")[0]);
			
			if(team == null)
				return "Could not find team!";
			else{
				String msg = "Team " + team.getTeamName() + " in " + Tournament.getTournament(validation.split("\\|")[1]).getName();
				if(discord != null) msg = "```" + msg + "\n";
				else msg += "=";
				
				for(Player player : team.getPlayers()){
					msg += player.getName();
					if(discord != null) msg += "\n";
					else msg += "=";
				}
				
				if(discord == null){
					String built = "";
					for(String part : msg.split("=")){
						if(part.isEmpty()) continue;
						if(e == null && pe == null) built += part + "\n";
						else Utils.info(e, pe, discord, part);
					}
					
					if(built.length() > 0) return built.substring(0, built.length() - 1);
				}else return msg + "```";
			}
		}
		
		return "";
	}
	
}
