package me.smc.sb.commands;

import me.itsghost.jdiscord.events.UserChatEvent;
import me.smc.sb.main.Main;
import me.smc.sb.perm.Permissions;
import me.smc.sb.utils.Configuration;
import me.smc.sb.utils.Utils;

public class SilentCommand extends GlobalCommand{

	public SilentCommand(){
		super(Permissions.MANAGE_MESSAGES, 
			  " - Silences many feedback messages in this server", 
			  "{prefix}silent\nThis command silences some feedback messages on the server to reduce spam.\n\n" +
			  "----------\nUsage\n----------\n{prefix}silent {true | false} - Silences select feedback messages on the server\n\n" + 
			  "----------\nAliases\n----------\nThere are no aliases.", 
			  false, 
			  "silent");
	}

	@Override
	public void onCommand(UserChatEvent e, String[] args) {
		e.getMsg().deleteMessage();
		if(!Utils.checkArguments(e, args, 1)) return;
		if(!args[0].equalsIgnoreCase("true") && !args[0].equalsIgnoreCase("false")) return;
		Configuration cfg = Main.serverConfigs.get(e.getServer().getId());
		cfg.writeValue("silent", args[0].toLowerCase());
	}
	
}
