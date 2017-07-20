package me.smc.sb.irccommands;

import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.PrivateMessageEvent;

import me.smc.sb.perm.Permissions;
import me.smc.sb.tourney.Match;
import me.smc.sb.tourney.Team;
import me.smc.sb.tourney.Tournament;
import me.smc.sb.utils.Utils;

public class SetMatchTeamsCommand extends IRCCommand{

	public SetMatchTeamsCommand(){
		super("Sets a match's participating teams.",
			  "<tournament name> {team 1} {team 2} <match num> ",
			  Permissions.TOURNEY_ADMIN,
			  "mpsetteams");
	}

	@Override
	public String onCommand(MessageEvent e, PrivateMessageEvent pe, String discord, String[] args){
		String argCheck = Utils.checkArguments(args, 3);
		if(argCheck.length() > 0) return argCheck;
		
		String validation = Utils.validateTournamentAndTeam(e, pe, discord, args);
		if(!validation.contains("|")) return validation;
		
		Tournament t = Tournament.getTournament(validation.split("\\|")[1]);
		
		String user = Utils.toUser(e, pe);
		
		if(t.isAdmin(user)){
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
			
			if(Team.getTeam(t, teamName2) == null){
				return "Invalid second team name!";
			}else{
				if(Utils.stringToInt(args[args.length - 1]) == -1) return "Match number needs to be a number!";
				if(Match.getMatch(t, Utils.stringToInt(args[args.length - 1])) == null) return "The match is invalid!";
				
				Match.getMatch(t, Utils.stringToInt(args[args.length - 1]))
				.setTeams(Team.getTeam(t, validation.split("\\|")[0]), Team.getTeam(t, teamName2));
				
				Match.getMatch(t, Utils.stringToInt(args[args.length - 1])).save(false);
				
				return "Match teams were set!";
			}
		}
		
		return "";
	}
	
}
