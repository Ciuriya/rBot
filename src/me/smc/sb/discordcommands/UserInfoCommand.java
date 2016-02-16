package me.smc.sb.discordcommands;

import me.smc.sb.utils.Utils;
import net.dv8tion.jda.MessageBuilder;
import net.dv8tion.jda.entities.Role;
import net.dv8tion.jda.entities.User;
import net.dv8tion.jda.events.message.MessageReceivedEvent;

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
	public void onCommand(MessageReceivedEvent e, String[] args){
		Utils.deleteMessage(e.getChannel(), e.getMessage());
		
		String user = "";
		for(String arg : args)
			user += " " + arg;
		user = user.substring(1);
		
		User gUser = null;
		for(User u : e.getGuild().getUsers())
			if(u.getUsername().equalsIgnoreCase(user)){
				gUser = u;
				break;
			}
		
		if(gUser == null) Utils.error(e.getChannel(), e.getAuthor(), " Invalid user!");
		
		String roles = "";
		for(Role r : e.getGuild().getRolesForUser(gUser)) roles += " - " + r.getName();
		roles = roles.substring(3);
		
		MessageBuilder builder = new MessageBuilder();
		builder.appendString("```User info for " + user + "\n")
			   .appendString("User status: " + gUser.getOnlineStatus().name().toLowerCase() + "\n")
			   .appendString("Playing (" + gUser.getCurrentGame() + ")\n")
		       .appendString("User id: " + gUser.getId() + "\n")
		       .appendString("Discriminator: " + gUser.getDiscriminator() + "\n")
		       .appendString("Roles: " + roles + "\n")
		       .appendString("Avatar: " + gUser.getAvatarUrl() + "```");
		Utils.infoBypass(e.getChannel(), builder.build().getContent());
	}

}
