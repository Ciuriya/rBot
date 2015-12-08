package me.Smc.sb.main;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.HashMap;

import me.Smc.sb.listeners.Listener;
import me.Smc.sb.utils.Configuration;
import me.itsghost.jdiscord.DiscordAPI;
import me.itsghost.jdiscord.DiscordBuilder;

public class Main{
	//logging all commands
	//set perm for commands
	//read suggestions
	//edit com?
	//!createlist {list name} !addlist {list name} {text} !listprint {list name}
	//clean specific user
	//yt stats
	
	private static final String email = "smcmaxime@gmail.com", password = "IaMaBoTwHoIsUsEfUl22";
	public static DiscordAPI api;
	public static HashMap<String, Configuration> serverConfigs;
	public static Configuration globalCommandsConfig;
	public static final double version = 0.01;
	public static int messagesThisSession = 0;
	public static long bootTime = 0;
	
	public static void main(String[] args){
		bootTime = System.currentTimeMillis();
		writeCodes(0);
		globalCommandsConfig = new Configuration(new File("global-commands.txt"));
		serverConfigs = new HashMap<String, Configuration>();
		api = new DiscordBuilder(email, password).build();
		try{
			api = api.login();
		}catch(Exception e){
			e.printStackTrace();
			return;
		}
		api.getEventManager().registerListener(new Listener(api));
		api.setAllowLogMessages(false);
	}
	
	public static void stop(int code){
		writeCodes(code);
		api.stop();
		System.exit(0);
	}
	
	public static String getCommandPrefix(String server){
		String prefix = Main.serverConfigs.get(server).getValue("command-prefix");
		if(prefix == "") return "~/";
		else return prefix;
	}
	
	public static void writeCodes(int retCode){
		File f = new File("codes.txt");
		BufferedWriter bw = null;
		try{
			bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f), "UTF-8"));
			bw.write(retCode + "");
		}catch(Exception e){}
		finally{
			try{ if(bw != null) bw.close(); 
			}catch(IOException e){e.printStackTrace();}
		}
	}
}
