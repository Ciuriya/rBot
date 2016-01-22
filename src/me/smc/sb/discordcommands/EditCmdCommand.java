package me.smc.sb.discordcommands;

import me.itsghost.jdiscord.events.UserChatEvent;
import me.smc.sb.main.Main;
import me.smc.sb.perm.Permissions;
import me.smc.sb.utils.Utils;

public class EditCmdCommand extends GlobalCommand{

	public EditCmdCommand(){
		super(Permissions.MANAGE_MESSAGES, 
			  " - Lets you add, edit and delete commands",
			  "{prefix}editcom\nThis command lets you create and administer custom commands.\n\n" +
			  "----------\nUsage\n----------\n{prefix}editcom {add / setdel / del} {command} - Administer said command\n" + 
			  "{prefix}editcom {add} {command} {instructions} ({desc={description}}) - Adds a command using the specified instructions\n" +
			  "{prefix}editcom {add} {command} {{delimiters={number of delimiters}}} ({desc={description}}) - Adds a command using the specified delimiter count\n" +
			  "{prefix}editcom {setdel} {command} {delimiters} {instructions} - Sets instructions into the specified delimiter combination\n\n" +
			  "----------\nAliases\n----------\n{prefix}editcmd", 
			  false, 
			  "editcom", "editcmd");
	}

	@Override
	public void onCommand(UserChatEvent e, String[] args){
		e.getMsg().deleteMessage();
		if(!Utils.checkArguments(e, args, 2)) return;
		switch(args[0].toLowerCase()){
			case "add": addCommand(e, args); break;
			case "setdel": setDelimiters(e, args); break;
			case "del": deleteCommand(e, args); break;
			default: break;
		}
	}

	private void addCommand(UserChatEvent e, String[] args){
		if(Command.findCommand(e.getServer().getId(), args[1]) != null){
			Utils.error(e.getGroup(), e.getUser().getUser(), " This command already exists!");
			return;
		}
		int delimiters = 0;
		String instructions = "", desc = "";
		for(int i = 2; i < args.length; i++)
			if(args[i].startsWith("{delimiters="))
				delimiters = Utils.stringToInt(args[i].split("\\{delimiters=")[1].split("}")[0]);
			else if(args[i].contains("{desc="))
				desc = instructions.split("\\{desc=")[1].split("}")[0];
			else instructions += " " + args[i];
		instructions = instructions.substring(1);
		Command cmd = new Command(e.getServer().getId(), args[1], instructions, delimiters, desc);
		cmd.save();
		Utils.info(e.getGroup(), Main.getCommandPrefix(e.getServer().getId()) + args[1] + " was added!");
	}
	
	private void setDelimiters(UserChatEvent e, String[] args){
		Command cmd = Command.findCommand(e.getServer().getId(), args[1]);
		if(cmd == null){
			Utils.error(e.getGroup(), e.getUser().getUser(), " This command does not exist!");
			return;
		}
		String dels = "";
		for(int i = 2; i < cmd.getDelimiterCount() + 2; i++)
			dels += " " + args[i];
		
		String instructions = "";
		for(int i = cmd.getDelimiterCount() + 2; i < args.length; i++)
			instructions += " " + args[i];
		cmd.setDelimiter(dels.substring(1).split(" "), instructions.substring(1));
		cmd.save();
		Utils.info(e.getGroup(), "The delimiter combination for " + Main.getCommandPrefix(e.getServer().getId()) + args[1] + " was set!");
	}
	
	private void deleteCommand(UserChatEvent e, String[] args){
		Command cmd = Command.findCommand(e.getServer().getId(), args[1]);
		if(cmd == null){
			Utils.error(e.getGroup(), e.getUser().getUser(), " That command does not exist!");
			return;
		}
		cmd.delete();
		Utils.info(e.getGroup(), Main.getCommandPrefix(e.getServer().getId()) + args[1] + " was deleted!");
	}
	
}
