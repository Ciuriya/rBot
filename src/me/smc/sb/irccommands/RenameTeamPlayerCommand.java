package me.smc.sb.irccommands;

import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.PrivateMessageEvent;

import me.smc.sb.multi.Player;
import me.smc.sb.multi.Team;
import me.smc.sb.multi.Tournament;
import me.smc.sb.perm.Permissions;
import me.smc.sb.utils.Utils;

public class RenameTeamPlayerCommand extends IRCCommand{
	
	public RenameTeamPlayerCommand(){
		super("Renames a player in a team.",
			  "<tournament name> {<team name>} {<old name>} {<new name} ",
			  Permissions.TOURNEY_ADMIN,
			  "renameplayer");
	}

	@Override
	public String onCommand(MessageEvent e, PrivateMessageEvent pe, String discord, String[] args){
		String argCheck = Utils.checkArguments(args, 4);
		if(argCheck.length() > 0) return argCheck;
		
		String validation = Utils.validateTournamentAndTeam(e, pe, discord, args);
		if(!validation.contains("|")) return validation;
		
		Tournament t = Tournament.getTournament(validation.split("\\|")[1]);
		Team team = t.getTeam(validation.split("\\|")[0]);
		
		String user = Utils.toUser(e, pe);
		
		if(t.isAdmin(user)){
			int occurence = 0;
			String oldName = "";
			String newName = "";
			
			for(int i = 0; i < args.length; i++){
				if(args[i].contains("{")){
					occurence++;
					
					if(occurence == 2){
						for(int o = i; o < args.length; o++){
							oldName += " " + args[o];
							
							if(args[o].contains("}")) break;
						}
						
						oldName = oldName.replace("{", "").replace("}", "").substring(1);
					}
					
					if(occurence == 3){
						for(int o = i; o < args.length; o++){
							newName += " " + args[o];
							
							if(args[o].contains("}")) break;
						}
						
						newName = newName.replace("{", "").replace("}", "").substring(1);
					}
				}
			}
			
			final String finalOldName = oldName;
			
			Player player = team.getPlayers().stream().filter(p -> p.getName().equalsIgnoreCase(finalOldName)).findFirst().orElse(null);
			if(player != null && newName.length() > 0){
				player.setName(newName);
				team.save(false);
				
				return "Changed " + oldName + "'s name to " + newName + "!";
			}
		}
		
		return "";
	}
}
