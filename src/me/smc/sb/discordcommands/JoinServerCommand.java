package me.smc.sb.discordcommands;

import java.util.Timer;
import java.util.TimerTask;

import me.smc.sb.listeners.Listener;
import me.smc.sb.utils.Utils;
import net.dv8tion.jda.events.message.MessageReceivedEvent;
import net.dv8tion.jda.utils.InviteUtil;

public class JoinServerCommand extends GlobalCommand{

	public JoinServerCommand(){
		super(null, 
			  " - Lets the bot join the requested server", 
			  "{prefix}joinserver\nThis command makes the bot join the requested server.\n\n" +
			  "----------\nUsage\n----------\n{prefix}joinserver - Makes the bot join the server\n\n" + 
			  "----------\nAliases\n----------\nThere are no aliases.",  
			  true, 
			  "joinserver");
	}

	@SuppressWarnings("deprecation")
	@Override
	public void onCommand(MessageReceivedEvent e, String[] args){
		if(!Utils.checkArguments(e, args, 1)) return;
		
		if(args[0].startsWith("https://discord.gg/"))
			args[0] = args[0].replace("https://discord.gg/", "");
		
		InviteUtil.join(InviteUtil.resolve(args[0]), e.getJDA());
		Timer t = new Timer();
		
		t.schedule(new TimerTask(){
			public void run(){
				Listener.loadGuilds(e.getJDA());
			}
		}, 2500);
	}

}
