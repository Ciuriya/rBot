package me.Smc.sb.commands;

import java.util.ArrayList;
import java.util.HashMap;

import me.Smc.sb.main.Main;
import me.Smc.sb.utils.Configuration;
import me.Smc.sb.utils.Utils;
import me.itsghost.jdiscord.events.UserChatEvent;
import me.itsghost.jdiscord.message.Message;
import me.itsghost.jdiscord.message.MessageBuilder;

public class Command{

	public static HashMap<String, HashMap<String, Command>> commands = new HashMap<String, HashMap<String, Command>>(); //perm levels in server
	private String name;
	private String instruction;
	private int delimiters;
	private String server;
	private String desc;
	private boolean global;
	private boolean adminOnly;
	public static HashMap<String, ArrayList<Thread>> threads = new HashMap<String, ArrayList<Thread>>();
	public static HashMap<String, Command> globalCommands = new HashMap<String, Command>();
	
	public Command(String server, String name, String instruction, int delimiters, String desc, boolean global, boolean adminOnly){
		this.desc = desc;
		this.name = name;
		this.global = global;
		this.adminOnly = adminOnly;
		if(!global){
			this.instruction = instruction;
			this.delimiters = delimiters;
			HashMap<String, Command> serverComms = commands.get(server);
			serverComms.put(name, this);
			commands.put(server, serverComms);
			this.server = server;
		}else globalCommands.put(name, this);
	}
	
	public boolean isGlobal(){
		return global;
	}
	
	public boolean isAdminOnly(){
		return adminOnly;
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
	
	public void execute(UserChatEvent e){
		Thread t = new Thread(new Runnable(){
			@SuppressWarnings("deprecation")
			public void run(){
				MessageBuilder msg = new MessageBuilder();
				String tempInstruction = instruction;
				if(delimiters > 0){
					String message = e.getMsg().getMessage();
					String[] dels = message.split(" ");
					String path = "cmd-" + name;
					if(dels.length < delimiters + 1){
						Utils.error(e.getGroup(), e.getUser().getUser(), " Invalid arguments!");
						return;
					}
					for(int i = 1; i < delimiters + 1; i++) path += "|||" + dels[i];
					Configuration cfg = Main.serverConfigs.get(server);
					tempInstruction = cfg.getValue(path);
				}
				if(tempInstruction == ""){
					Utils.error(e.getGroup(), e.getUser().getUser(), " Invalid arguments!");
					return;
				}
				String[] split = tempInstruction.split("\\{");
				for(String str : split){
					if(str.contains("}")){
						String tag = str.split("}")[0];
						convertTag(e, str.split("}")[0], msg, name, server);
						if(tag.startsWith("delay=")) msg = new MessageBuilder();
						if(str.split("}").length > 1)
							msg.addString(str.split("}")[1]);
					}else msg.addString(str);	
				}
				Message m = msg.build();
				if(m.toString().startsWith(" "))
					m.setMessage(Utils.removeStartSpaces(m.getMessage()));
				e.getGroup().sendMessage(m);
				ArrayList<Thread> sThreads = new ArrayList<Thread>();
				if(threads.containsKey(e.getServer().getId())) sThreads = threads.get(e.getServer().getId());
				sThreads.remove(Thread.currentThread());
				threads.put(e.getServer().getId(), sThreads);
				Thread.currentThread().stop();
			}
		});
		ArrayList<Thread> sThreads = new ArrayList<Thread>();
		if(threads.containsKey(e.getServer().getId())) sThreads = threads.get(e.getServer().getId());
		sThreads.add(t);
		threads.put(e.getServer().getId(), sThreads);
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
	
	public String getServer(){
		return server;
	}
	
	public static void convertTag(UserChatEvent e, String tag, MessageBuilder msg, String name, String server){
		switch(tag){
			case "user": msg.addUserTag(e.getUser(), e.getGroup()); return;
			case "increment": 
				Configuration cfg = Main.serverConfigs.get(server);
				int incNum = cfg.getInt("cmd-" + name + "-increment") + 1;
				msg.addString(incNum + "");
				cfg.writeValue("cmd-" + name + "-increment", incNum);
				return;
			case "nextdel": return;
			default: break;
		}
		if(tag.startsWith("delay=")){
			int length = Utils.stringToInt(tag.replace("delay=", ""));
			e.getGroup().sendMessage(msg.build());
			try{Thread.sleep(length);
			}catch(Exception e1){
				e1.printStackTrace();
			}
		}
	}
	
	public void toGlobal(UserChatEvent e){
		//are you sure?
	}
	
	public void save(){
		Configuration cfg = Main.serverConfigs.get(server);
		ArrayList<String> list = cfg.getStringList("commands");
		if(!list.contains(name)){
			list.add(name);
			cfg.writeStringList("commands", list);
		}
		if(desc != null && desc != "") cfg.writeValue("cmd-" + name + "-desc", desc);
		if(delimiters > 0) cfg.writeValue("cmd-" + name + "-del", delimiters);
		else cfg.writeValue("cmd-" + name, instruction);
	}
	
	public void delete(){
		Configuration cfg = Main.serverConfigs.get(server);
		ArrayList<String> list = cfg.getStringList("commands");
		if(list.contains(name)){
			list.remove(name);
			cfg.writeStringList("commands", list);
		}
		if(delimiters > 0){
			for(String line : new ArrayList<String>(cfg.getLines()))
				if(line.startsWith("cmd-" + name))
					cfg.deleteKey(line.split(":")[0]);
		}else cfg.deleteKey("cmd-" + name);
		cfg.deleteKey("cmd-" + name + "-increment");
		commands.get(server).remove(name);
	}
	
	public static void loadGlobalCommands(){
		Configuration cfg = Main.globalCommandsConfig;
		ArrayList<String> list = cfg.getStringList("commands");
		if(!list.isEmpty())
			for(String str : list){
				String desc = cfg.getValue(str + "-desc");
				new Command("", str, "", 0, desc, true, cfg.getStringList("bot-admin-only-commands").contains(str));
			}
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
					new Command(server, str, "", amount, desc, false, false);
				}else new Command(server, str, cfg.getValue("cmd-" + str), 0, desc, false, false);
			}
	}
	
}
