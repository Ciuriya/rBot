package me.smc.sb.discordcommands;

import me.smc.sb.utils.Utils;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

public class IdToUserCommand extends GlobalCommand{

	public IdToUserCommand(){
		super(null, 
			  " - Converts discord ID to name", 
			  "{prefix}idconvert\nThis command converts discord IDs to name\n\n" +
			  "----------\nUsage\n----------\n{prefix}idconvert {id} - Shows the discord name of that id\n\n" + 
			  "----------\nAliases\n----------\nThere are no aliases.", 
			  false, 
			  "idconvert");
	}

	@Override
	public void onCommand(MessageReceivedEvent e, String[] args){
		Utils.deleteMessage(e.getChannel(), e.getMessage());
		
		if(args.length == 0) return;
		
		User user = e.getJDA().getUserById(args[0]);
		
		if(user == null){
			Utils.infoBypass(e.getChannel(), "User not found");
			return;
		}
		
		Utils.infoBypass(e.getChannel(), args[0] + "'s name is " + user.getName());
	}
}
