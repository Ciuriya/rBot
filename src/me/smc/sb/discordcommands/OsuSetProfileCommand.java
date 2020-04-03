package me.smc.sb.discordcommands;

import java.io.File;

import me.smc.sb.utils.Configuration;
import me.smc.sb.utils.Utils;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class OsuSetProfileCommand extends GlobalCommand{
	
	public static Configuration config;
	
	public OsuSetProfileCommand() {
		super(null, 
				" - Lets you set your osu profile to your discord account", 
				"{prefix}osusetprofile\nThis command lets you set your osu! account to your discord\n\n" +
				"----------\nUsage\n----------\n{prefix}osusetprofile {player} - Sets the profile to your discord \n\n" + 
			    "----------\nAliases\n----------\n{prefix}osuset", 
			    true, 
				"osusetprofile", "osuset");
		
		config = new Configuration(new File("osuprofiles.txt"));
	}

	@Override
	public void onCommand(MessageReceivedEvent e, String[] args) {
		Utils.deleteMessage(e.getChannel(), e.getMessage());
		if(!Utils.checkArguments(e, args, 1)) return;
		
		String player = "";
		
		for(int i = 0; i < args.length; i++)
			player += " " + args[i];
		
		player = player.substring(1);
		
		String playerId = Utils.getOsuPlayerIdFast(player);
		String osuProfile = config.getValue(e.getAuthor().getId());
		
		if(osuProfile.equalsIgnoreCase(playerId)) {
			Utils.infoBypass(e.getChannel(), "Your osu! profile is already set to this player!");
			return;
		}
		
		config.writeValue(e.getAuthor().getId(), playerId);
		
		Utils.infoBypass(e.getChannel(), "Your osu! profile was set to " + player);
	}
}
