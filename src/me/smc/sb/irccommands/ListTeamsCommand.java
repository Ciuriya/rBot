package me.smc.sb.irccommands;

import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.PrivateMessageEvent;

import me.smc.sb.multi.Team;
import me.smc.sb.multi.Tournament;
import me.smc.sb.perm.Permissions;
import me.smc.sb.utils.Utils;

public class ListTeamsCommand extends IRCCommand{

	public ListTeamsCommand(){
		super("Lists all teams.",
			  "<tournament name> ",
			  Permissions.TOURNEY_ADMIN,
			  "teamlist");
	}
	
	@Override
	public String onCommand(MessageEvent e, PrivateMessageEvent pe, String discord, String[] args){
		String argCheck = Utils.checkArguments(args, 1);
		if(argCheck.length() > 0) return argCheck;
		
		String tournamentName = "";
		
		for(int i = 0; i < args.length; i++) tournamentName += args[i] + " ";
		Tournament t = Tournament.getTournament(tournamentName.substring(0, tournamentName.length() - 1));
		
		if(t == null) return "Invalid tournament!";
		
		String user = Utils.toUser(e, pe);

		if(t.isAdmin(user)){
			String msg = "Teams in " + t.getName();
			if(discord != null) msg = "```" + msg + "\n";
			else msg += "=";
			
			for(Team team : t.getTeams()){
				msg += team.getTeamName();
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
		
		return "";
	}
	
}
