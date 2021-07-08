package me.smc.sb.discordcommands;

import me.smc.sb.utils.Utils;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

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
		if(args.length == 0) return;
		
		User user = e.getJDA().retrieveUserById(args[0]).complete();
		
		if(user == null){
			Utils.infoBypass(e.getChannel(), "User not found");
			return;
		}
		
		Utils.infoBypass(e.getChannel(), args[0] + "'s name is " + user.getName());
	}
}