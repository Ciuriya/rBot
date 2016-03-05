package me.smc.sb.irccommands;

import org.pircbotx.PircBotX;
import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.PrivateMessageEvent;

import me.smc.sb.multi.Match;
import me.smc.sb.multi.Tournament;
import me.smc.sb.perm.Permissions;
import me.smc.sb.utils.Utils;

public class ListMatchesCommand extends IRCCommand{
	
	public ListMatchesCommand(){
		super("Lists all matches.",
			  "<tournament name> ",
			  Permissions.IRC_BOT_ADMIN,
			  "mplist");
	}
	
	@Override
	public void onCommand(MessageEvent<PircBotX> e, PrivateMessageEvent<PircBotX> pe, String discord, String[] args){
		if(!Utils.checkArguments(e, pe, discord, args, 1)) return;
		
		String tournamentName = "";
		
		for(int i = 0; i < args.length; i++) tournamentName += args[i] + " ";
		Tournament t = Tournament.getTournament(tournamentName.substring(0, tournamentName.length() - 1));
		
		if(t == null){Utils.info(e, pe, discord, "Invalid tournament!"); return;}
		
		String msg = "Matches in " + t.getName();
		if(discord != null) msg = "```" + msg + "\n";
		else msg += "=";
		
		for(Match match : t.getMatches()){
			msg += "#" + match.getMatchNum();
			
			if(match.getFirstTeam() != null)
				msg += " - " + match.getFirstTeam().getTeamName() + " vs";
			else msg += " -  vs";
			
			if(match.getSecondTeam() != null)
				msg += " " + match.getSecondTeam().getTeamName();
			else msg += "  ";
			
			if(match.getTime() != 0) msg += " at " + Utils.toDate(match.getTime()) + " UTC";
			
			if(discord != null) msg += "\n";
			else msg += "=";
		}
		
		if(discord == null)
			for(String part : msg.split("=")){
				if(part.isEmpty()) continue;
				Utils.info(e, pe, discord, part);
			}
		else Utils.info(e, pe, discord, msg + "```");
	}
	
}
