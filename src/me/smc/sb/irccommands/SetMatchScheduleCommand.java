package me.smc.sb.irccommands;

import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.PrivateMessageEvent;

import me.smc.sb.multi.Tournament;
import me.smc.sb.perm.Permissions;
import me.smc.sb.utils.Utils;

public class SetMatchScheduleCommand extends IRCCommand{

	public SetMatchScheduleCommand(){
		super("Sets a match's scheduled time in UTC.",
			  "<tournament name> <match num> <yyyy> <MM> <dd> <HH> <mm> ",
			  Permissions.TOURNEY_ADMIN,
			  "mpsetschedule");
	}

	@Override
	public String onCommand(MessageEvent e, PrivateMessageEvent pe, String discord, String[] args){
		String argCheck = Utils.checkArguments(args, 7);
		if(argCheck.length() > 0) return argCheck;
		
		String tournamentName = "";
		
		for(int i = 0; i < args.length - 6; i++) tournamentName += args[i] + " ";
		Tournament t = Tournament.getTournament(tournamentName.substring(0, tournamentName.length() - 1));
		
		if(t == null) return "Invalid tournament!";
		if(Utils.stringToInt(args[args.length - 6]) == -1) return "Match number needs to be a number!";
		if(t.getMatch(Utils.stringToInt(args[args.length - 6])) == null) return "The match is invalid!";
		
		String user = Utils.toUser(e, pe);
		
		if(t.isAdmin(user)){
			String date = "";
			for(int i = args.length - 5; i < args.length; i++)
				date += args[i] + " ";
			long time = Utils.toTime(date.substring(0, date.length() - 1));
			
			if(time == -1 || time < Utils.getCurrentTimeUTC()) return "This time is either in the past or invalid!";
			
			t.getMatch(Utils.stringToInt(args[args.length - 6])).setTime(time);
			t.getMatch(Utils.stringToInt(args[args.length - 6])).save(false);
			
			return "Set match #" + args[args.length - 6] + " to " + date;
		}
		
		return "";
	}
	
}
