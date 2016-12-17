package me.smc.sb.discordcommands;

import me.smc.sb.main.Main;
import me.smc.sb.perm.Permissions;
import me.smc.sb.utils.Utils;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

public class SetPrefixCommand extends GlobalCommand{

	public SetPrefixCommand(){
		super(Permissions.MANAGE_SERVER, 
			  " - Sets the command prefix for this server", 
			  "{prefix}setprefix\nThis command lets you change the command prefix for the server\n\n" +
			  "----------\nUsage\n----------\n{prefix}setprefix - Sets the command prefix for the server\n\n" + 
			  "----------\nAliases\n----------\nThere are no aliases.",
			  false, 
			  "setprefix");
	}

	@Override
	public void onCommand(MessageReceivedEvent e, String[] args) {
		Utils.deleteMessage(e.getChannel(), e.getMessage());
		if(!Utils.checkArguments(e, args, 1));
		
		String server = e.getGuild().getId();
		Main.serverConfigs.get(server).writeValue("command-prefix", args[0]);
		Utils.info(e.getChannel(), "The server's prefix has been set to " + args[0]);
	}

}
