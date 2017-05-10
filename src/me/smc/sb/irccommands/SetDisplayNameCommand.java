package me.smc.sb.irccommands;

import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.PrivateMessageEvent;

import me.smc.sb.multi.Tournament;
import me.smc.sb.perm.Permissions;
import me.smc.sb.utils.Utils;

public class SetDisplayNameCommand extends IRCCommand{
	
	public SetDisplayNameCommand(){
		super("Sets the lobby display name of the tournament.",
			  "<tournament name> {display name} ",
			  Permissions.TOURNEY_ADMIN,
			  "setdisplayname");
	}

	@Override
	public String onCommand(MessageEvent e, PrivateMessageEvent pe, String discord, String[] args){
		String argCheck = Utils.checkArguments(args, 2);
		if(argCheck.length() > 0) return argCheck;
		
		String tournamentName = "";
		
		int o = 0;
		for(int i = 0; i < args.length; i++) 
			if(args[i].contains("{")){o = i; break;} 
			else tournamentName += args[i] + " ";
		
		Tournament t = Tournament.getTournament(tournamentName.substring(0, tournamentName.length() - 1));
		
		if(t == null) return "Invalid tournament!";
		
		String user = Utils.toUser(e, pe);
		
		if(t.isAdmin(user)){
			String displayName = "";
			
			for(int i = o; i < args.length; i++) 
				if(args[i].contains("}")){
					displayName += args[i].replace("}", "").replace("}", "") + " ";
					break;
				}else displayName += args[i].replace("{", "") + " ";
			
			displayName = displayName.replace("{", "").substring(0, displayName.length() - 2);
			t.setDisplayName(displayName);
			t.save(false);
			
			return "Set the tournament's display name to " + displayName + "!";
		}
		
		return "";
	}
	
}
