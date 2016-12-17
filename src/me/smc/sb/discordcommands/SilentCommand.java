package me.smc.sb.discordcommands;

import me.smc.sb.main.Main;
import me.smc.sb.perm.Permissions;
import me.smc.sb.utils.Configuration;
import me.smc.sb.utils.Utils;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

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
	public void onCommand(MessageReceivedEvent e, String[] args) {
		Utils.deleteMessage(e.getChannel(), e.getMessage());
		if(!Utils.checkArguments(e, args, 1)) return;
		
		if(!args[0].equalsIgnoreCase("true") && !args[0].equalsIgnoreCase("false")) return;
		Configuration cfg = Main.serverConfigs.get(e.getGuild().getId());
		cfg.writeValue("silent", args[0].toLowerCase());
	}
	
}
