package me.smc.sb.discordcommands;

import java.io.File;

import me.smc.sb.utils.Configuration;
import me.smc.sb.utils.Utils;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

public class SuggestCommand extends GlobalCommand{

	public SuggestCommand(){
		super(null, 
			  " - Sends a suggestion to the developer", 
			  "{prefix}suggest\nThis command lets you send suggestions to the developer.\n\n" +
			  "----------\nUsage\n----------\n{prefix}suggest {suggestion} - Sends a suggestion to the developer\n\n" + 
			  "----------\nAliases\n----------\nThere are no aliases.",  
			  true, 
			  "suggest");
	}

	@Override
	public void onCommand(MessageReceivedEvent e, String[] args){
		Utils.deleteMessage(e.getChannel(), e.getMessage());
		if(!Utils.checkArguments(e, args, 1)) return;
		
		Configuration cfg = new Configuration(new File("suggestions.txt"));
		
		String suggestion = "";
		for(String arg : args)
			suggestion += " " + arg;
		
		String suggestionStr = Utils.getDate() + " Suggestion by " + e.getAuthor().getName() + "#" + e.getAuthor().getDiscriminator() + 
							   " (" + e.getAuthor().getId() + ") - " + suggestion.substring(1);
		
		cfg.appendToStringList("suggestions", suggestionStr, true);
		Utils.info(e.getChannel(), "Your suggestion has been sent!");
		Utils.infoBypass(e.getJDA().getUserById("91302128328392704").openPrivateChannel().complete(), suggestionStr);
	}

}
