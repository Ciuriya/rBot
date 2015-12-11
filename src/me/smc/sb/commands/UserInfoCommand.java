package me.smc.sb.commands;

import me.itsghost.jdiscord.Role;
import me.itsghost.jdiscord.events.UserChatEvent;
import me.itsghost.jdiscord.message.MessageBuilder;
import me.itsghost.jdiscord.talkable.GroupUser;
import me.smc.sb.utils.Utils;

public class UserInfoCommand extends GlobalCommand{

	public UserInfoCommand(){
		super(null, 
			  " - Shows various info about the specified user", 
			  "{prefix}userinfo\nThis command shows information about the specified user.\n\n" +
			  "----------\nUsage\n----------\n{prefix}userinfo {username} - Shows various information about the specified user\n\n" + 
			  "----------\nAliases\n----------\nThere are no aliases.", 
			  false, 
			  "userinfo");
	}

	@Override
	public void onCommand(UserChatEvent e, String[] args){
		e.getMsg().deleteMessage();
		String user = "";
		for(String arg : args)
			user += " " + arg;
		user = user.substring(1);
		
		GroupUser gUser = e.getServer().getGroupUserByUsername(user);
		if(gUser == null) return;
		
		String roles = "";
		for(Role r : gUser.getRoles()) roles += " - " + r.getName();
		roles = roles.substring(3);
		MessageBuilder builder = new MessageBuilder();
		builder.addString("```User info for " + user + "\n")
			   .addString("User status: " + gUser.getUser().getOnlineStatus().name().toLowerCase() + "\n")
			   .addString("Playing (" + gUser.getUser().getGame() + ")\n")
		       .addString("User id: " + gUser.getUser().getId() + "\n")
		       .addString("Discriminator: " + gUser.getDiscriminator() + "\n")
		       .addString("Roles: " + roles + "\n")
		       .addString("Avatar: " + gUser.getUser().getAvatar() + "```");
		Utils.infoBypass(e.getGroup(), builder.build().getMessage());
	}

}
