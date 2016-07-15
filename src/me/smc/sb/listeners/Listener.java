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
import net.dv8tion.jda.events.ReadyEvent;
import net.dv8tion.jda.events.ReconnectedEvent;
import net.dv8tion.jda.events.guild.GuildJoinEvent;
import net.dv8tion.jda.events.guild.GuildLeaveEvent;
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
	    	if(!dm){
	    		serverId = e.getGuild().getId();
	    		
	    		if(!Main.serverConfigs.containsKey(serverId))
	    			loadGuilds(event.getJDA());
	    	}
	    	
	    	String cmdPrefix = Main.getCommandPrefix(serverId);
	    	String msg = e.getMessage().getContent();
	    	
	    	String strippedMsg = msg;
	    	
	    	if(!cmdPrefix.equals(Main.defaultPrefix) && msg.startsWith(cmdPrefix))
	    		strippedMsg = strippedMsg.replace(cmdPrefix, "");
	    	else strippedMsg = strippedMsg.replace(Main.defaultPrefix, "");
	    	
	    	if(!strippedMsg.equals(msg)){
	    		if(!dm && Main.serverConfigs.get(serverId).getStringList("rpg-enabled-channels").contains(e.getTextChannel().getId()))
	    			strippedMsg = "rpg " + strippedMsg;
	    		
	        	Log.logger.log(Level.INFO, "{Command in " + Utils.getGroupLogString(e.getChannel())
				 + " sent by " + e.getAuthor().getUsername() + " <" + e.getAuthor().getId() + ">}\n" + msg);
	        	
	        	Command cmd = null;
	        	cmd = Command.findCommand(serverId, strippedMsg.split(" ")[0]);
	        	
	        	if(GlobalCommand.handleCommand(e, strippedMsg)) return;
	        	else if(cmd == null) return;
	        	
	        	cmd.execute(e);
	        	return;
	    	}else if(dm && !msg.startsWith(Main.defaultPrefix)){
	        	Log.logger.log(Level.INFO, "{Prefixless command in " + Utils.getGroupLogString(e.getChannel())
				 + " sent by " + e.getAuthor().getUsername() + " <" + e.getAuthor().getId() + ">}\n" + msg);
	        	
	        	Command cmd = null;
	        	cmd = Command.findCommand(serverId, msg.split(" ")[0]);
	        	
	        	if(GlobalCommand.handleCommand(e, msg)) return;
	        	else if(cmd == null) return;
	        	
	        	cmd.execute(e);
	        	return;
	    	}
	    	
	    	if(dm)
	    		Utils.infoBypass(e.getChannel(), "It seems you are having a problem... use ~/help to get a list of commands!" +
	    									     "\nIf you have any issues, feel free to contact Smc!" +
	    									     "\nContact: PM, server: https://discord.gg/0phGqtqLYwSzCdwn");
		}else if(event instanceof ReadyEvent){
			GlobalCommand.registerCommands();
			api = event.getJDA();
			api.setAutoReconnect(true);
			loadGuilds(api);
			Utils.infoBypass(api.getUserById("91302128328392704").getPrivateChannel(), "I am now logged in!"); //Sends the developer a message on login
			IRCChatListener.pmList = new Configuration(new File("login.txt")).getStringList("yield-pms");
		}else if(event instanceof ReconnectedEvent)
			Utils.infoBypass(api.getUserById("91302128328392704").getPrivateChannel(), "I have reconnected!");
		else if(event instanceof GuildJoinEvent || event instanceof GuildLeaveEvent)
			loadGuilds(event.getJDA());
    }
    
    public static void loadGuilds(JDA api){
    	Main.serverConfigs.clear();
    	Command.commands.clear();
    	
    	for(Guild guild : api.getGuilds()){
    		Main.serverConfigs.put(guild.getId(), new Configuration(new File("Guilds/" + guild.getId() + ".txt")));
    		Command.loadCommands(guild.getId());	
    	}
    }
	
}
