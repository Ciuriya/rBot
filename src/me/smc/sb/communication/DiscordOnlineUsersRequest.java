package me.smc.sb.communication;

import java.io.File;

import me.smc.sb.main.Main;
import me.smc.sb.utils.Configuration;
import me.smc.sb.utils.Utils;
import net.dv8tion.jda.OnlineStatus;
import net.dv8tion.jda.entities.Guild;
import net.dv8tion.jda.entities.User;

public class DiscordOnlineUsersRequest extends IncomingRequest{

	public DiscordOnlineUsersRequest(){
		super("REQUEST_ONLINE_USERS", "eq");
	}

	@Override
	public void onRequest(String request){
		Guild guild = Main.api.getGuildById(
					  new Configuration(new File("login.txt"))
					  .getValue("discord-server"));
		
		String names = "REQUESTED_USERS:";
		
		for(User user : guild.getUsers())
			if(user != null && 
				user.getOnlineStatus() != null &&
				!user.getOnlineStatus().equals(OnlineStatus.OFFLINE) &&
				!user.getOnlineStatus().equals(OnlineStatus.UNKNOWN))
				names += user.getUsername() + "`" + user.getId() + ":";
		
		Utils.info(null, null, null, names.substring(0, names.length() - 1));
	}
	
}
