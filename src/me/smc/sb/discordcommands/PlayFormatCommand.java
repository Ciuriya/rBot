package me.smc.sb.discordcommands;

import me.smc.sb.perm.Permissions;
import me.smc.sb.tracking.PlayFormat;
import me.smc.sb.tracking.TrackingGuild;
import me.smc.sb.utils.Utils;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class PlayFormatCommand extends GlobalCommand{

	public PlayFormatCommand(){
		super(Permissions.MANAGE_MESSAGES, 
			  " - Allows customization of the server's osu!track score post format", 
			  "{prefix}playformat\nThis command pings the specified website\n\n" +
			  "----------\nUsage\n----------\n{prefix}playformat set {format} - Sets the server's format to the specified format if it exists.\n" + 
			  "{prefix}playformat list - Lists all existing play formats.\n\n" +
		      "----------\nAliases\n----------\nThere are no aliases.",  
			  false, 
			  "playformat");
	}
	
	@Override
	public void onCommand(MessageReceivedEvent e, String[] args){
		Utils.deleteMessage(e.getChannel(), e.getMessage());
		if(!Utils.checkArguments(e, args, 1)) return;
		
		switch(args[0].toLowerCase()){
			case "set":
				if(!Utils.checkArguments(e, args, 2)) return;
				
				PlayFormat toSet = PlayFormat.get(args[1]);
				
				if(toSet != null){
					TrackingGuild.get(e.getGuild().getId()).setPlayFormat(toSet.getName());
					
					Utils.info(e.getChannel(), "Changed the server's play format to " + toSet.getName() + "!");
				}else{
					Utils.info(e.getChannel(), "Could not find play format!");
				}
				
				
				break;
			case "list":
				String text = "```diff\n- List of existing play formats\n";
				
				for(PlayFormat toList : PlayFormat.registeredFormats){
					text += "\n+ " + toList.getName().substring(0, 1).toUpperCase() + toList.getName().substring(1);
				}
				
				Utils.info(e.getChannel(), text + "```");
				
				break;
		}
	}
	
}
