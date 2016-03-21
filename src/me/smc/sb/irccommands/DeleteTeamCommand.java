package me.smc.sb.irccommands;

import org.pircbotx.PircBotX;
import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.PrivateMessageEvent;

import me.smc.sb.multi.Team;
import me.smc.sb.multi.Tournament;
import me.smc.sb.perm.Permissions;
import me.smc.sb.utils.Utils;

public class DeleteTeamCommand extends IRCCommand{

	public DeleteTeamCommand(){
		super("Deletes a tournament team.",
			  "<tournament name> {<team name>} ",
			  Permissions.IRC_BOT_ADMIN,
			  "teamdelete");
	}

	@Override
	public String onCommand(MessageEvent<PircBotX> e, PrivateMessageEvent<PircBotX> pe, String discord, String[] args){
		String argCheck = Utils.checkArguments(args, 2);
		if(argCheck.length() > 0) return argCheck;
		
		String validation = Utils.validateTournamentAndTeam(e, pe, discord, args);
		if(!validation.contains("|")) return validation;
		
		Team team = Tournament.getTournament(validation.split("\\|")[1]).getTeam(validation.split("\\|")[0]);
		
		if(team == null)
			return "Could not find team!";
		else{
			Tournament.getTournament(validation.split("\\|")[1]).removeTeam(validation.split("\\|")[0]);
			return "Deleted the " + validation.split("\\|")[0] + " team!";	
		}
	}
	
}
