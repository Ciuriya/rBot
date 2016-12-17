package me.smc.sb.discordcommands;

import me.smc.sb.perm.Permissions;
import me.smc.sb.utils.Utils;
import net.dv8tion.jda.core.entities.ChannelType;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

public class ListPermsCommand extends GlobalCommand{

	public ListPermsCommand(){
		super(Permissions.MANAGE_ROLES, 
			  " - Displays the specified user's permissions",
			  "{prefix}listperms\nThis command lists all of the specified user's permissions\n\n" +
			  "----------\nUsage\n----------\n{prefix}listperms {mentioned user}- Shows all permissions of said user\n\n" + 
			  "----------\nAliases\n----------\nThere are no aliases.", 
			  false, 
			  "listperms");
	}

	@Override
	public void onCommand(MessageReceivedEvent e, String[] args){
		Utils.deleteMessage(e.getChannel(), e.getMessage());
		if(!Utils.checkArguments(e, args, 1)) return;
		
		User user = e.getMessage().getMentionedUsers().get(0);
		
		if(user == null){
			Utils.error(e.getChannel(), e.getAuthor(), " Invalid user!");
			return;
		}
		
		StringBuilder builder = new StringBuilder();
		
		builder.append("```Permissions for " + user.getName() + "\n");
		
		for(Permissions perm : Permissions.values()){
			boolean allowed = e.isFromType(ChannelType.PRIVATE) ? Permissions.check(user, perm) : Permissions.hasPerm(user, e.getTextChannel(), perm);
			builder.append(perm.name() + " (" + allowed + ")\n");
		}
		
		builder.append("```");
		Utils.infoBypass(e.getChannel(), builder.toString());
	}

}
