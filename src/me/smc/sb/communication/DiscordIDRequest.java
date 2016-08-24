package me.smc.sb.communication;

import java.io.File;

import me.smc.sb.main.Main;
import me.smc.sb.utils.Configuration;
import me.smc.sb.utils.Utils;
import net.dv8tion.jda.entities.Guild;
import net.dv8tion.jda.entities.User;

public class DiscordIDRequest extends IncomingRequest{
	
	public DiscordIDRequest(){
		super("REQUEST_ID:", "start");
	}

	@Override
	public void onRequest(String request){
		Guild guild = Main.api.getGuildById(
				  new Configuration(new File("login.txt"))
				  .getValue("discord-server"));
		
		String name = request.replace("REQUEST_ID:", "");
		
		User user = null;
		for(User u : guild.getUsers())
			if(u.getUsername().equalsIgnoreCase(name)){
				user = u;
				break;
			}
		
		if(user != null) 
			Utils.info(null, null, null, "REQUESTED_ID:" + user.getId());
	}
	
}
