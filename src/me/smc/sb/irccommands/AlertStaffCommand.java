package me.smc.sb.irccommands;

import java.util.ArrayList;
import java.util.List;

import org.pircbotx.PircBotX;
import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.PrivateMessageEvent;

import me.smc.sb.main.Main;
import me.smc.sb.multi.Game;
import me.smc.sb.multi.Tournament;
import me.smc.sb.utils.Utils;
import net.dv8tion.jda.core.entities.TextChannel;

public class AlertStaffCommand extends IRCCommand{

	public static List<Game> gamesAllowedToAlert;
	
	public AlertStaffCommand(){
		super("Sends a message to tournament staff.",
			  "<message> ",
			  null,
			  "alert");
		
		gamesAllowedToAlert = new ArrayList<>();
	}
	
	@Override
	public String onCommand(MessageEvent<PircBotX> e, PrivateMessageEvent<PircBotX> pe, String discord, String[] args){
		if(e == null || discord != null || pe != null) return "You cannot alert staff in here!";
		
		String argCheck = Utils.checkArguments(args, 1);
		if(argCheck.length() > 0) return argCheck;
		
		String userName = Utils.toUser(e, pe);
		
		String msg = "";
		
		for(String arg : args) msg += arg + " ";
		
		msg = msg.substring(0, msg.length() - 1);
		
		if(!gamesAllowedToAlert.isEmpty())
			for(Game game : gamesAllowedToAlert)
				if(game.verifyPlayer(userName.replaceAll(" ", "_"))){
					Tournament t = game.match.getTournament();
					
					TextChannel channel = Main.api.getTextChannelById(t.getAlertDiscord());
					String mention = channel.getGuild().getRolesByName(t.getAlertMessage(), true).get(0).getAsMention();
					
					if(Main.api.getTextChannelById(t.getAlertDiscord()) != null)
						Utils.infoBypass(channel, mention + 
												  "\nGame #" + game.getMpNum() + " (match #" + game.match.getMatchNum() + ")\n" + 
												  userName + "\n" + msg);
						
					return "";
				}
		
		return "You cannot alert staff right now.";
	}
	
}
