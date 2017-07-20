package me.smc.sb.irccommands;

import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.PrivateMessageEvent;

import me.smc.sb.tourney.Game;
import me.smc.sb.tourney.TwitchHandler;
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
		if(Utils.isTwitch(e)){
			Game streamedGame = TwitchHandler.get(e.getChannel().getName().replace("#", "")).getStreamed();
			Utils.info(e, pe, discord, "https://osu.ppy.sh/b/" + streamedGame.getSelectionManager().getMap().getBeatmapID());
		}
		
		return "";
	}
	
}
