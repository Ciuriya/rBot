package me.smc.sb.irccommands;

import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.PrivateMessageEvent;

import me.smc.sb.perm.Permissions;
import me.smc.sb.tourney.Tournament;
import me.smc.sb.utils.Utils;

public class SetScoreV2Command extends IRCCommand{

	public SetScoreV2Command(){
		super("Sets whether or not scoreV2 will be used.",
			  "<tournament name> <true/false> ",
			  Permissions.TOURNEY_ADMIN,
			  "setscoring");
	}

	@Override
	public String onCommand(MessageEvent e, PrivateMessageEvent pe, String discord, String[] args){
		String argCheck = Utils.checkArguments(args, 2);
		if(argCheck.length() > 0) return argCheck;
		
		String tournamentName = "";
		
		for(int i = 0; i < args.length - 1; i++) tournamentName += args[i] + " ";
		Tournament t = Tournament.getTournament(tournamentName.substring(0, tournamentName.length() - 1));
		
		if(t == null) return "Invalid tournament!";
		
		String user = Utils.toUser(e, pe);
		
		if(t.isAdmin(user)){
			t.set("scoreV2", Boolean.parseBoolean(args[args.length - 1]));
			t.save(false);
			
			return "Set the tournament's scoring to scoreV" + (t.getBool("scoreV2") ? "2" : "1") + "!";
		}
		
		return "";
	}
	
}
