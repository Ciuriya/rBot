package me.smc.sb.discordcommands;

import me.itsghost.jdiscord.events.UserChatEvent;
import me.smc.sb.listeners.Listener;
import me.smc.sb.main.Main;
import me.smc.sb.utils.Utils;

public class JoinServerCommand extends GlobalCommand{

	public JoinServerCommand(){
		super(null, 
			  " - Lets the bot join the requested server", 
			  "{prefix}joinserver\nThis command makes the bot join the requested server.\n\n" +
			  "----------\nUsage\n----------\n{prefix}joinserver - Makes the bot join the server\n\n" + 
			  "----------\nAliases\n----------\nThere are no aliases.",  
			  true, 
			  "joinserver");
	}

	@Override
	public void onCommand(UserChatEvent e, String[] args){
		if(!Utils.checkArguments(e, args, 1)) return;
		if(args[0].startsWith("https://discord.gg/"))
			args[0] = args[0].replace("https://discord.gg/", "");
		Main.api.joinInviteId(args[0]);
		Listener.loadServers(Main.api);
	}

}
