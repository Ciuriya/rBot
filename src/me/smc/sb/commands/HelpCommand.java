package me.smc.sb.commands;

import me.itsghost.jdiscord.events.UserChatEvent;
import me.itsghost.jdiscord.message.MessageBuilder;
import me.smc.sb.main.Main;
import me.smc.sb.utils.Utils;

public class HelpCommand extends GlobalCommand{
	
	public HelpCommand(){
		super(null, 
			  " - Lists all commands", 
			  "{prefix}help\nThis command lists every command currently available to you.\n\n" +
		      "----------\nUsage\n----------\n{prefix}help - Lists all commands\n" + 
			  "{prefix}help {command} - Shows the help page for the specific command\n\n" +
		      "----------\nAliases\n----------\nThere are no aliases.", 
			  true, "help");
	}

	@Override
	public void onCommand(UserChatEvent e, String[] args){
		e.getMsg().deleteMessage();
		MessageBuilder msg = new MessageBuilder();
		
    	String serverId = "-1";
    	if(!e.isDm()) serverId = e.getServer().getId();
    	
    	if(args.length > 0){
    		msg.addString("```");
    		boolean added = false;
    		for(GlobalCommand gc : GlobalCommand.commands)
    			if(gc.isName(args[0])){
    				if(!gc.canUse(e.getUser())) return;
    				msg.addString(gc.getExtendedDescription().replaceAll("\\{prefix}", Main.getCommandPrefix(serverId)));
    				added = true;
    				break;
    			}
    		if(!added)
    			if(Command.commands.containsKey(serverId) && Command.commands.get(serverId).containsKey(args[0].toLowerCase())){
    				Command cmd = Command.commands.get(serverId).get(args[0].toLowerCase());
    				msg.addString(Main.getCommandPrefix(serverId) + args[0].toLowerCase() + " - " + cmd.getDesc()); //add extended support here later
    			}
    	}else{
        	msg.addString("```Use '" + Main.getCommandPrefix(serverId) + "help {command}' for specific help per command\nGlobal Commands\n\n");
        	
    		for(GlobalCommand gc : GlobalCommand.commands)
    			if(e.isDm() && !gc.allowsDm()) continue;
    			else if(gc.canUse(e.getUser())) msg.addString(Main.getCommandPrefix(serverId) + gc.getNamesDisplay() + gc.getDescription() + "\n");
    		
    		if(!e.isDm()){ 
    			msg.addString("User Commands\n\n");
    			for(String name : Command.commands.get(serverId).keySet()){
    				String desc = Command.commands.get(serverId).get(name).getDesc();
    				msg.addString(Main.getCommandPrefix(serverId) + name + (desc != "" ? (" - " + desc) : "") + "\n");
    			}
    		}
    	}
		Utils.info(e.getUser().getUser().getGroup(), msg.addString("```").build().getMessage());
	}
	
}
