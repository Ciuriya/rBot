package me.smc.sb.irccommands;

import org.pircbotx.PircBotX;
import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.PrivateMessageEvent;

import me.smc.sb.multi.Tournament;
import me.smc.sb.perm.Permissions;
import me.smc.sb.utils.Utils;

public class SetMatchTeamsCommand extends IRCCommand{

	public SetMatchTeamsCommand(){
		super("Sets a match's participating teams.",
			  "<tournament name> {team 1} {team 2} <match num> ",
			  Permissions.IRC_BOT_ADMIN,
			  "mpsetteams");
	}

	@Override
	public String onCommand(MessageEvent<PircBotX> e, PrivateMessageEvent<PircBotX> pe, String discord, String[] args){
		String argCheck = Utils.checkArguments(args, 3);
		if(argCheck.length() > 0) return argCheck;
		
		String validation = Utils.validateTournamentAndTeam(e, pe, discord, args);
		if(!validation.contains("|")) return validation;
		
		String teamName2 = "";
		String fullArgs = "";
		for(String arg : args) fullArgs += arg + " ";
		fullArgs = fullArgs.substring(0, fullArgs.length() - 1);
		
		for(String arg : fullArgs.split("\\{")[2].split(" ")){
			teamName2 += arg + " ";
			if(arg.contains("}")){
				teamName2 = teamName2.substring(0, teamName2.length() - 2);
				break;
			}
		}
		
		Tournament t = Tournament.getTournament(validation.split("\\|")[1]);
		if(t.getTeam(teamName2) == null){
			return "Invalid second team name!";
		}else{
			if(Utils.stringToInt(args[args.length - 1]) == -1) return "Match number needs to be a number!";
			if(t.getMatch(Utils.stringToInt(args[args.length - 1])) == null) return "The match is invalid!";
			
			t.getMatch(Utils.stringToInt(args[args.length - 1]))
			.setTeams(t.getTeam(validation.split("\\|")[0]), t.getTeam(teamName2));
			
			t.getMatch(Utils.stringToInt(args[args.length - 1])).save(false);
			
			return "Match teams were set!";
		}
	}
	
}
