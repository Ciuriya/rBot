package me.smc.sb.irccommands;

import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.PrivateMessageEvent;

import me.smc.sb.multi.Tournament;
import me.smc.sb.perm.Permissions;
import me.smc.sb.utils.Utils;

public class SetAlertMessageCommand extends IRCCommand{

	public SetAlertMessageCommand(){
		super("Sets the mention message to alert staff with.",
			  "{<tournament name>} <message> ",
			  Permissions.TOURNEY_ADMIN,
			  "setalertmessage");
	}
	
	@Override
	public String onCommand(MessageEvent e, PrivateMessageEvent pe, String discord, String[] args){
		String argCheck = Utils.checkArguments(args, 2);
		if(argCheck.length() > 0) return argCheck;
		
		String tournamentName = "";
		int i = 0;
		
		for(i = 0; i < args.length; i++){
			tournamentName += args[i] + " ";
			
			if(args[i].endsWith("}")) break;
		}
		
		if(i == args.length - 1) return "There is no message!";
		
		i++;
		
		String message = "";
		
		for(; i < args.length; i++){
			message += args[i] + " ";
		}
		
		message = message.substring(0, message.length() - 1);
		
		tournamentName = tournamentName.substring(1);
		tournamentName = tournamentName.substring(0, tournamentName.length() - 2);
		
		Tournament t = Tournament.getTournament(tournamentName);
		if(t == null) return "The tournament does not exist!";
		
		String user = Utils.toUser(e, pe);
		
		if(t.isAdmin(user)){
			t.setAlertMessage(message);
			t.save(false);
			
			return "The alert message was set!";
		}
		
		return "";
	}
	
}
