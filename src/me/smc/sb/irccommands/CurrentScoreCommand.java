package me.smc.sb.irccommands;

import org.pircbotx.PircBotX;
import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.PrivateMessageEvent;

import me.smc.sb.multi.Tournament;
import me.smc.sb.utils.Utils;

public class CurrentScoreCommand extends IRCCommand{

	public CurrentScoreCommand(){
		super("Prints the current score of the twitch streamed game.",
			  " ",
			  null,
			  true,
			  "score");
	}

	@Override
	public String onCommand(MessageEvent<PircBotX> e, PrivateMessageEvent<PircBotX> pe, String discord, String[] args){
		if(Utils.isTwitch(e))
			Utils.info(e, pe, discord, Tournament.getCurrentScore(e.getChannel().getName().replace("#", "")));
		
		return "";
	}
	
}
