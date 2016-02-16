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
			  Permissions.IRC_BOT_ADMIN,
			  "teamcreate");
	}

	@Override
	public void onCommand(MessageEvent<PircBotX> e, PrivateMessageEvent<PircBotX> pe, String discord, String[] args){
		if(!Utils.checkArguments(e, pe, discord, args, 2)) return;
		
		String validation = Utils.validateTournamentAndTeam(e, pe, discord, args);
		if(validation.length() == 0) return;
		
		new Team(Tournament.getTournament(validation.split("\\|")[1]), validation.split("\\|")[0]);
		Utils.info(e, pe, discord, "Created the " + validation.split("\\|")[0] + " team!");
	}
	
}