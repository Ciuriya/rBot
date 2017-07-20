package me.smc.sb.irccommands;

import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.PrivateMessageEvent;

import me.smc.sb.tourney.Game;
import me.smc.sb.tourney.TwitchHandler;
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
	public String onCommand(MessageEvent e, PrivateMessageEvent pe, String discord, String[] args){
		if(Utils.isTwitch(e)){
			Game streamedGame = TwitchHandler.get(e.getChannel().getName().replace("#", "")).getStreamed();
			String score = streamedGame.getFirstTeam().getTeam().getTeamName() + " " + streamedGame.getFirstTeam().getPoints() + " | " +
						   streamedGame.getSecondTeam().getPoints() + " " + streamedGame.getSecondTeam().getTeam().getTeamName() + " BO" + 
						   streamedGame.match.getBestOf();
			
			Utils.info(e, pe, discord, score);
		}
		
		return "";
	}
	
}
