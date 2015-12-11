package me.smc.sb.main;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;

import me.itsghost.jdiscord.DiscordAPI;
import me.itsghost.jdiscord.DiscordBuilder;
import me.itsghost.jdiscord.exception.BadUsernamePasswordException;
import me.itsghost.jdiscord.exception.DiscordFailedToConnectException;
import me.itsghost.jdiscord.exception.NoLoginDetailsException;
import me.smc.sb.listeners.Listener;
import me.smc.sb.utils.Configuration;
import me.smc.sb.utils.Log;

public class Main{
	//set perm for commands
	//read suggestions
	//edit com?
	//!createlist {list name} !addlist {list name} {text} !listprint {list name}
	//clean specific user
	//yt stats
	//refactor commands
	//cooldown on commands instead of pure lockdown IMPORTANT
	//add recent plays to osu shit?
	//better help
	
	private static String email, password;
	public static DiscordAPI api;
	public static HashMap<String, Configuration> serverConfigs;
	public static final double version = 0.01;
	public static int messagesReceivedThisSession = 0, messagesSentThisSession = 0, commandsUsedThisSession = 0;
	public static long bootTime = 0;
	
	public static void main(String[] args){
		bootTime = System.currentTimeMillis();
		Log.init(new File(".").getAbsolutePath());
		
		writeCodes(0); //bot is running
		Timer t = new Timer();
		t.scheduleAtFixedRate(new TimerTask(){
			public void run(){
				keepAlive(); //for the bash script
			}
		}, 0, 5000);
	
		serverConfigs = new HashMap<String, Configuration>();
		
		Configuration login = new Configuration(new File("login.txt"));
		email = login.getValue("user");
		password = login.getValue("pass");
		
		try{
			api = new DiscordBuilder(email, password).build().login();
		}catch(Exception e){
			e.printStackTrace();
			return;
		}
		login();
		
		checkForDisconnections();
	}
	
	public static void login(){
		api.getEventManager().registerListener(new Listener(api));
		api.setAllowLogMessages(false);
	}
	
	public static void stop(int code){
		writeCodes(code);
		api.stop();
		System.exit(0);
	}
	
	public static String getCommandPrefix(String server){
		if(!Main.serverConfigs.containsKey(server)) return "~/";
		else return Main.serverConfigs.get(server).getValue("command-prefix");
	}
	
	public static void keepAlive(){
		File f = new File("keepalive.txt");
		write("" + System.currentTimeMillis(), f);
	}
	
	public static void write(String str, File f){
		BufferedWriter bw = null;
		try{
			bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f), "UTF-8"));
			bw.write(str);
		}catch(Exception e){}
		finally{
			try{if(bw != null) bw.close(); 
			}catch(IOException e){e.printStackTrace();}
		}
	}
	
	public static void writeCodes(int retCode){
		File f = new File("codes.txt");
		write(retCode + "", f);
	}
	
	public static void checkForDisconnections(){
		while(true){
	        try{
	            Field requestManager = api.getClass().getDeclaredField("requestManager");
	            requestManager.setAccessible(true);
	            Object requestManagerObj = requestManager.get(api);

	            Field socketClient = requestManagerObj.getClass().getDeclaredField("socketClient");
	            socketClient.setAccessible(true);
	            Object socketClientObj = socketClient.get(requestManagerObj);

	            Field readyPoll = socketClientObj.getClass().getDeclaredField("readyPoll");
	            readyPoll.setAccessible(true);
	            Object readyPollObj = readyPoll.get(socketClientObj);

	            Field thread = readyPollObj.getClass().getDeclaredField("thread");
	            thread.setAccessible(true);
	            Object threadObj = thread.get(readyPollObj);
	            if(threadObj instanceof Thread){
	            	Thread t = (Thread) threadObj;
	            	t.join();
	            	Log.logger.log(Level.INFO, "jDiscord lost connection! Logging back in...");
	                while(true){
	                    try{
	                        api = new DiscordBuilder("username", "password").build().login();
	                        login();
	                        break;
	                    }catch(NoLoginDetailsException e){
	                        Log.logger.log(Level.INFO, "No login details provided! Please give an email and password in the config file.");
	                        System.exit(0);
	                    }catch(BadUsernamePasswordException e){
	                    	Log.logger.log(Level.INFO, "It seems that our password has changed since last login.");
	                        System.exit(0);
	                    }catch(DiscordFailedToConnectException e){
	                    	Log.logger.log(Level.INFO, "We failed to connect to the Discord API. Do you have internet connection?"); 
	                        Thread.sleep(10000);
	                    }
	                }
	            }
	        }catch(Exception ex){
	        	Log.logger.log(Level.INFO, "If you see this message, please report it to the developer. Reflection failure on disconnection checks!");
	            ex.printStackTrace();
	        }	
		}
	}
	
	
}
