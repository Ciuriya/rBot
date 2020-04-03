package me.smc.sb.communication;

import java.io.File;

import me.smc.sb.main.Main;
import me.smc.sb.utils.Configuration;
import me.smc.sb.utils.Utils;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;

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
		
		for(Member member : guild.getMembers())
			if(member != null)
				names += member.getUser().getName() + "`" + member.getUser().getId() + ":";
		
		Utils.info(null, null, null, names.substring(0, names.length() - 1));
	}
	
}
