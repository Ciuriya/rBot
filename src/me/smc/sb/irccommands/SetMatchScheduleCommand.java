package me.smc.sb.irccommands;

import org.pircbotx.PircBotX;
import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.PrivateMessageEvent;

import me.smc.sb.multi.Tournament;
import me.smc.sb.perm.Permissions;
import me.smc.sb.utils.Utils;

public class SetMatchScheduleCommand extends IRCCommand{

	public SetMatchScheduleCommand(){
		super("Sets a match's scheduled time in UTC.",
			  "<tournament name> <match num> <yyyy> <MM> <dd> <HH>",
			  Permissions.IRC_BOT_ADMIN,
			  "mpsetschedule");
	}

	@Override
	public void onCommand(MessageEvent<PircBotX> e, PrivateMessageEvent<PircBotX> pe, String discord, String[] args){
		if(!Utils.checkArguments(e, pe, discord, args, 6)) return;
		
		String tournamentName = "";
		
		for(int i = 0; i < args.length - 5; i++) tournamentName += args[i] + " ";
		Tournament t = Tournament.getTournament(tournamentName.substring(0, tournamentName.length() - 1));
		
		if(t == null){Utils.info(e, pe, discord, "Invalid tournament!"); return;}
		if(Utils.stringToInt(args[args.length - 5]) == -1){Utils.info(e, pe, discord, "Match number needs to be a number!"); return;}
		if(t.getMatch(Utils.stringToInt(args[args.length - 5])) == null){Utils.info(e, pe, discord, "The match is invalid!"); return;}
		
		String date = "";
		for(int i = args.length - 4; i < args.length; i++)
			date += args[i] + " ";
		long time = Utils.toTime(date.substring(0, date.length() - 1));
		if(time == -1 || time < Utils.getCurrentTimeUTC()){Utils.info(e, pe, discord, "This time is either in the past or invalid!"); return;}
		
		t.getMatch(Utils.stringToInt(args[args.length - 5])).setTime(time);
		t.getMatch(Utils.stringToInt(args[args.length - 5])).save(false);
		
		Utils.info(e, pe, discord, "Set match #" + args[args.length - 5] + " to " + date);
	}
	
}
