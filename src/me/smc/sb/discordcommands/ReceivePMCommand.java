package me.smc.sb.discordcommands;

import java.io.File;

import me.smc.sb.listeners.IRCChatListener;
import me.smc.sb.perm.Permissions;
import me.smc.sb.utils.Configuration;
import me.smc.sb.utils.Utils;
import net.dv8tion.jda.core.entities.ChannelType;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

public class ReceivePMCommand extends GlobalCommand{

	public ReceivePMCommand(){
		super(Permissions.IRC_BOT_ADMIN, 
			  " - Toggles the osu! pm notifications to discord", 
			  "{prefix}receivePM\nThis command toggles the pm bridge from osu! to discord.\n\n" +
			  "----------\nUsage\n----------\n{prefix}receivePM - Toggles the private message bridge between osu! and discord\n\n" +
			  "----------\nAliases\n----------\nThere are no aliases.",  
			  true,
			  "receivePM");
	}

	@Override
	public void onCommand(MessageReceivedEvent e, String[] args){
		if(!e.isFromType(ChannelType.PRIVATE)) return;
		Configuration cfg = new Configuration(new File("login.txt"));
		
		boolean toggled = true;
		
		if(cfg.getStringList("yield-pms").contains(e.getAuthor().getId())){
			cfg.removeFromStringList("yield-pms", e.getAuthor().getId(), true);
			IRCChatListener.pmList.remove(e.getAuthor().getId());
			toggled = false;
		}else{
			cfg.appendToStringList("yield-pms", e.getAuthor().getId(), true);
			IRCChatListener.pmList.add(e.getAuthor().getId());
		}
		
		String message = "";
		message = toggled ? "You are now listening to osu! private messages!" :
							"You are no longer listening to osu! private messages!";
		
		Utils.infoBypass(e.getAuthor().getPrivateChannel(), message);
	}
	
}
