package me.smc.sb.discordcommands;

import java.util.ArrayList;

import me.smc.sb.perm.Permissions;
import me.smc.sb.utils.Utils;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

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
	public void onCommand(MessageReceivedEvent e, String[] args){
		Utils.deleteMessage(e.getChannel(), e.getMessage());
		if(args.length >= 1 && args[0].equalsIgnoreCase("true")){
			final String instruction = "To synchronize the song to the lyrics, please play the long version and start on my mark.{delay=1000}3{delay=1000}2{delay=1000}1{delay=1000}NOW!{delay=1900}Are you ready?{delay=357}-y (x63){delay=22500}Are you ready?{delay=357}-y (x62){delay=22100}ADRENALINE IS PUMPING{delay=2700}ADRENALINE IS PUMPING{delay=2500}GENERATOR{delay=2500}AUTOMATIC LOVER{delay=3500}ATOMIC{delay=1400}ATOMIC{delay=1200}OVERDRIVE{delay=2000}BLOCKBUSTER{delay=3000}BRAINPOWER{delay=3800}CALL ME A LEADER{delay=1600}COCAINE{delay=3100}DON'T YOU TRY IT{delay=1900}DON'T YOU TRYYY IT{delay=3600}INNOVATOR{delay=2500}KILLER MACHINE{delay=3000}THERE'S NO FAITH{delay=2400}TAKE CONTROL{delay=3200}BRAINPOWER{delay=1800}LET{delay=200}THE{delay=200}BASS{delay=200}KICK{delay=400}O-oooooooooo AAAAE-A-A-I-A-U- JO-oooooooooooo AAE-O-A-A-U-U-A- E-eee-ee-eee AAAAE-A-E-I-E-A- JO-ooo-oo-oo-oo EEEEO-A-AAA-AAAA";
			
			Thread t = new Thread(new Runnable(){
				@SuppressWarnings("deprecation")
				public void run(){
					StringBuilder msg = new StringBuilder();
					String[] split = instruction.split("\\{");
					
					for(String str : split){
						if(str.contains("}")){
							Command.convertTag(e, str.split("}")[0], msg, "brainpower", e.getGuild().getId());
							msg = new StringBuilder();
							
							if(str.split("}").length > 1)
								msg.append(str.split("}")[1]);
						}else msg.append(str);	
					}
					
					String m = msg.toString();
					
					if(m.startsWith(" "))
						m = Utils.removeStartSpaces(m);
					
					Utils.infoBypass(e.getChannel(), m);
					
					ArrayList<Thread> sThreads = new ArrayList<Thread>();
					if(Command.threads.containsKey(e.getGuild().getId())) sThreads = Command.threads.get(e.getGuild().getId());
					sThreads.remove(Thread.currentThread());
					Command.threads.put(e.getGuild().getId(), sThreads);
					
					Thread.currentThread().stop();
				}
			});
			
			ArrayList<Thread> sThreads = new ArrayList<Thread>();
			if(Command.threads.containsKey(e.getGuild().getId())) sThreads = Command.threads.get(e.getGuild().getId());
			sThreads.add(t);
			Command.threads.put(e.getGuild().getId(), sThreads);
			t.start();
		}else Utils.infoBypass(e.getChannel(), "O-oooooooooo AAAAE-A-A-I-A-U- JO-oooooooooooo AAE-O-A-A-U-U-A- E-eee-ee-eee AAAAE-A-E-I-E-A- JO-ooo-oo-oo-oo EEEEO-A-AAA-AAAA");
	}
	
}
