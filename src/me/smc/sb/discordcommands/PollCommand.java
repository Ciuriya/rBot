package me.smc.sb.discordcommands;

import java.util.ArrayList;
import java.util.List;

import me.smc.sb.perm.Permissions;
import me.smc.sb.polls.Option;
import me.smc.sb.polls.Poll;
import me.smc.sb.utils.Utils;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class PollCommand extends GlobalCommand{

	public PollCommand(){
		super(null, 
			  " - Allows users to run polls in the server", 
			  "{prefix}poll\nThis command allows creation of custom polls (maximum 3 per server, default time limit 1 hour)\n\n" +
			  "----------\nUsage\n----------\n{prefix}poll start {name} option1,option2,option3 {{(time before close, ex: 60m or 1y5M21d)}} - Creates a poll with the specified options and time limit\n" + 
			  "{prefix}poll end {name} - Ends the poll and posts the results\n" +
			  "{prefix}poll list - Lists every poll running on the server\n" +
			  "{prefix}poll setchannel - Sets the result channel for polls\n" +
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
			case "create": case "new":
			case "start": startPoll(e, args); break;
			case "stop":
			case "end": endPoll(e, args); break;
			case "list": listPolls(e); break;
			case "setchannel": setResultChannel(e); break;
			case "stat": case "stats": case "results":
			case "status": statusPoll(e, args); break;
			case "v":
			case "vote": votePoll(e, args); break;
		}
	}
	
	public void startPoll(MessageReceivedEvent e, String[] args){
		if(!Permissions.hasPerm(e.getAuthor(), (TextChannel) e.getChannel(), Permissions.MANAGE_MESSAGES)){return;}
		if(args.length < 3){Utils.info(e.getChannel(), "Invalid Arguments"); return;}
		if(Poll.findPolls(e.getGuild()).size() > 3){Utils.info(e.getChannel(), "You can only have 3 polls running at once!"); return;}
		
		String name = "";
		String time = "1h";
		List<String> options = new ArrayList<>();
		
		for(int i = 1; i < args.length; i++)
			if(args[i].contains("{") && args[i].contains("}"))
				time = args[i].replace("{", "").replace("}", "");
			else if(args[i].contains(","))
				for(String split : args[i].split(","))
					options.add(split);
			else name += args[i] + " ";
		
		name = name.substring(0, name.length() - 1);
		
		if(options.size() == 0){Utils.info(e.getChannel(), "You need to add options to vote for!"); return;}
		
		long length = Utils.fromDuration(time);
		long expirationTime = System.currentTimeMillis() + length;
		
		if(time.equals("0")) expirationTime = 0;
		
		Poll poll = new Poll(e.getGuild(), e.getAuthor(), name, expirationTime, options);
		
		Utils.info(e.getChannel(), "Poll " + poll.getName() + " was created!\n" +
		(expirationTime == 0 ? "It will never expire!" : "It will expire in " + Utils.toDuration(length) + "!"));
	}
	
	public void endPoll(MessageReceivedEvent e, String[] args){
		if(!Permissions.hasPerm(e.getAuthor(), (TextChannel) e.getChannel(), Permissions.MANAGE_MESSAGES)){return;}
		if(args.length < 2){Utils.info(e.getChannel(), "Invalid Arguments"); return;}
		
		String name = "";
		
		for(int i = 1; i < args.length; i++)
			name += args[i] + " ";
		
		name = name.substring(0, name.length() - 1);
		
		Poll poll = Poll.findPoll(name, e.getGuild());
		if(poll == null){Utils.info(e.getChannel(), "Poll not found!"); return;}
		
		poll.end(e.getTextChannel());
	}
	
	public void listPolls(MessageReceivedEvent e){
		List<Poll> polls = Poll.findPolls(e.getGuild());
		if(polls.size() == 0){Utils.info(e.getChannel(), "No polls running!"); return;}
		
		String message = "```diff\n- Polls running in " + e.getGuild().getName() + "\n";
		
		for(Poll poll : polls)
			message += "\n+ " + poll.getName() + "\n- " + poll.getVoteCount() + " votes | " +
					   "Ends in " + Utils.toDuration(poll.getExpirationTime() - System.currentTimeMillis());
		
		Utils.info(e.getChannel(), message + "```");
	}
	
	public void setResultChannel(MessageReceivedEvent e){
		if(!Permissions.hasPerm(e.getAuthor(), (TextChannel) e.getChannel(), Permissions.MANAGE_MESSAGES)){return;}
		Poll.setResultChannel(e.getTextChannel());
		
		Utils.info(e.getChannel(), "Poll results will be posted in this channel!");
	}
	
	public void statusPoll(MessageReceivedEvent e, String[] args){
		if(args.length < 2){Utils.info(e.getChannel(), "Invalid Arguments"); return;}
		
		String name = "";
		
		for(int i = 1; i < args.length; i++)
			name += args[i] + " ";
		
		name = name.substring(0, name.length() - 1);
		
		Poll poll = Poll.findPoll(name, e.getGuild());
		if(poll == null){Utils.info(e.getChannel(), "Poll not found!"); return;}
		
		poll.postResults(e.getTextChannel());
	}
	
	public void votePoll(MessageReceivedEvent e, String[] args){
		if(args.length < 3){Utils.info(e.getChannel(), "Invalid Arguments"); return;}
		
		String name = "";
		
		for(int i = 1; i < args.length - 1; i++)
			name += args[i] + " ";
		
		name = name.substring(0, name.length() - 1);
		
		Poll poll = Poll.findPoll(name, e.getGuild());
		if(poll == null){Utils.info(e.getChannel(), "Poll not found!"); return;}
		
		Option option = poll.getOption(args[args.length - 1]);
		if(option == null){Utils.info(e.getChannel(), "Option not found!"); return;}
		
		boolean voted = poll.vote(option.getName(), e.getAuthor().getId());
		
		Utils.info(e.getChannel(), voted ? "Your vote has been added!" : "Could not add vote!");
	}
}
