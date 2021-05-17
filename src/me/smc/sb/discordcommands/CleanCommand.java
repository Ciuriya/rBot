package me.smc.sb.discordcommands;

import java.util.ArrayList;
import java.util.List;

import me.smc.sb.perm.Permissions;
import me.smc.sb.utils.Utils;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageHistory;
import net.dv8tion.jda.api.entities.PrivateChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class CleanCommand extends GlobalCommand{

	public CleanCommand(){
		super(Permissions.MANAGE_MESSAGES, 
			  " - Cleans the last messages sent by everyone or a specific user", 
			  "{prefix}clean\nThis command cleans n amount of messages in the channel\n\n" +
			  "----------\nUsage\n----------\n{prefix}clean {amount} (@user) - Removes n amount of messages sent by everyone (or by user on mention)\n\n" + 
		      "----------\nAliases\n----------\nThere are no aliases.",  
			  true, 
			  "clean");
	}

	@Override
	public void onCommand(MessageReceivedEvent e, String[] args){
		if(!Utils.checkArguments(e, args, 1)) return;
		
		int amount = Integer.valueOf(args[0]);
		User cleanUser = null;
		
		if(args.length >= 2) {
			cleanUser = e.getJDA().retrieveUserById(e.getMessage().getContentRaw().split(" ")[2].replace("<", "").replace("@", "").replace(">", "").replace("!", "")).complete();
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
			
			if((cleanUser != null && message.getAuthor().getId().equalsIgnoreCase(cleanUser.getId())) || cleanUser == null){
				cleared++;
				
				if(e.getChannel() instanceof PrivateChannel){
					message.delete().complete();
					Utils.sleep(350);
				}else toDelete.add(message);
			}
			
			if(messageList.size() == 0 && cleared < amount)
				messageList = history.retrievePast(100).complete();
		}
		
		if(toDelete.size() == 1) 
			toDelete.get(0).delete().complete();
		else if(!toDelete.isEmpty()) 
			e.getTextChannel().deleteMessages(toDelete).complete();
		
		Utils.info(e.getChannel(), "Cleared " + cleared + " messages!");
	}

}
