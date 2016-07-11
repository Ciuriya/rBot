package me.smc.sb.discordcommands;

import me.smc.sb.drpg.RPGHelpGuide;
import me.smc.sb.drpg.RPGTutorial;
import me.smc.sb.main.Main;
import me.smc.sb.perm.Permissions;
import me.smc.sb.utils.Configuration;
import me.smc.sb.utils.Utils;
import net.dv8tion.jda.entities.TextChannel;
import net.dv8tion.jda.events.message.MessageReceivedEvent;

public class RPGCommand extends GlobalCommand{

	public RPGCommand(){
		super(null, 
			  " - The home of the discord RPG", 
			  "{prefix}rpg\nThis command handles everything RPG related\n\n" +
			  "----------\nUsage\n----------\n{prefix}rpg {subcommand} - Handles every aspect of the RPG\n" +
			  "{prefix}rpg install - Adds rpg at the start of every command sent in this channel ({prefix}rpg install -> {prefix}install)\n" +
			  "{prefix}rpg uninstall - Removes the automatic RPG addition outlined above\n" +
			  "{prefix}rpg help - An in-depth help guide for the RPG\n\n" + 
		      "----------\nAliases\n----------\nThere are no aliases.",  
			  true, 
			  "rpg");
	}
	
	@Override
	public void onCommand(MessageReceivedEvent e, String[] args){
		Utils.deleteMessage(e.getChannel(), e.getMessage());
		if(!Utils.checkArguments(e, args, 1)) return;
		
		switch(args[0].toLowerCase()){
			case "install":
				String cmdPrefix = Main.getCommandPrefix(e.getGuild().getId());
				
				if(e.getChannel() instanceof TextChannel){
					if(!Permissions.hasPerm(e.getAuthor(), e.getTextChannel(), Permissions.MANAGE_CHANNELS)) return;
					Configuration cfg = Main.serverConfigs.get(e.getGuild().getId());
					
					if(!cfg.getStringList("rpg-enabled-channels").contains(e.getTextChannel().getId())){
						cfg.appendToStringList("rpg-enabled-channels", e.getTextChannel().getId(), true);
						Utils.info(e.getChannel(), "This channel now automatically appends rpg to commands!" +
												   " (" + cmdPrefix + "rpg install) -> (" + cmdPrefix + "install)\n" +
												   "Use " + cmdPrefix + "uninstall to remove this!");
					}else
						Utils.info(e.getChannel(), "This feature is already installed in this channel, use " + cmdPrefix + "uninstall if you wish to remove it!");
				}else Utils.info(e.getChannel(), "Sorry, you cannot install this feature in PM!\nHowever, you do not need a prefix for PM.");
				
				break;
			case "uninstall":
				if(e.getChannel() instanceof TextChannel){
					if(!Permissions.hasPerm(e.getAuthor(), e.getTextChannel(), Permissions.MANAGE_CHANNELS)) return;
					Configuration cfg = Main.serverConfigs.get(e.getGuild().getId());
					
					if(cfg.getStringList("rpg-enabled-channels").contains(e.getTextChannel().getId())){
						cfg.removeFromStringList("rpg-enabled-channels", e.getTextChannel().getId(), true);
						
						Utils.info(e.getChannel(), "The RPG feature has been removed from this channel!");
					}
				}
				
				break;
			case "help":
				String arg = "";
				
				if(args.length > 1) arg = args[1].toLowerCase();
				
				new RPGHelpGuide(e, arg);
				break;
			case "tutorial":
				new RPGTutorial(e);
				
				break;
			default: 
				break;
		}
	}
	
}
