package me.smc.sb.discordcommands;

import me.itsghost.jdiscord.events.UserChatEvent;
import me.smc.sb.missingapi.MessageHistory;
import me.smc.sb.perm.Permissions;
import me.smc.sb.utils.Utils;

public class CleanCommand extends GlobalCommand{

	public CleanCommand(){
		super(Permissions.MANAGE_MESSAGES, 
			  " - Cleans the last messages sent by the bot, or everyone", 
			  "{prefix}clean\nThis command cleans n amount of messages in the channel\n\n" +
			  "----------\nUsage\n----------\n{prefix}clean {amount} (all) - Removes n amount of messages sent by the bot (or everyone if all)\n\n" + 
		      "----------\nAliases\n----------\nThere are no aliases.",  
			  false, 
			  "clean");
	}

	@Override
	public void onCommand(UserChatEvent e, String[] args){
		e.getMsg().deleteMessage();
		if(!Utils.checkArguments(e, args, 1)) return;
		int amount = Integer.valueOf(args[0]);
		boolean force = false;
		if(args.length >= 2 && args[1].equalsIgnoreCase("all")) force = true;
		int cleared = MessageHistory.getHistory(e.getGroup().getId()).deleteLastMessages(amount, force);
		Utils.info(e.getGroup(), "Cleared " + cleared + " messages!");
	}

}
