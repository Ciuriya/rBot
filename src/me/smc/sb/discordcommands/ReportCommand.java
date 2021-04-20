package me.smc.sb.discordcommands;

import java.io.File;

import me.smc.sb.utils.Configuration;
import me.smc.sb.utils.Utils;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class ReportCommand extends GlobalCommand{

	public ReportCommand(){
		super(null, 
			  " - Sends a message to the developer", 
			  "{prefix}suggest\nThis command lets you send messages to the developer.\n\n" +
			  "----------\nUsage\n----------\n{prefix}report {message} - Sends a message to the developer\n\n" + 
			  "----------\nAliases\n----------\n{prefix}suggest\n",  
			  true, 
			  "report", "suggest");
	}

	@Override
	public void onCommand(MessageReceivedEvent e, String[] args){
		if(!Utils.checkArguments(e, args, 1)) return;
		
		Configuration cfg = new Configuration(new File("reports.txt"));
		
		String report = "";
		for(String arg : args)
			report += " " + arg;
		
		String reportStr = Utils.getDate() + " Report from " + e.getAuthor().getName() + "#" + e.getAuthor().getDiscriminator() + 
							   				 " (" + e.getAuthor().getId() + ") - " + report.substring(1);
		
		cfg.appendToStringList("reports", reportStr, true);
		Utils.info(e.getChannel(), "Your message has been sent to the developer!");
		Utils.infoBypass(e.getJDA().retrieveUserById("91302128328392704").complete().openPrivateChannel().complete(), reportStr);
	}

}
