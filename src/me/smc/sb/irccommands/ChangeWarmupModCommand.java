package me.smc.sb.irccommands;

import java.util.ArrayList;
import java.util.List;

import org.pircbotx.PircBotX;
import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.PrivateMessageEvent;

import me.smc.sb.multi.Game;
import me.smc.sb.utils.Utils;

public class ChangeWarmupModCommand extends IRCCommand{

	public static List<Game> gamesAllowedToChangeMod;
	
	public ChangeWarmupModCommand(){
		super("Allows teams to add either DT or HT to warmups.",
			  "<DT/HT> ",
			  null,
			  "changemod");
		gamesAllowedToChangeMod = new ArrayList<>();
	}

	@Override
	public String onCommand(MessageEvent<PircBotX> e, PrivateMessageEvent<PircBotX> pe, String discord, String[] args) {
		if(e == null || discord != null || pe != null) return "You cannot change mods in here!";
		
		String argCheck = Utils.checkArguments(args, 1);
		if(argCheck.length() > 0) return argCheck;
		
		if(!args[0].equalsIgnoreCase("DT") && !args[0].equalsIgnoreCase("HT") && !args[0].equalsIgnoreCase("NM") &&
			!args[0].equalsIgnoreCase("NC"))
			return "You can only choose DT, NC, NM or HT!";
		
		String userName = Utils.toUser(e, pe);
		
		if(!gamesAllowedToChangeMod.isEmpty())
			for(Game game : gamesAllowedToChangeMod)
				if(game.verifyPlayer(userName.replaceAll(" ", "_"))){
					game.acceptWarmupModChange(userName.replaceAll(" ", "_"), args[0]);
					return "";
				}
		
		return "Could not change mods!";
	}
	
}
