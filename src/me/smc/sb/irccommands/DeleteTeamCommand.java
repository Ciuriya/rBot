package me.smc.sb.irccommands;

import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.PrivateMessageEvent;

import me.smc.sb.perm.Permissions;
import me.smc.sb.tourney.Team;
import me.smc.sb.tourney.Tournament;
import me.smc.sb.utils.Utils;

public class DeleteTeamCommand extends IRCCommand{

	public DeleteTeamCommand(){
		super("Deletes a tournament team.",
			  "<tournament name> {<team name>} ",
			  Permissions.TOURNEY_ADMIN,
			  "teamdelete");
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
			Team team = Team.getTeam(t, validation.split("\\|")[0]);
			
			if(team == null)
				return "Could not find team!";
			else{
				Team.removeTeam(t, validation.split("\\|")[0]);
				
				return "Deleted the " + validation.split("\\|")[0] + " team!";	
			}
		}
		
		return "";
	}
	
}
