package me.smc.sb.communication;

import java.util.logging.Level;

import me.smc.sb.main.Main;
import me.smc.sb.utils.Log;
import me.smc.sb.utils.Utils;

public class VerifyUserRequest extends IncomingRequest{

	public VerifyUserRequest(){
		super("VERIFY:", "start");
	}

	@Override
	public void onRequest(String request){
		String[] split = request.split(":");
		
		String medium = split[1];
		
		switch(medium.toLowerCase()){
			case "discord":
				if(Main.api.getUserById(split[2]) != null)
					Utils.infoBypass(Main.api.getUserById(split[2]).getPrivateChannel(), "Your verification code is " + split[2]); 	
				break;
			case "osu": 
				String user = split[2].replaceAll(" ", "_");
				try{
					Main.ircBot.sendIRC().joinChannel(user);
				}catch(Exception ex){
					Log.logger.log(Level.INFO, "Could not talk to " + user + "!");
				}
				
				Main.ircBot.sendIRC().message(user, "Your verification code is " + split[3]);
				break;
			default: break;
		}
	}

}
