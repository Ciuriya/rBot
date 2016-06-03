package me.smc.sb.irccommands;

import org.pircbotx.PircBotX;
import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.PrivateMessageEvent;

import me.smc.sb.multi.Tournament;
import me.smc.sb.utils.Utils;

public class MPLinkCommand extends IRCCommand{

	public MPLinkCommand(){
		super("Prints the mp link of the currently streamed twitch game.",
			  " ",
			  null,
			  true,
			  "mp");
	}

	@Override
	public String onCommand(MessageEvent<PircBotX> e, PrivateMessageEvent<PircBotX> pe, String discord, String[] args){
		if(Utils.isTwitch(e))
			Utils.info(e, pe, discord, "The MP link is " + Tournament.getCurrentMPLink(e.getChannel().getName().replace("#", "")));
		
		return "";
	}
	
}
