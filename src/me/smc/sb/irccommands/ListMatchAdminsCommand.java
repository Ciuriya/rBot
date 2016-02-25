package me.smc.sb.irccommands;

import org.pircbotx.PircBotX;
import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.PrivateMessageEvent;

import me.smc.sb.main.Main;
import me.smc.sb.multi.Match;
import me.smc.sb.multi.Tournament;
import me.smc.sb.perm.Permissions;
import me.smc.sb.utils.Utils;

public class ListMatchAdminsCommand extends IRCCommand{

	public ListMatchAdminsCommand(){
		super("Lists all admins in the match.",
			  "<tournament name> <match number> ",
			  Permissions.IRC_BOT_ADMIN,
			  "mplistadmins");
	}

	@Override
	public void onCommand(MessageEvent<PircBotX> e, PrivateMessageEvent<PircBotX> pe, String discord, String[] args){
		if(!Utils.checkArguments(e, pe, discord, args, 2)) return;
		
		String tournamentName = "";
		
		for(int i = 0; i < args.length - 1; i++) tournamentName += args[i] + " ";
		Tournament t = Tournament.getTournament(tournamentName.substring(0, tournamentName.length() - 1));
		
		if(t == null){Utils.info(e, pe, discord, "Invalid tournament!"); return;}
		if(Utils.stringToInt(args[args.length - 1]) == -1){Utils.info(e, pe, discord, "Match number needs to be a number!"); return;}
		
		Match match = t.getMatch(Utils.stringToInt(args[args.length - 1]));
		
		String admins = "";
		
		if(!match.getMatchAdmins().isEmpty())
			for(String admin : match.getMatchAdmins()){
				if(admin.length() == 17) admins += admin + " (" + Main.api.getUserById(admin).getUsername() + "), ";
				else admins += admin + " ";
			}
		
		if(admins.length() > 0){
			admins = admins.substring(0, admins.length() - 2);
			Utils.info(e, pe, discord, "Administrators for match #" + match.getMatchNum() + " in tournament " + tournamentName + ": " + admins);
		}
	}
	
}
