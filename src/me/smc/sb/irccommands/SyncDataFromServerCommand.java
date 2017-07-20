package me.smc.sb.irccommands;

import java.util.ArrayList;

import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.PrivateMessageEvent;

import me.smc.sb.perm.Permissions;
import me.smc.sb.tourney.Match;
import me.smc.sb.tourney.Team;
import me.smc.sb.tourney.Tournament;
import me.smc.sb.utils.RemotePatyServerUtils;
import me.smc.sb.utils.Utils;

public class SyncDataFromServerCommand extends IRCCommand{

	public SyncDataFromServerCommand(){
		super("Syncs teams and matches for a tournament from the web server.",
			  "<tournament name>",
			  Permissions.TOURNEY_ADMIN,
			  "sync");
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
			for(Match match : new ArrayList<>(Match.getMatches(t))){
				Match.removeMatch(t, match.getMatchNum());
			}
			
			for(Team team : new ArrayList<>(Team.getTeams(t))){
				Team.removeTeam(t, team.getTeamName());
			}
			
			new Thread(new Runnable(){
				public void run(){
					RemotePatyServerUtils.syncTeams(t.get("name"));
					RemotePatyServerUtils.syncMatches(t.get("name"));
					
					Utils.info(e, pe, discord, Team.getTeams(t).size() + " teams and " + Match.getMatches(t).size() + " matches synced!");
				}
			}).start();
		}
		
		return "";
	}
	
}
