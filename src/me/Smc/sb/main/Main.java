package me.Smc.sb.main;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

import me.Smc.sb.listeners.Listener;
import me.Smc.sb.utils.Configuration;
import me.itsghost.jdiscord.DiscordAPI;
import me.itsghost.jdiscord.DiscordBuilder;
import me.itsghost.jdiscord.exception.BadUsernamePasswordException;
import me.itsghost.jdiscord.exception.DiscordFailedToConnectException;
import me.itsghost.jdiscord.exception.NoLoginDetailsException;

public class Main{
	//logging all commands
	//set perm for commands
	//read suggestions
	//edit com?
	//!createlist {list name} !addlist {list name} {text} !listprint {list name}
	//clean specific user
	//yt stats
	//refactor commands
	//cooldown on commands instead of pure lockdown
	//add recent plays to osu shit?
	
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
		keepAlive();
		globalCommandsConfig = new Configuration(new File("global-commands.txt"));
		serverConfigs = new HashMap<String, Configuration>();
		try{
			api = new DiscordBuilder(email, password).build().login();
		}catch(Exception e){
			e.printStackTrace();
			return;
		}
		login();
		Timer t = new Timer();
		t.scheduleAtFixedRate(new TimerTask(){
			public void run(){
				keepAlive();
			}
		}, 0, 5000);
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
		String prefix = Main.serverConfigs.get(server).getValue("command-prefix");
		if(prefix == "") return "~/";
		else return prefix;
	}
	
	public static void keepAlive(){
		File f = new File("keepalive.txt");
		BufferedWriter bw = null;
		try{
			bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f), "UTF-8"));
			bw.write("" + System.currentTimeMillis());
		}catch(Exception e){}
		finally{
			try{if(bw != null) bw.close(); 
			}catch(IOException e){e.printStackTrace();}
		}
	}
	
	public static void writeCodes(int retCode){
		File f = new File("codes.txt");
		BufferedWriter bw = null;
		try{
			bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f), "UTF-8"));
			bw.write(retCode + "");
		}catch(Exception e){}
		finally{
			try{if(bw != null) bw.close(); 
			}catch(IOException e){e.printStackTrace();}
		}
	}
	
	public static void checkForDisconnections(){ //change the messages to logging some day
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
	            	System.out.println("jDiscord lost connection! Logging back in...");
	                while(true){
	                    try{
	                        api = new DiscordBuilder("username", "password").build().login();
	                        login();
	                        break;
	                    }catch (NoLoginDetailsException e){
	                        System.out.println("No login details provided! Please give an email and password in the config file.");
	                        System.exit(0);
	                    }catch (BadUsernamePasswordException e){
	                        System.out.println("It seems that our password has changed since last login.");
	                        System.exit(0);
	                    }catch (DiscordFailedToConnectException e){
	                        System.out.println("We failed to connect to the Discord API. Do you have internet connection?"); 
	                        Thread.sleep(10000);
	                    }
	                }
	            }
	        }catch(Exception ex){
	            System.out.println("If you see this message, please report it to the developer. Reflection failure on disconnection checks!");
	            ex.printStackTrace();
	        }	
		}
	}
	
	
}
