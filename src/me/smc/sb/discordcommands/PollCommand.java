package me.smc.sb.discordcommands;

import me.smc.sb.utils.Utils;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

public class PollCommand extends GlobalCommand{

	public PollCommand(){
		super(null, 
			  " - Allows users to run polls in the server", 
			  "{prefix}poll\nThis command allows creation of custom polls (maximum 3 per server, default time limit 1 hour)\n\n" +
			  "----------\nUsage\n----------\n{prefix}poll start {name} option1,option2,option3 (time before close, ex: 60m) - Creates a poll with the specified options and time limit\n" + 
			  "{prefix}poll end {name} - Ends the poll and posts the results\n" +
			  "{prefix}poll list - Lists every poll running on the server\n" +
			  "{prefix}poll status {name} - Sends info about the poll (time left, votes, options, etc.)\n" +
			  "{prefix}poll vote {name} {option} - Adds a vote for the option in the poll (or changes the vote)\n\n" +
		      "----------\nAliases\n----------\nThere are no aliases.",  
			  false, 
			  "poll");
	}
	
	@Override
	public void onCommand(MessageReceivedEvent e, String[] args){
		Utils.deleteMessage(e.getChannel(), e.getMessage());
		if(!Utils.checkArguments(e, args, 1)) return;
		
		switch(args[0].toLowerCase()){
			case "start":
				
				break;
		}
	}
}
