package me.smc.sb.communication;

import java.io.File;

import me.smc.sb.main.Main;
import me.smc.sb.utils.Configuration;
import net.dv8tion.jda.entities.Guild;
import net.dv8tion.jda.entities.User;

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
		for(User u : guild.getUsers())
			if(u.getId().equalsIgnoreCase(id)){
				user = u;
				break;
			}
		
		if(user != null)
			Main.server.sendMessage("REQUESTED_NAME:" + user.getUsername());
	}

}
