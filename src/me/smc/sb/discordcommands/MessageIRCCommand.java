package me.smc.sb.discordcommands;

import java.util.logging.Level;

import me.smc.sb.main.Main;
import me.smc.sb.perm.Permissions;
import me.smc.sb.utils.Log;
import me.smc.sb.utils.Utils;
import net.dv8tion.jda.events.message.MessageReceivedEvent;

public class MessageIRCCommand extends GlobalCommand{

	public MessageIRCCommand(){
		super(Permissions.IRC_BOT_ADMIN, 
			  " - Send a message to an IRC user", 
			  "{prefix}msgIRC\nThis command sends messages through IRC.\n\n" +
			  "----------\nUsage\n----------\n{prefix}msgIRC {user} {message} - Sends a message to the specified user\n\n" +
			  "----------\nAliases\n----------\nThere are no aliases.",  
			  true,
			  "msgIRC");
	}

	@Override
	public void onCommand(MessageReceivedEvent e, String[] args){
		Utils.deleteMessage(e.getChannel(), e.getMessage());
		if(!Utils.checkArguments(e, args, 2)) return;
		
		String message = "";
		for(int i = 1; i < args.length; i++)
			message += " " + args[i];
		message = message.substring(1);
		
		if(Main.ircBot == null) Utils.info(e.getChannel(), "The IRC bot object is null! Make sure to tell Smc!");
		else{
			try{
				Main.ircBot.sendIRC().joinChannel(args[0]);
			}catch(Exception ex){
				Log.logger.log(Level.INFO, "Could not join channel " + args[0]);
			}
			if(!args[0].startsWith("#") || Utils.verifyChannel(args[0])) Main.ircBot.sendIRC().message(args[0], message);
		}
	}
	
}
