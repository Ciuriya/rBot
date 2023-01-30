package me.smc.sb.discordcommands;

import java.util.ArrayList;
import java.util.List;

import me.smc.sb.utils.Utils;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

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
		User user = e.getMessage().getMentions().getUsers().get(0);
		
		if(user == null){
			Utils.error(e.getChannel(), e.getAuthor(), "Invalid user!");
			return;
		}
		
		Member member = e.getGuild().getMember(user);
		
		if(member == null){
			Utils.error(e.getChannel(), e.getAuthor(), "User is not in this guild!");
			return;
		}
		
		String roles = "";
		
		List<Role> userRoles = new ArrayList<Role>();
		
		for(Role role : e.getGuild().getRoles())
			if(e.getGuild().getMembersWithRoles(role).contains(member))
				userRoles.add(role);
		
		if(userRoles.size() > 0){
			for(Role r : userRoles) roles += " - " + r.getName();
			
			roles = roles.substring(3);
		}
		
		StringBuilder builder = new StringBuilder();
		builder.append("```User info for " + user.getName() + "\n")
			   .append("User status: " + member.getOnlineStatus().name().toLowerCase() + "\n")
		       .append("User id: " + user.getId() + "\n")
		       .append("Discriminator: " + user.getDiscriminator() + "\n")
		       .append("Roles: " + roles + "\n")
		       .append("Avatar: " + user.getAvatarUrl() + "```");
		
		Utils.infoBypass(e.getChannel(), builder.toString());
	}

}
