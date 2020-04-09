package me.smc.sb.discordcommands;

import me.smc.sb.perm.Permissions;
import me.smc.sb.utils.Utils;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class MsgUserCommand extends GlobalCommand{

	public MsgUserCommand(){
		super(Permissions.BOT_ADMIN, 
			  " - Send a message to a discord user", 
			  "{prefix}msgUser\nThis command sends messages through discord to other users.\n\n" +
			  "----------\nUsage\n----------\n{prefix}msgUser {userId} {message} - Sends a message to the specified user\n\n" +
			  "----------\nAliases\n----------\nThere are no aliases.",  
			  true,
			  "msgUser");
	}

	@Override
	public void onCommand(MessageReceivedEvent e, String[] args){
		Utils.deleteMessage(e.getChannel(), e.getMessage());
		if(!Utils.checkArguments(e, args, 2)) return;

		String userId = args[0];
		String message = "";
		User user = e.getJDA().retrieveUserById(userId).complete();
		
		if(user == null){
			Utils.info(e.getChannel(), "Invalid user!");
			
			return;
		}
		
		for(int i = 1; i < args.length; i++)
			message += " " + args[i];
		
		message = message.substring(1);
		
		Utils.infoBypass(user.openPrivateChannel().complete(), message);
	}
	
}
