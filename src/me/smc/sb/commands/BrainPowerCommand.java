package me.smc.sb.commands;

import java.util.ArrayList;

import me.itsghost.jdiscord.events.UserChatEvent;
import me.itsghost.jdiscord.message.Message;
import me.itsghost.jdiscord.message.MessageBuilder;
import me.smc.sb.main.Main;
import me.smc.sb.perm.Permissions;
import me.smc.sb.utils.Utils;

public class BrainPowerCommand extends GlobalCommand{
	
	public BrainPowerCommand(){
		super(Permissions.MANAGE_SERVER, 
			  " - Sends either the copy-pasta or the synced lyrics of the song", 
			  "{prefix}brainpower\nThis command sings the lyrics of the song brainpower. (long version)\n" + 
			  "https://www.youtube.com/watch?v=9R8aSKwTEMg\n\n----------\nUsage\n----------\n" +
			  "{prefix}brainpower - Sends the copy-pasta version\n{prefix}brainpower false - Same as the above\n" +
			  "{prefix}brainpower true - Sings the lyrics of the song\n\n" + 
			  "----------\nAliases\n----------\nThere are no aliases.", 
			  false, 
			  "brainpower");
	}

	@Override
	public void onCommand(UserChatEvent e, String[] args){
		e.getMsg().deleteMessage();
		if(args.length >= 1 && args[0].equalsIgnoreCase("true")){
			final String instruction = "To synchronize the song to the lyrics, please play the long version and start on my mark.{delay=1000}3{delay=1000}2{delay=1000}1{delay=1000}NOW!{delay=1900}Are you ready?{delay=357}-y (x63){delay=22500}Are you ready?{delay=357}-y (x62){delay=22100}ADRENALINE IS PUMPING{delay=2700}ADRENALINE IS PUMPING{delay=2500}GENERATOR{delay=2500}AUTOMATIC LOVER{delay=3500}ATOMIC{delay=1400}ATOMIC{delay=1200}OVERDRIVE{delay=2000}BLOCKBUSTER{delay=3000}BRAINPOWER{delay=3800}CALL ME A LEADER{delay=1600}COCAINE{delay=3100}DON'T YOU TRY IT{delay=1900}DON'T YOU TRYYY IT{delay=3600}INNOVATOR{delay=2500}KILLER MACHINE{delay=3000}THERE'S NO FAITH{delay=2400}TAKE CONTROL{delay=3200}BRAINPOWER{delay=1800}LET{delay=200}THE{delay=200}BASS{delay=200}KICK{delay=400}O-oooooooooo AAAAE-A-A-I-A-U- JO-oooooooooooo AAE-O-A-A-U-U-A- E-eee-ee-eee AAAAE-A-E-I-E-A- JO-ooo-oo-oo-oo EEEEO-A-AAA-AAAA";
			Thread t = new Thread(new Runnable(){
				@SuppressWarnings("deprecation")
				public void run(){
					MessageBuilder msg = new MessageBuilder();
					String[] split = instruction.split("\\{");
					for(String str : split){
						if(str.contains("}")){
							Command.convertTag(e, str.split("}")[0], msg, "brainpower", e.getServer().getId());
							msg = new MessageBuilder();
							if(str.split("}").length > 1)
								msg.addString(str.split("}")[1]);
						}else msg.addString(str);	
					}
					Message m = msg.build(Main.api);
					if(m.toString().startsWith(" "))
						m.setMessage(Utils.removeStartSpaces(m.getMessage()));
					Utils.infoBypass(e.getGroup(), m);
					ArrayList<Thread> sThreads = new ArrayList<Thread>();
					if(Command.threads.containsKey(e.getServer().getId())) sThreads = Command.threads.get(e.getServer().getId());
					sThreads.remove(Thread.currentThread());
					Command.threads.put(e.getServer().getId(), sThreads);
					Thread.currentThread().stop();
				}
			});
			ArrayList<Thread> sThreads = new ArrayList<Thread>();
			if(Command.threads.containsKey(e.getServer().getId())) sThreads = Command.threads.get(e.getServer().getId());
			sThreads.add(t);
			Command.threads.put(e.getServer().getId(), sThreads);
			t.start();
		}else Utils.infoBypass(e.getGroup(), "O-oooooooooo AAAAE-A-A-I-A-U- JO-oooooooooooo AAE-O-A-A-U-U-A- E-eee-ee-eee AAAAE-A-E-I-E-A- JO-ooo-oo-oo-oo EEEEO-A-AAA-AAAA");
	}
	
}
