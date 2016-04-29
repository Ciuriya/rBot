package me.smc.sb.discordcommands;

import me.smc.sb.perm.Permissions;
import me.smc.sb.utils.Utils;
import net.dv8tion.jda.MessageHistory;
import net.dv8tion.jda.entities.Message;
import net.dv8tion.jda.entities.PrivateChannel;
import net.dv8tion.jda.entities.User;
import net.dv8tion.jda.events.message.MessageReceivedEvent;

public class CleanCommand extends GlobalCommand{

	public CleanCommand(){
		super(Permissions.MANAGE_MESSAGES, 
			  " - Cleans the last messages sent by everyone or a specific user", 
			  "{prefix}clean\nThis command cleans n amount of messages in the channel\n\n" +
			  "----------\nUsage\n----------\n{prefix}clean {amount} (@user) - Removes n amount of messages sent by everyone (or by user on mention)\n\n" + 
		      "----------\nAliases\n----------\nThere are no aliases.",  
			  false, 
			  "clean");
	}

	@Override
	public void onCommand(MessageReceivedEvent e, String[] args){
		Utils.deleteMessage(e.getChannel(), e.getMessage());
		if(!Utils.checkArguments(e, args, 1)) return;
		
		int amount = Integer.valueOf(args[0]);
		User cleanUser = null;
		
		if(args.length >= 2 && args[1].contains("@") && e.getTextChannel() != null)
			for(User u : e.getTextChannel().getUsers())
				if(u.getUsername().equalsIgnoreCase(args[1].substring(1))){
					cleanUser = u;
				}
		
		if(amount > 100) amount = 100;
		else if(amount < 1) amount = 1;
		
		MessageHistory history = null;
				
		if(e.getChannel() instanceof PrivateChannel)
			history = new MessageHistory(e.getPrivateChannel());
		else history = new MessageHistory(e.getTextChannel());
		
		int cleared = 0;
		
		for(Message message : history.retrieve(100)){
			if(cleanUser != null && message.getAuthor().getId().equalsIgnoreCase(cleanUser.getId())){
				message.deleteMessage();
				cleared++;
			}else if(cleanUser == null){
				message.deleteMessage();
				cleared++;
			}
			if(cleared >= amount) break;
		}
		
		Utils.info(e.getChannel(), "Cleared " + cleared + " messages!");
	}

}
