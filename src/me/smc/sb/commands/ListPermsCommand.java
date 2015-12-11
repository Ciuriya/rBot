package me.smc.sb.commands;

import me.itsghost.jdiscord.events.UserChatEvent;
import me.itsghost.jdiscord.message.MessageBuilder;
import me.smc.sb.perm.Permissions;
import me.smc.sb.utils.Utils;

public class ListPermsCommand extends GlobalCommand{

	public ListPermsCommand(){
		super(Permissions.MANAGE_ROLES, 
			  " - Displays the specified user's permissions",
			  "{prefix}listperms\nThis command lists all of the specified user's permissions\n\n" +
			  "----------\nUsage\n----------\n{prefix}listperms {username}- Shows all permissions of said user\n\n" + 
			  "----------\nAliases\n----------\nThere are no aliases.", 
			  false, 
			  "listperms");
	}

	@Override
	public void onCommand(UserChatEvent e, String[] args){
		e.getMsg().deleteMessage();
		if(!Utils.checkArguments(e, args, 1)) return;
		String user = "";
		for(String arg : args)
			user += " " + arg;
		user = user.substring(1);
		MessageBuilder builder = new MessageBuilder();
		builder.addString("```Permissions for " + user + "\n");
		for(Permissions perm : Permissions.values())
			builder.addString(perm.name() + " (" + Permissions.hasPerm(e.getServer().getGroupUserByUsername(user), perm) + ")\n");
		builder.addString("```");
		Utils.infoBypass(e.getGroup(), builder.build().getMessage());
	}

}
