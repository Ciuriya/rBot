package me.Smc.sb.commands;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

import me.Smc.sb.perm.Permissions;
import me.Smc.sb.utils.Utils;
import me.itsghost.jdiscord.events.UserChatEvent;

public class Halt{

	public static HashMap<String, Boolean> stopCommands = new HashMap<String, Boolean>();
	
	@SuppressWarnings("deprecation")
	public static void execute(UserChatEvent e){
		e.getMsg().deleteMessage();
		if(!Permissions.hasPerm(e.getUser(), Permissions.MANAGE_MESSAGES)) return;
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
		Utils.info(e.getGroup(), e.getUser().getUser(), " has halted all running commands on this server!");
	}
	
}
