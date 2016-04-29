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
	public String onCommand(MessageEvent<PircBotX> e, PrivateMessageEvent<PircBotX> pe, String discord, String[] args){
		String argCheck = Utils.checkArguments(args, 1);
		if(argCheck.length() > 0) return argCheck;
		
		String tournamentName = "";
		
		for(int i = 0; i < args.length; i++) tournamentName += args[i] + " ";
		Tournament t = Tournament.getTournament(tournamentName.substring(0, tournamentName.length() - 1));
		
		if(t == null) return "Invalid tournament!";
		
		String msg = "Matches in " + t.getName();
		if(discord != null) msg = "```" + msg + "\n";
		else msg += "=";
		
		for(Match match : t.getMatches()){
			msg += "\n#" + match.getMatchNum();
			
			if(match.getFirstTeam() != null)
				msg += " - " + match.getFirstTeam().getTeamName() + " vs";
			else msg += " -  vs";
			
			if(match.getSecondTeam() != null)
				msg += " " + match.getSecondTeam().getTeamName();
			else msg += "  ";
			
			if(match.getTime() != 0) msg += " - " + Utils.toDate(match.getTime()) + " UTC";
			
			msg += "\n" + match.getPlayers() + " players - BO" + 
			       match.getBestOf() + " - MapPool #" + 
				   (match.getMapPool() != null ? 
				   match.getMapPool().getPoolNum() : -1);
			
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
		
		return "";
	}
	
}
