package me.smc.sb.irccommands;

import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.PrivateMessageEvent;

import me.smc.sb.multi.Tournament;
import me.smc.sb.utils.Utils;

public class CurrentMapCommand extends IRCCommand{

	public CurrentMapCommand(){
		super("Prints the current map of the currently streamed twitch game.",
			  " ",
			  null,
			  true,
			  "map");
	}

	@Override
	public String onCommand(MessageEvent e, PrivateMessageEvent pe, String discord, String[] args){
		if(Utils.isTwitch(e))
			Utils.info(e, pe, discord, "https://osu.ppy.sh/b/" + Tournament.getCurrentMap(e.getChannel().getName().replace("#", "")));
		
		return "";
	}
	
}
