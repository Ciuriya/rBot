package me.smc.sb.discordcommands;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

import me.smc.sb.perm.Permissions;
import me.smc.sb.utils.Utils;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public class HaltCommand extends GlobalCommand{

	public static HashMap<String, Boolean> stopCommands = new HashMap<String, Boolean>();
	
	public HaltCommand() {
		super(Permissions.MANAGE_MESSAGES, 
			  " - Halts all running commands on the current server", 
			  "{prefix}halt\nHalts every running command on the current server.\n\n" +
			  "----------\nUsage\n----------\n{prefix}halt - Stops all running commands\n\n" + 
			  "----------\nAliases\n----------\nThere are no aliases.", 
			  false, 
			  "halt");
	}

	@SuppressWarnings("deprecation")
	@Override
	public void onCommand(MessageReceivedEvent e, String[] args){
		ArrayList<Thread> threads = Command.threads.get(e.getGuild().getId());
		if(threads != null && !threads.isEmpty()){
			for(Thread t : threads)
				t.stop();
			Command.threads.get(e.getGuild().getId()).clear();
		}
		
		Timer t = new Timer();
		stopCommands.put(e.getGuild().getId(), true);
		t.schedule(new TimerTask(){
			public void run(){
				stopCommands.put(e.getGuild().getId(), false);
			}
		}, 2000);
		
		Utils.info(e.getChannel(), "All running commands on this server were halted!");
	}
	
}
