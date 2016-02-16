package me.smc.sb.discordcommands;

import me.smc.sb.perm.Permissions;
import me.smc.sb.utils.Utils;
import net.dv8tion.jda.MessageBuilder;
import net.dv8tion.jda.entities.User;
import net.dv8tion.jda.events.message.MessageReceivedEvent;

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
	public void onCommand(MessageReceivedEvent e, String[] args){
		Utils.deleteMessage(e.getChannel(), e.getMessage());
		if(!Utils.checkArguments(e, args, 1)) return;
		
		String user = "";
		for(String arg : args)
			user += " " + arg;
		user = user.substring(1);
		
		MessageBuilder builder = new MessageBuilder();
		
		builder.appendString("```Permissions for " + user + "\n");
		
		User u = null;
		for(User gUser : e.getGuild().getUsers())
			if(gUser.getUsername().equalsIgnoreCase(user)){
				u = gUser;
				break;
			}
		
		if(u == null){
			Utils.error(e.getChannel(), e.getAuthor(), " Invalid user!");
			return;
		}
		
		for(Permissions perm : Permissions.values()){
			boolean allowed = e.isPrivate() ? Permissions.check(u, perm) : Permissions.hasPerm(u, e.getTextChannel(), perm);
			builder.appendString(perm.name() + " (" + allowed + ")\n");
		}
		
		builder.appendString("```");
		Utils.infoBypass(e.getChannel(), builder.build().getContent());
	}

}
