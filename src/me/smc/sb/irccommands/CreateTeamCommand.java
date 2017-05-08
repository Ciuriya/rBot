package me.smc.sb.irccommands;

import org.pircbotx.PircBotX;
import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.PrivateMessageEvent;

import me.smc.sb.multi.Team;
import me.smc.sb.multi.Tournament;
import me.smc.sb.perm.Permissions;
import me.smc.sb.utils.Utils;

public class CreateTeamCommand extends IRCCommand{

	public CreateTeamCommand(){
		super("Creates a team for tournaments.",
			  "<tournament name> {<name>} ",
			  Permissions.TOURNEY_ADMIN,
			  "teamcreate");
	}

	@Override
	public String onCommand(MessageEvent<PircBotX> e, PrivateMessageEvent<PircBotX> pe, String discord, String[] args){
		String argCheck = Utils.checkArguments(args, 2);
		if(argCheck.length() > 0) return argCheck;
		
		String validation = Utils.validateTournamentAndTeam(e, pe, discord, args);
		if(!validation.contains("|")) return validation;
		
		Tournament t = Tournament.getTournament(validation.split("\\|")[1]);
		
		String user = Utils.toUser(e, pe);
		
		if(t.isAdmin(user)){
			new Team(t, validation.split("\\|")[0]);
			return "Created the " + validation.split("\\|")[0] + " team!";
		}
		
		return "";
	}
	
}