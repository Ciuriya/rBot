package me.Smc.sb.listeners;

import java.io.File;

import me.Smc.sb.commands.Command;
import me.Smc.sb.commands.GlobalCommands;
import me.Smc.sb.commands.Halt;
import me.Smc.sb.main.Main;
import me.Smc.sb.missingapi.MessageHistory;
import me.Smc.sb.utils.Configuration;
import me.Smc.sb.utils.Utils;
import me.itsghost.jdiscord.DiscordAPI;
import me.itsghost.jdiscord.Server;
import me.itsghost.jdiscord.event.EventListener;
import me.itsghost.jdiscord.events.APILoadedEvent;
import me.itsghost.jdiscord.events.UserChatEvent;

public class Listener implements EventListener{

	DiscordAPI api;
	
	public Listener(DiscordAPI api){
		this.api = api;
	}
	
    public void userChat(UserChatEvent e){
    	if(MessageHistory.getHistory(e.getGroup().getId()) == null)
    		new MessageHistory(e.getGroup().getId());
    	MessageHistory.addMessage(e.getGroup(), e.getMsg());
    	if(e.getUser().getUser().getId().equalsIgnoreCase("120923487467470848")) return;
    	Main.messagesThisSession++;
    	if(Halt.stopCommands.containsKey(e.getServer().getId()))
    		if(Halt.stopCommands.get(e.getServer().getId())) return;
    	boolean dm = false;
    	if(e.getServer() == null) dm = true;
    	String cmdPrefix = "~/";
    	if(!dm) cmdPrefix = Main.getCommandPrefix(e.getServer().getId());
    	String msg = e.getMsg().getMessage();
    	String lowMsg = msg.toLowerCase();
    	if(!lowMsg.startsWith(cmdPrefix)){
    		if(msg.contains("\u0028\u256F\u00B0\u25A1\u00B0\uFF09\u256F\uFE35\u0020\u253B\u2501\u253B"))
    			e.getGroup().sendMessage("\u252C\u2500\u252C\u30CE\u0028\u0020\u25D5\u25E1\u25D5\u0020\u30CE\u0029");
    		return;
    	}
    	msg = msg.replaceFirst(cmdPrefix, "");
		String name = msg.split(" ")[0];
		Command cmd = null;
		if(!dm) cmd = Command.findCommand(e.getServer().getId(), name);
		if(Command.globalCommands.containsKey(name)){
			GlobalCommands.handleCommand(e, name, dm);
			return;
		}else if(cmd == null){
			if(!dm) Utils.error(e.getGroup(), e.getUser().getUser(), " This command does not exist!");
			else Utils.info(e.getGroup(), "This command does not exist!");
			return;
		}
		if(!dm) cmd.execute(e);
    }
    
    public void apiLoad(APILoadedEvent e){
    	for(Server s : api.getAvailableServers()){
    		Main.serverConfigs.put(s.getId(), new Configuration(new File(s.getId() + ".txt")));
    		Command.loadCommands(s.getId());	
    	}
    	Command.loadGlobalCommands();
    	Main.api.getUserById("91302128328392704").getGroup().sendMessage("I have logged in! :D"); //Sends the developer a message on login
    }
    
    public static boolean checkArguments(String msg, int length, UserChatEvent e){
		if(msg.split(" ").length < length){
			Utils.error(e.getGroup(), e.getUser().getUser(), " Invalid arguments!");
			return false;
		}
		return true;
    }
	
}
