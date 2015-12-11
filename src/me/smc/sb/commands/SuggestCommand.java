package me.smc.sb.commands;

import java.io.File;

import me.itsghost.jdiscord.events.UserChatEvent;
import me.smc.sb.utils.Configuration;
import me.smc.sb.utils.Utils;

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
	public void onCommand(UserChatEvent e, String[] args){
		e.getMsg().deleteMessage();
		if(!Utils.checkArguments(e, args, 1)) return;
		Configuration cfg = new Configuration(new File("suggestions.txt"));
		
		String suggestion = "";
		for(String arg : args)
			suggestion += " " + arg;
		
		cfg.appendToStringList("suggestions", Utils.getDate() + " Suggestion by " + e.getUser().getUser().getUsername() + " - " + suggestion.substring(1));
		Utils.info(e.getGroup(), "Your suggestion has been sent!");
	}

}
