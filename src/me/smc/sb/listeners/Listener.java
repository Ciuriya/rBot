package me.smc.sb.listeners;

import java.io.File;
import java.util.logging.Level;

import me.itsghost.jdiscord.DiscordAPI;
import me.itsghost.jdiscord.Server;
import me.itsghost.jdiscord.event.EventListener;
import me.itsghost.jdiscord.events.APILoadedEvent;
import me.itsghost.jdiscord.events.UserChatEvent;
import me.smc.sb.commands.Command;
import me.smc.sb.commands.GlobalCommand;
import me.smc.sb.commands.HaltCommand;
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
    	
    	if(!e.isDm() && HaltCommand.stopCommands.containsKey(e.getServer().getId()) && HaltCommand.stopCommands.get(e.getServer().getId())) return;
    	
    	String serverId = "-1";
    	if(!e.isDm()) serverId = e.getServer().getId();
    	
    	String cmdPrefix = Main.getCommandPrefix(serverId);
    	String msg = e.getMsg().getMessage();
    	if(!msg.toLowerCase().startsWith(cmdPrefix)){
    		if(msg.contains("\u0028\u256F\u00B0\u25A1\u00B0\uFF09\u256F\uFE35\u0020\u253B\u2501\u253B")) //tableflip
    			Utils.info(e.getGroup(), "\u252C\u2500\u252C\u30CE\u0028\u0020\u25D5\u25E1\u25D5\u0020\u30CE\u0029"); //response
    		return;
    	}
    	Log.logger.log(Level.INFO, "{Command in " + Utils.getGroupLogString(e.getGroup())
    			                   + " sent by " + e.getUser().getUser() + " <" + e.getUser().getUser().getId() + ">}\n" + msg);
		Command cmd = null;
		cmd = Command.findCommand(serverId, msg.split(" ")[0].replace(cmdPrefix, ""));
		if(GlobalCommand.handleCommand(e, msg.replace(cmdPrefix, ""))) return;
		else if(cmd == null) return;
		cmd.execute(e);
    }
    
    public void apiLoad(APILoadedEvent e){
    	for(Server s : api.getAvailableServers()){
    		Main.serverConfigs.put(s.getId(), new Configuration(new File(s.getId() + ".txt")));
    		Command.loadCommands(s.getId());	
    	}
    	GlobalCommand.registerCommands();
    	Utils.infoBypass(Main.api.getUserById("91302128328392704").getGroup(), "I have logged in! :D"); //Sends the developer a message on login
    }
	
}
