package me.smc.sb.discordcommands;

import me.smc.sb.utils.Utils;
import net.dv8tion.jda.entities.Role;
import net.dv8tion.jda.entities.User;
import net.dv8tion.jda.events.message.MessageReceivedEvent;

public class UserInfoCommand extends GlobalCommand{

	public UserInfoCommand(){
		super(null, 
			  " - Shows various info about the specified user", 
			  "{prefix}userinfo\nThis command shows information about the specified user.\n\n" +
			  "----------\nUsage\n----------\n{prefix}userinfo {mentioned user} - Shows various information about the specified user\n\n" + 
			  "----------\nAliases\n----------\nThere are no aliases.", 
			  false, 
			  "userinfo");
	}

	@Override
	public void onCommand(MessageReceivedEvent e, String[] args){
		Utils.deleteMessage(e.getChannel(), e.getMessage());
		
		User user = e.getMessage().getMentionedUsers().get(0);
		
		if(user == null){
			Utils.error(e.getChannel(), e.getAuthor(), " Invalid user!");
			return;
		}
		
		String roles = "";
		for(Role r : e.getGuild().getRolesForUser(user)) roles += " - " + r.getName();
		roles = roles.substring(3);
		
		StringBuilder builder = new StringBuilder();
		builder.append("```User info for " + user.getUsername() + "\n")
			   .append("User status: " + user.getOnlineStatus().name().toLowerCase() + "\n")
			   .append("Playing (" + user.getCurrentGame() + ")\n")
		       .append("User id: " + user.getId() + "\n")
		       .append("Discriminator: " + user.getDiscriminator() + "\n")
		       .append("Roles: " + roles + "\n")
		       .append("Avatar: " + user.getAvatarUrl() + "```");
		Utils.infoBypass(e.getChannel(), builder.toString());
	}

}
