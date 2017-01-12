package me.smc.sb.discordcommands;

import java.util.ArrayList;
import java.util.List;

import me.smc.sb.perm.Permissions;
import me.smc.sb.utils.Utils;
import net.dv8tion.jda.core.MessageHistory;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.PrivateChannel;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

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
		e.getMessage().deleteMessage().complete();
		if(!Utils.checkArguments(e, args, 1)) return;
		
		int amount = Integer.valueOf(args[0]);
		Member cleanUser = null;
		
		if(args.length >= 2 && args[1].contains("@") && e.getTextChannel() != null)
			for(Member m : e.getTextChannel().getMembers())
				if(m.getUser().getName().equalsIgnoreCase(args[1].substring(1))){
					cleanUser = m;
				}
		
		if(amount > 100) amount = 100;
		else if(amount < 1) amount = 1;
		
		MessageHistory history = null;
				
		if(e.getChannel() instanceof PrivateChannel)
			history = new MessageHistory(e.getPrivateChannel());
		else history = new MessageHistory(e.getTextChannel());
		
		int cleared = 0;
		
		List<Message> messageList;
		
		messageList = history.retrievePast(100).complete();
		
		List<Message> toDelete = new ArrayList<>();
		
		while(cleared < amount && messageList.size() > 0){
			Message message = messageList.get(0);
			
			messageList.remove(0);
			
			if((cleanUser != null && message.getAuthor().getId().equalsIgnoreCase(cleanUser.getUser().getId())) || cleanUser == null){
				cleared++;
				
				if(e.getChannel() instanceof PrivateChannel){
					message.deleteMessage().complete();
					Utils.sleep(350);
				}else toDelete.add(message);
			}
			
			if(messageList.size() == 0 && cleared < amount)
				messageList = history.retrievePast(100).complete();
		}
		
		if(toDelete.size() == 1) 
			toDelete.get(0).deleteMessage().complete();
		else if(!toDelete.isEmpty()) 
			e.getTextChannel().deleteMessages(toDelete).complete();
		
		Utils.info(e.getChannel(), "Cleared " + cleared + " messages!");
	}

}
