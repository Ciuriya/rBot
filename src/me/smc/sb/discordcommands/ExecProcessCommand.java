package me.smc.sb.discordcommands;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.logging.Level;

import me.smc.sb.perm.Permissions;
import me.smc.sb.utils.Log;
import me.smc.sb.utils.Utils;
import net.dv8tion.jda.events.message.MessageReceivedEvent;

public class ExecProcessCommand extends GlobalCommand{

	public ExecProcessCommand(){
		super(Permissions.IRC_BOT_ADMIN, 
			  " - Executes a process", "",  
			  true,
			  "execP");
	}

	@Override
	public void onCommand(MessageReceivedEvent e, String[] args){
		if(!Utils.checkArguments(e, args, 1)) return;
		
		String message = "";
		for(int i = 0; i < args.length; i++)
			message += " " + args[i];
		message = message.substring(1);
		
		try{
			Process p = Runtime.getRuntime().exec(message);
			
			BufferedReader pIn = new BufferedReader(new InputStreamReader(p.getInputStream()));
			
			String t = null;
			
			Log.logger.log(Level.INFO, "-- Input --");
			
			while((t = pIn.readLine()) != null){
				Log.logger.log(Level.INFO, t);
			}
			
			Log.logger.log(Level.INFO, "-- Error --");
			
			BufferedReader pErr = new BufferedReader(new InputStreamReader(p.getErrorStream()));
			
			while((t = pErr.readLine()) != null){
				Log.logger.log(Level.INFO, t);
			}
			
		}catch(Exception ex){
			Log.logger.log(Level.SEVERE, ex.getMessage(), ex);
		}
	}
	
}
