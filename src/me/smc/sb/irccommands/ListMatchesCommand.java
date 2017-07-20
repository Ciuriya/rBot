package me.smc.sb.irccommands;

import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.PrivateMessageEvent;

import me.smc.sb.perm.Permissions;
import me.smc.sb.tourney.Match;
import me.smc.sb.tourney.Tournament;
import me.smc.sb.utils.Utils;

public class ListMatchesCommand extends IRCCommand{
	
	public ListMatchesCommand(){
		super("Lists all matches.",
			  "<tournament name> ",
			  Permissions.TOURNEY_ADMIN,
			  "mplist");
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
			String msg = "Matches in " + t.get("name");
			if(discord != null) msg = "```" + msg + "\n";
			else msg += "=";
			
			for(Match match : Match.getMatches(t)){
				msg += "\n#" + match.getMatchNum();
				
				if(match.getFirstTeam() != null)
					msg += " - " + match.getFirstTeam().getTeamName() + " vs";
				else msg += " -  vs";
				
				if(match.getSecondTeam() != null)
					msg += " " + match.getSecondTeam().getTeamName();
				else msg += "  ";
				
				if(match.getTime() != 0) msg += " - " + Utils.toDate(match.getTime()) + " UTC";
				
				msg += "\n" + match.getMatchSize() + " players - BO" + 
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
			}else{
				msg += "```";
				
				if(msg.length() > 2000){
					int max = (int) Math.ceil((double) msg.length() / 2000.0);
					
					for(int i = 0; i < max; i++){
						String message = "";
						
						if(i != 0) message = "```";
						
						if(i != max - 1) message += msg.substring(i * 1990, (i + 1) * 1990) + "```";
						else message += msg.substring(i * 1990, msg.length());
						
						Utils.info(e, pe, discord, message);
					}
				}else Utils.info(e, pe, discord, msg);
			}
		}
		
		return "";
	}
	
}
