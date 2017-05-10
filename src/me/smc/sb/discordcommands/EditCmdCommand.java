package me.smc.sb.discordcommands;

import me.smc.sb.main.Main;
import me.smc.sb.perm.Permissions;
import me.smc.sb.utils.Utils;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

public class EditCmdCommand extends GlobalCommand{

	public EditCmdCommand(){
		super(Permissions.MANAGE_MESSAGES, 
			  " - Lets you add, edit and delete commands",
			  "{prefix}editcom\nThis command lets you create and administer custom commands.\n\n" +
			  "----------\nUsage\n----------\n{prefix}editcom {add / edit / setdel / del} {command} - Administer said command\n" + 
			  "{prefix}editcom add {command} {instructions} ({desc={description}}) - Adds a command using the specified instructions\n" +
			  "{prefix}editcom add {command} {{delimiters={number of delimiters}}} ({desc={description}}) - Adds a command using the specified delimiter count\n" +
			  "{prefix}editcom setdel {command} {delimiters} {instructions} - Sets instructions into the specified delimiter combination\n\n" +
			  "----------\nAliases\n----------\n{prefix}editcmd", 
			  false, 
			  "editcom", "editcmd", "ec");
	}

	@Override
	public void onCommand(MessageReceivedEvent e, String[] args){
		Utils.deleteMessage(e.getChannel(), e.getMessage());
		if(!Utils.checkArguments(e, args, 2)) return;
		switch(args[0].toLowerCase()){
			case "add": addCommand(e, args); break;
			case "setdel": setDelimiters(e, args); break;
			case "del": deleteCommand(e, args); break;
			default: break;
		}
	}

	private void addCommand(MessageReceivedEvent e, String[] args){
		if(Command.findCommand(e.getGuild().getId(), args[1]) != null){
			Utils.error(e.getChannel(), e.getAuthor(), " This command already exists!");
			return;
		}
		
		int delimiters = 0;
		String instructions = "", desc = "";
		
		for(int i = 2; i < args.length; i++)
			if(args[i].startsWith("{delimiters="))
				delimiters = Utils.stringToInt(args[i].split("\\{delimiters=")[1].split("}")[0]);
			else if(args[i].contains("{desc="))
				desc = args[i].split("\\{desc=")[1].split("}")[0];
			else instructions += " " + args[i];
		
		if(delimiters == 0) instructions = instructions.substring(1);
		Command cmd = new Command(e.getGuild().getId(), args[1], instructions, delimiters, desc);
		cmd.save();
		Utils.info(e.getChannel(), Main.getCommandPrefix(e.getGuild().getId()) + args[1] + " was added!");
	}
	
	private void setDelimiters(MessageReceivedEvent e, String[] args){
		Command cmd = Command.findCommand(e.getGuild().getId(), args[1]);
		if(cmd == null){
			Utils.error(e.getChannel(), e.getAuthor(), " This command does not exist!");
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
		
		Utils.info(e.getChannel(), "The delimiter combination for " + Main.getCommandPrefix(e.getGuild().getId()) + args[1] + " was set!");
	}
	
	private void deleteCommand(MessageReceivedEvent e, String[] args){
		Command cmd = Command.findCommand(e.getGuild().getId(), args[1]);
		if(cmd == null){
			Utils.error(e.getChannel(), e.getAuthor(), " That command does not exist!");
			return;
		}
		
		cmd.delete();
		Utils.info(e.getChannel(), Main.getCommandPrefix(e.getGuild().getId()) + args[1] + " was deleted!");
	}
	
}
