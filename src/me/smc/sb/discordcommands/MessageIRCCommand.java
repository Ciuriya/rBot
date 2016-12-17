package me.smc.sb.discordcommands;

import org.pircbotx.PircBotX;

import me.smc.sb.main.Main;
import me.smc.sb.perm.Permissions;
import me.smc.sb.utils.Utils;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

public class MessageIRCCommand extends GlobalCommand{

	public MessageIRCCommand(){
		super(Permissions.IRC_BOT_ADMIN, 
			  " - Send a message to an IRC user", 
			  "{prefix}msgIRC\nThis command sends messages through IRC.\n\n" +
			  "----------\nUsage\n----------\n{prefix}msgIRC (t or i) {user} {message} - Sends a message to the specified user\n\n" +
			  "----------\nAliases\n----------\nThere are no aliases.",  
			  true,
			  "msgIRC");
	}

	@Override
	public void onCommand(MessageReceivedEvent e, String[] args){
		Utils.deleteMessage(e.getChannel(), e.getMessage());
		if(!Utils.checkArguments(e, args, 3)) return;
		
		boolean twitch = args[0].equalsIgnoreCase("t") ? true : false;
		
		PircBotX bot = twitch ? Main.twitchBot : (args[0].equalsIgnoreCase("i") ? Main.ircBot : null);

		String message = "";
		for(int i = 2; i < args.length; i++)
			message += " " + args[i];
		
		message = message.substring(1);
		
		if(bot == null) Utils.info(e.getChannel(), "The bot object is null! Make sure to tell Smc!");
		else{	
			if(args[0].equalsIgnoreCase("t")) Main.twitchRegulator.sendMessage(args[1], message);
			else if(!args[1].startsWith("#") || Utils.verifyChannel(args[1])) Main.ircBot.sendIRC().message(args[1], message);
		}
	}
	
}
