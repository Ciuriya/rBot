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
			  "<tournament name> {<name>} ",
			  Permissions.IRC_BOT_ADMIN,
			  "teamdelete");
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
			Tournament.getTournament(validation.split("\\|")[1]).removeTeam(validation.split("\\|")[0]);
			Utils.info(e, pe, discord, "Deleted the " + validation.split("\\|")[0] + " team!");	
		}
	}
	
}
