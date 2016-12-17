package me.smc.sb.communication;

import java.io.File;

import me.smc.sb.main.Main;
import me.smc.sb.utils.Configuration;
import me.smc.sb.utils.Utils;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.User;

public class DiscordNameRequest extends IncomingRequest{

	public DiscordNameRequest(){
		super("REQUEST_NAME:", "start");
	}

	@Override
	public void onRequest(String request){
		Guild guild = Main.api.getGuildById(
				  new Configuration(new File("login.txt"))
				  .getValue("discord-server"));
		
		String id = request.replace("REQUEST_NAME:", "");
		
		User user = null;
		for(Member m : guild.getMembers())
			if(m.getUser().getId().equalsIgnoreCase(id)){
				user = m.getUser();
				break;
			}
		
		if(user != null)
			Utils.info(null, null, null, "REQUESTED_NAME:" + user.getName());
	}

}
