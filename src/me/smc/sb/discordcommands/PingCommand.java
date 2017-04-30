package me.smc.sb.discordcommands;

import java.net.InetAddress;
import java.util.logging.Level;

import me.smc.sb.perm.Permissions;
import me.smc.sb.utils.Log;
import me.smc.sb.utils.Utils;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

public class PingCommand extends GlobalCommand{

	public PingCommand(){
		super(Permissions.BOT_ADMIN, 
			  " - Pings a website", 
			  "{prefix}ping\nThis command pings the specified website\n\n" +
			  "----------\nUsage\n----------\n{prefix}ping {site} - Pings the website and says how long it took\n\n" + 
		      "----------\nAliases\n----------\nThere are no aliases.",  
			  true, 
			  "ping");
	}
	
	@Override
	public void onCommand(MessageReceivedEvent e, String[] args){
		Utils.deleteMessage(e.getChannel(), e.getMessage());
		if(!Utils.checkArguments(e, args, 1)) return;
		
		Thread thread = new Thread(new Runnable(){
			public void run(){
				try{
					InetAddress address = InetAddress.getByName(args[0]);
					long time = System.currentTimeMillis();
					
					boolean reachable = address.isReachable(15000);
					
					long finalTime = System.currentTimeMillis() - time;
					
					if(reachable) Utils.info(e.getChannel(), "Response time of " + args[0] + " is " + finalTime + " milliseconds!");
					else Utils.info(e.getChannel(), "Couldn't reach " + args[0] + " in 15 seconds!");
				}catch(Exception ex){
					Utils.info(e.getChannel(), "An error occured while pinging the website!");
					Log.logger.log(Level.SEVERE, ex.getMessage(), ex);
				}	
			}
		});
		
		thread.start();
	}
	
}
