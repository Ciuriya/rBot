package me.smc.sb.discordcommands;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

import me.itsghost.jdiscord.events.UserChatEvent;
import me.smc.sb.perm.Permissions;
import me.smc.sb.utils.Utils;

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
	public void onCommand(UserChatEvent e, String[] args){
		e.getMsg().deleteMessage();
		ArrayList<Thread> threads = Command.threads.get(e.getServer().getId());
		if(threads != null && !threads.isEmpty()){
			for(Thread t : threads)
				t.stop();
			Command.threads.get(e.getServer().getId()).clear();
		}
		Timer t = new Timer();
		stopCommands.put(e.getServer().getId(), true);
		t.schedule(new TimerTask(){
			public void run(){
				stopCommands.put(e.getServer().getId(), false);
			}
		}, 2000);
		Utils.info(e.getGroup(), "All running commands on this server were halted!");
	}
	
}
