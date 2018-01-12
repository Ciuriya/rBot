package me.smc.sb.discordcommands;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import me.smc.sb.main.Main;
import me.smc.sb.utils.Configuration;
import me.smc.sb.utils.Utils;
import net.dv8tion.jda.core.entities.Emote;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

public class Command{

	public static HashMap<String, HashMap<String, Command>> commands = new HashMap<String, HashMap<String, Command>>();
	private String name;
	private String instruction;
	private int delimiters;
	private String server;
	private String desc;
	public static HashMap<String, ArrayList<Thread>> threads = new HashMap<String, ArrayList<Thread>>();
	
	public Command(String server, String name, String instruction, int delimiters, String desc){
		this.desc = desc;
		this.name = name;
		this.instruction = instruction;
		this.delimiters = delimiters;
		
		HashMap<String, Command> serverComms = commands.get(server);
		serverComms.put(name, this);
		commands.put(server, serverComms);
		
		this.server = server;
	}
	
	public static Command findCommand(String server, String name){
		HashMap<String, Command> serverComms = commands.get(server);
		if(serverComms == null || serverComms.isEmpty()) return null;
		for(String str : serverComms.keySet())
			if(str.toLowerCase().equalsIgnoreCase(name))
				return serverComms.get(str);
		return null;
	}
	
	public String getDesc(){
		return desc;
	}
	
	public void execute(MessageReceivedEvent e){
		Thread t = new Thread(new Runnable(){
			@SuppressWarnings("deprecation")
			public void run(){
				StringBuilder msg = new StringBuilder();
				String tempInstruction = instruction;
				
				if(delimiters > 0){
					String message = e.getMessage().getContentDisplay();
					String[] dels = message.split(" ");
					String path = "cmd-" + name;
					
					if(dels.length < delimiters + 1){
						Utils.error(e.getChannel(), e.getAuthor(), " Invalid arguments!");
						return;
					}
					
					for(int i = 1; i < delimiters + 1; i++) path += "|||" + dels[i];
					
					Configuration cfg = Main.serverConfigs.get(server);
					tempInstruction = cfg.getValue(path);
				}
				
				if(tempInstruction == ""){
					Utils.error(e.getChannel(), e.getAuthor(), " Invalid arguments!");
					return;
				}
				
				Main.commandsUsedThisSession++;
				String[] split = tempInstruction.split("\\{");
				
				for(String str : split){
					if(str.contains("}")){
						String tag = str.split("}")[0];
						convertTag(e, str.split("}")[0], msg, name, server);
						
						if(tag.startsWith("delay=")) msg = new StringBuilder();
						
						if(str.split("}").length > 1)
							msg.append(str.split("}")[1]);
						
					}else msg.append(str);	
				}
				
				String m = msg.toString();
				
				if(m.startsWith(" "))
					m = Utils.removeStartSpaces(m);
				
				Utils.infoBypass(e.getChannel(), fillInEmotes(e, m));
				
				ArrayList<Thread> sThreads = new ArrayList<Thread>();
				if(threads.containsKey(e.getGuild().getId())) sThreads = threads.get(e.getGuild().getId());
				sThreads.remove(Thread.currentThread());
				threads.put(e.getGuild().getId(), sThreads);
				
				Thread.currentThread().stop();
			}
		});
		
		ArrayList<Thread> sThreads = new ArrayList<Thread>();
		if(threads.containsKey(e.getGuild().getId())) sThreads = threads.get(e.getGuild().getId());
		sThreads.add(t);
		threads.put(e.getGuild().getId(), sThreads);
		
		t.start();
	}
	
	public int getDelimiterCount(){
		return delimiters;
	}
	
	public void setDelimiter(String[] dels, String instruction){
		if(delimiters > 0 && dels.length >= delimiters){
			Configuration cfg = Main.serverConfigs.get(server);
			String path = "cmd-" + name;
			
			for(int i = 0; i < delimiters; i++) path += "|||" + dels[i];
			
			cfg.writeValue(path, instruction);
		}
	}
	
	public void setDescription(String description){
		this.desc = description;
	}
	
	public void setInstructions(String instructions){
		this.instruction = instructions;
	}
	
	public String getServer(){
		return server;
	}
	
	public static void convertTag(MessageReceivedEvent e, String tag, StringBuilder msg, String name, String server){
		switch(tag){
			case "user": msg.append(e.getAuthor().getAsMention()); return;
			case "increment": 
				Configuration cfg = Main.serverConfigs.get(server);
				int incNum = cfg.getInt("cmd-" + name + "-increment") + 1;
				
				msg.append(incNum + "");
				cfg.writeValue("cmd-" + name + "-increment", incNum);
				return;
			case "nextdel": return;
			default: break;
		}
		
		if(tag.startsWith("delay=")){
			int length = Utils.stringToInt(tag.replace("delay=", ""));
			Utils.infoBypass(e.getChannel(), msg.toString());
			
			try{
				Thread.sleep(length);
			}catch(Exception e1){
				e1.printStackTrace();
			}
		}else if(tag.startsWith("random=")){
			int random = new Random().nextInt(Utils.stringToInt(tag.replace("random=", "")) + 1);
			msg.append(random + "");
		}else if(tag.startsWith("mention=")){
			msg.append(e.getGuild().getMembers()
			   .stream()
			   .filter(x -> x != null && ((Member) x).getUser() != null &&
			   				((Member) x).getUser().getName() != null && ((Member) x).getNickname() != null &&
			   				(((Member) x).getUser().getName().contains(tag.replace("mention=", "")) ||
					   		((Member) x).getNickname().contains(tag.replace("mention=", "")) || 
					   		((Member) x).getUser().getId().equals(tag.replace("mention=", ""))))
			   .findFirst().get().getAsMention());
		}
	}
	
	private String fillInEmotes(MessageReceivedEvent e, String msg){
		String fixedMsg = "";
		
		for(String split : msg.split(" ")){
			if(split.startsWith(":") && split.endsWith(":")){
				List<Emote> emotes = e.getJDA().getEmotesByName(split.replaceAll(":", ""), true);
				
				boolean added = false;
				
				if(!emotes.isEmpty()){
					for(Emote emote : emotes)
						if(emote.canInteract(e.getJDA().getSelfUser(), e.getChannel())){
							fixedMsg += " " + emotes.get(0).getAsMention();
							added = true;
							
							break;
						}		
				}
				if(!added) fixedMsg += " " + split;
			} else fixedMsg += " " + split;
		}
		
		return fixedMsg.substring(1);
	}
	
	public void save(){
		Configuration cfg = Main.serverConfigs.get(server);
		ArrayList<String> list = cfg.getStringList("commands");
		if(!list.contains(name)){
			list.add(name);
			cfg.writeStringList("commands", list, true);
		}
		if(desc != null && desc != "") cfg.writeValue("cmd-" + name + "-desc", desc);
		if(delimiters > 0) cfg.writeValue("cmd-" + name + "-del", delimiters);
		else cfg.convertAndWriteValue("cmd-" + name, instruction);
	}
	
	public void delete(){
		Configuration cfg = Main.serverConfigs.get(server);
		ArrayList<String> list = cfg.getStringList("commands");
		if(list.contains(name)){
			list.remove(name);
			cfg.writeStringList("commands", list, true);
		}
		if(delimiters > 0){
			for(String line : new ArrayList<String>(cfg.getLines()))
				if(line.startsWith("cmd-" + name))
					cfg.deleteKey(line.split(":")[0]);
		}else cfg.deleteKey("cmd-" + name);
		cfg.deleteKey("cmd-" + name + "-increment");
		commands.get(server).remove(name);
	}
	
	public static void loadCommands(String server){
		Configuration cfg = Main.serverConfigs.get(server);
		commands.put(server, new HashMap<String, Command>());
		ArrayList<String> list = cfg.getStringList("commands");
		if(!list.isEmpty())
			for(String str : list){
				String desc = cfg.getValue("cmd-" + str + "-desc");
				if(cfg.getInt("cmd-" + str + "-del") > 0){
					int amount = cfg.getInt("cmd-" + str + "-del");
					new Command(server, str, "", amount, desc);
				}else{
					String value = cfg.getValue("cmd-" + str);
					
					if(value.equals("list")){
						value = "";
						List<String> values = cfg.getStringList("cmd-" + str);
						
						if(values.size() > 0)
							for(int i = 0; i < values.size(); i++){
								value += values.get(i);
								
								if(i + 1 < values.size()) value += "\n";
							}
					}
					
					new Command(server, str, value, 0, desc);
				}
			}
	}
	
}
