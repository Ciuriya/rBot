package me.smc.sb.commands;

import me.itsghost.jdiscord.events.UserChatEvent;
import me.smc.sb.main.Main;
import me.smc.sb.perm.Permissions;
import me.smc.sb.utils.Utils;

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
	public void onCommand(UserChatEvent e, String[] args) {
		e.getMsg().deleteMessage();
		if(!Utils.checkArguments(e, args, 1));
		String server = e.getServer().getId();
		Main.serverConfigs.get(server).writeValue("command-prefix", args[0]);
		Utils.info(e.getGroup(), "The server's prefix has been set to " + args[0]);
	}

}
