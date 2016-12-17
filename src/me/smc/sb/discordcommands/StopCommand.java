package me.smc.sb.discordcommands;

import me.smc.sb.main.Main;
import me.smc.sb.perm.Permissions;
import me.smc.sb.utils.Utils;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

public class StopCommand extends GlobalCommand{

	public StopCommand(){
		super(Permissions.BOT_ADMIN, 
			  " - Stops, restarts or updates the server", 
			  "{prefix}stop\nThis command either stops, restarts or updates the server.\n\n" +
			  "----------\nUsage\n----------\n{prefix}stop - Stops the server\n" + 
			  "{prefix}stop {exit code} - Stops with the selected exit code, 1 stops, 2 restarts and 3 updates\n\n" +
			  "----------\nAliases\n----------\n{prefix}restart - Restarts the server\n{prefix}update - Updates the server",  
			  true, 
			  "stop", "restart", "update");
	}

	@Override
	public void onCommand(MessageReceivedEvent e, String[] args){
		Utils.deleteMessage(e.getChannel(), e.getMessage());
		
		int retCode = 1;
		
		if(args.length > 0) retCode = Utils.stringToInt(args[0]);
		else if(e.getMessage().getContent().contains("restart")) retCode = 2;
		else if(e.getMessage().getContent().contains("update")) retCode = 3;
		
		Utils.info(e.getChannel(), "You have" + getMessageBasedOnCode(retCode));
		Main.stop(retCode);
	}
	
	private String getMessageBasedOnCode(int retCode){
		switch(retCode){
			case 2: return " requested a restart!";
			case 3: return " requested a bot update!";
			default: return " requested a shutdown!";
		}
	}
	
}
