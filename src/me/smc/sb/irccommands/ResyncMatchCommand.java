package me.smc.sb.irccommands;

import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.PrivateMessageEvent;

import me.smc.sb.perm.Permissions;
import me.smc.sb.tourney.Match;
import me.smc.sb.tourney.Tournament;
import me.smc.sb.utils.RemotePatyServerUtils;
import me.smc.sb.utils.Utils;

public class ResyncMatchCommand extends IRCCommand{

	public ResyncMatchCommand(){
		super("Resyncs a match.",
			  "<tournament name> <match num> ",
			  Permissions.TOURNEY_ADMIN,
			  "resync");
	}
	
	@Override
	public String onCommand(MessageEvent e, PrivateMessageEvent pe, String discord, String[] args){
		String argCheck = Utils.checkArguments(args, 2);
		if(argCheck.length() > 0) return argCheck;
		
		String tournamentName = "";
		
		for(int i = 0; i < args.length - 1; i++) tournamentName += args[i] + " ";
		Tournament t = Tournament.getTournament(tournamentName.substring(0, tournamentName.length() - 1));
		
		if(t == null) return "Invalid tournament!";
		if(Utils.stringToInt(args[args.length - 1]) == -1) return "Match number needs to be a number!";
		
		String matchId = args[args.length - 1];
		if(matchId.length() == 0) return "The match number is invalid!";
		
		String user = Utils.toUser(e, pe);
		
		if(t.isAdmin(user)){
			Match.removeMatch(t, Integer.parseInt(matchId));
			RemotePatyServerUtils.syncMatch(t.get("name"), matchId);
			
			if(Match.getMatch(t, Integer.parseInt(matchId)) != null){
				return "Match resynced successfully!";
			}else return "Could not resync the match!";
		}
		
		return "";
	}
	
}
