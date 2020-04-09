package me.smc.sb.discordcommands;

import me.smc.sb.main.Main;
import me.smc.sb.utils.Utils;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.PrivateChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class HelpCommand extends GlobalCommand{
	
	public HelpCommand(){
		super(null, 
			  " - Lists all commands", 
			  "{prefix}help\nThis command lists every command currently available to you.\n\n" +
		      "----------\nUsage\n----------\n{prefix}help - Lists all commands\n" + 
			  "{prefix}help {command} - Shows the help page for the specific command\n\n" +
		      "----------\nAliases\n----------\nThere are no aliases.", 
			  true, 
			  "help");
	}

	@Override
	public void onCommand(MessageReceivedEvent e, String[] args){
		Utils.deleteMessage(e.getChannel(), e.getMessage());
		StringBuilder msg = new StringBuilder();
		
    	String serverId = "-1";
    	if(!e.isFromType(ChannelType.PRIVATE)) serverId = e.getGuild().getId();
    	
    	if(args.length > 0){
    		boolean added = false;
    		
    		for(GlobalCommand gc : GlobalCommand.commands)
    			if(gc.isName(args[0])){
    				if(!gc.canUse(e.getAuthor(), e.getChannel())) return;
    				msg.append(gc.getExtendedDescription().replaceAll("\\{prefix}", Main.getCommandPrefix(serverId)));
    				added = true;
    				break;
    			}
    		
    		if(!added)
    			if(Command.commands.containsKey(serverId) && Command.commands.get(serverId).containsKey(args[0].toLowerCase())){
    				Command cmd = Command.commands.get(serverId).get(args[0].toLowerCase());
    				msg.append(Main.getCommandPrefix(serverId) + args[0].toLowerCase() + " - " + cmd.getDesc()); //add extended support here later
    			}
    	}else{
        	msg.append("Use 'help {command}' for specific help per command (you do not need a command prefix in PM)\n\nGlobal Commands\n\n");
    		
    		for(GlobalCommand gc : GlobalCommand.commands)		
    			if(serverId.equalsIgnoreCase("-1") && !gc.allowsDm()) continue;
    			else if(gc.canUse(e.getAuthor(), e.getChannel())) msg.append(Main.getCommandPrefix(serverId) + gc.getNamesDisplay() + gc.getDescription() + "\n");
    		
    		if(!serverId.equalsIgnoreCase("-1")){ 
    			msg.append("\n\nUser Commands\n\n");
    			
    			for(String name : Command.commands.get(serverId).keySet()){
    				String desc = Command.commands.get(serverId).get(name).getDesc();
    				msg.append(Main.getCommandPrefix(serverId) + name + (desc != "" ? (" - " + desc) : "") + "\n");
    			}
    		}
    	}
    	
    	String rest = msg.toString();
    	
    	PrivateChannel channel = e.getAuthor().openPrivateChannel().complete();
    	
    	while(rest.length() > 1960){
    		String toSend = rest.substring(0, 1960);
    		rest = rest.substring(1960);
    		
    		Utils.info(channel, "```\n" + toSend + "```");
    	}
    	
		Utils.info(channel, "```\n" + rest + "```" + (args.length == 0 ? "\nHelp Server: http://discord.gg/0phGqtqLYwSzCdwn" : ""));
	}
	
}
