package me.Smc.sb.commands;

import me.Smc.sb.main.Main;
import me.Smc.sb.perm.GlobalAdmins;
import me.Smc.sb.utils.Utils;
import me.itsghost.jdiscord.events.UserChatEvent;
import me.itsghost.jdiscord.message.MessageBuilder;
import me.itsghost.jdiscord.talkable.Group;

public class CommandList{

	public static void execute(UserChatEvent e, boolean dm){
		e.getMsg().deleteMessage();
		MessageBuilder msg = new MessageBuilder();
		String server = "";
		if(!dm) server = e.getServer().getId();
		msg.addString("```Global Commands\n\n")
		   .addString(Main.getCommandPrefix(server) + "joinserver {invite}\n")
		   .addString(Main.getCommandPrefix(server) + "search {search service} {search query} ({result={result number}} GOOGLE ONLY)");
		Group userGroup = e.getGroup();
		if(!dm){
			for(String cmdName : Command.globalCommands.keySet()){
				if(Command.globalCommands.get(cmdName).isAdminOnly() && !GlobalAdmins.isAdmin(e.getUser())) continue;
				else if(cmdName.equalsIgnoreCase("joinserver")) continue;
				switch(cmdName){
					case "joinserver": continue;
					case "search": continue;
					case "addcom": continue;
					default: break;
				}
				String desc = Command.globalCommands.get(cmdName).getDesc();
				if(desc != "") msg.addString(Main.getCommandPrefix(server) + cmdName + " " + desc + "\n");
				else msg.addString(Main.getCommandPrefix(server) + cmdName + "\n");
			}
		    msg.addString(Main.getCommandPrefix(server) + "addcom {command} {text} {{desc={description}}}\n")
			   .addString(Main.getCommandPrefix(server) + "addcom {command} {{delimiters={number of delimiters}}} {{desc={description}}}\n");
			userGroup = e.getUser().getUser().getGroup();
			msg.addString("\nUser Commands\n\n");
			for(String name : Command.commands.get(server).keySet()){
				String desc = Command.commands.get(server).get(name).getDesc();
				msg.addString(Main.getCommandPrefix(server) + name + (desc != "" ? (" - " + desc) : "") + "\n");
			}
		}
		Utils.info(userGroup, msg.addString("```").build().getMessage());
	}
	
}
