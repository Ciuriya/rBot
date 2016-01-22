package me.smc.sb.listeners;

import java.io.File;
import java.util.logging.Level;

import me.itsghost.jdiscord.DiscordAPI;
import me.itsghost.jdiscord.Server;
import me.itsghost.jdiscord.event.EventListener;
import me.itsghost.jdiscord.events.APILoadedEvent;
import me.itsghost.jdiscord.events.UserChatEvent;
import me.smc.sb.discordcommands.Command;
import me.smc.sb.discordcommands.GlobalCommand;
import me.smc.sb.discordcommands.HaltCommand;
import me.smc.sb.main.Main;
import me.smc.sb.missingapi.MessageHistory;
import me.smc.sb.utils.Configuration;
import me.smc.sb.utils.Log;
import me.smc.sb.utils.Utils;

public class Listener implements EventListener{

	DiscordAPI api;
	
	public Listener(DiscordAPI api){
		this.api = api;
	}
	
    public void userChat(UserChatEvent e){
    	if(MessageHistory.getHistory(e.getGroup().getId()) == null)
    		new MessageHistory(e.getGroup().getId());
    	MessageHistory.addMessage(e.getGroup(), e.getMsg());
    	if(e.getUser().getUser().getId().equalsIgnoreCase("120923487467470848")) return; //if user is bot, no msg
    	Main.messagesReceivedThisSession++;
    	
    	if(e.getServer() != null && HaltCommand.stopCommands.containsKey(e.getServer().getId()) && HaltCommand.stopCommands.get(e.getServer().getId())) return;
    	
    	String serverId = "-1";
    	if(e.getServer() != null) serverId = e.getServer().getId();
    	
    	String cmdPrefix = Main.getCommandPrefix(serverId);
    	String msg = e.getMsg().getMessage();
    	if(msg.startsWith(cmdPrefix) && msg.contains(cmdPrefix)){
        	Log.logger.log(Level.INFO, "{Command in " + Utils.getGroupLogString(e.getGroup())
			 + " sent by " + e.getUser().getUser() + " <" + e.getUser().getUser().getId() + ">}\n" + msg);
        	Command cmd = null;
        	cmd = Command.findCommand(serverId, msg.split(" ")[0].replace(cmdPrefix, ""));
        	if(GlobalCommand.handleCommand(e, msg.replace(cmdPrefix, ""))) return;
        	else if(cmd == null) return;
        	cmd.execute(e);
    	}
    }
    
    public void apiLoad(APILoadedEvent e){
    	loadServers(api);
    	Utils.infoBypass(Main.api.getUserById("91302128328392704").getGroup(), "I have logged in! :D"); //Sends the developer a message on login
    }
    
    public static void loadServers(DiscordAPI api){
    	Main.serverConfigs.clear();
    	Command.commands.clear();
    	for(Server s : api.getAvailableServers()){
    		Main.serverConfigs.put(s.getId(), new Configuration(new File(s.getId() + ".txt")));
    		Command.loadCommands(s.getId());	
    	}
    	GlobalCommand.registerCommands();
    }
	
}
