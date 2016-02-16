package me.smc.sb.listeners;

import java.io.File;
import java.util.logging.Level;

import me.smc.sb.discordcommands.Command;
import me.smc.sb.discordcommands.GlobalCommand;
import me.smc.sb.discordcommands.HaltCommand;
import me.smc.sb.main.Main;
import me.smc.sb.utils.Configuration;
import me.smc.sb.utils.Log;
import me.smc.sb.utils.Utils;
import net.dv8tion.jda.JDA;
import net.dv8tion.jda.entities.Guild;
import net.dv8tion.jda.events.Event;
import net.dv8tion.jda.events.message.MessageReceivedEvent;
import net.dv8tion.jda.hooks.EventListener;

public class Listener implements EventListener{

	JDA api;
	
	public Listener(){}
	
	public void setAPI(JDA api){
		this.api = api;
	}
	
	@Override
    public void onEvent(Event event){
		if(event instanceof MessageReceivedEvent){
			MessageReceivedEvent e = (MessageReceivedEvent) event;
			
	    	if(e.getAuthor().getId().equalsIgnoreCase("120923487467470848")) return; //if user is bot, no msg
	    	Main.messagesReceivedThisSession++;
	    	
	    	boolean dm = e.isPrivate();
	    	
	    	if(!dm && HaltCommand.stopCommands.containsKey(e.getGuild().getId()) && HaltCommand.stopCommands.get(e.getGuild().getId())) return;
	    	
	    	String serverId = "-1";
	    	if(!dm) serverId = e.getGuild().getId();
	    	
	    	String cmdPrefix = Main.getCommandPrefix(serverId);
	    	String msg = e.getMessage().getContent();
	    	
	    	if(msg.startsWith(cmdPrefix) && msg.contains(cmdPrefix)){
	        	Log.logger.log(Level.INFO, "{Command in " + Utils.getGroupLogString(e.getChannel())
				 + " sent by " + e.getAuthor().getUsername() + " <" + e.getAuthor().getId() + ">}\n" + msg);
	        	
	        	Command cmd = null;
	        	cmd = Command.findCommand(serverId, msg.split(" ")[0].replace(cmdPrefix, ""));
	        	
	        	if(GlobalCommand.handleCommand(e, msg.replace(cmdPrefix, ""))) return;
	        	else if(cmd == null) return;
	        	
	        	cmd.execute(e);
	    	}
		}
    }
    
    public static void loadGuilds(JDA api){
    	Main.serverConfigs.clear();
    	Command.commands.clear();
    	
    	for(Guild guild : api.getGuilds()){
    		Main.serverConfigs.put(guild.getId(), new Configuration(new File(guild.getId() + ".txt")));
    		Command.loadCommands(guild.getId());	
    	}
    	
    	GlobalCommand.registerCommands();
    	Utils.infoBypass(Main.api.getUserById("91302128328392704").getPrivateChannel(), "I am now logged in!"); //Sends the developer a message on login
    }
	
}
