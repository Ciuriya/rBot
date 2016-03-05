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

import org.pircbotx.PircBotX;

import me.smc.sb.irccommands.IRCCommand;
import me.smc.sb.listeners.IRCChatListener;
import me.smc.sb.listeners.Listener;
import me.smc.sb.multi.Tournament;
import me.smc.sb.utils.Configuration;
import me.smc.sb.utils.Log;
import me.smc.sb.utils.Server;
import me.smc.sb.utils.Utils;
import net.dv8tion.jda.JDA;
import net.dv8tion.jda.JDABuilder;

public class Main{
	
	//read suggestions
	//!createlist {list name} !addlist {list name} {text} !listprint {list name}
	//clean specific user
	//yt stats
	//cooldown on commands instead of pure lockdown IMPORTANT
	
	private static String discordEmail, discordPassword, osuUser, osuPassword;
	public static JDA api;
	public static PircBotX ircBot = null;
	public static HashMap<String, Configuration> serverConfigs;
	public static final double version = 0.01;
	public static int messagesReceivedThisSession = 0, messagesSentThisSession = 0, commandsUsedThisSession = 0;
	public static long bootTime = 0;
	private static Server server;
	
	public static void main(String[] args){
		new Main();
	}
	
	public Main(){
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
		discordEmail = login.getValue("discorduser");
		discordPassword = login.getValue("discordpass");
		osuUser = login.getValue("osuuser");
		osuPassword = login.getValue("osupass");
		
		login();
		
		server = new Server(login.getValue("STwebIP"), 
				            login.getInt("STwebPortIn"),
				            login.getInt("STwebPortOut"));
		
		Thread thread = new Thread(new Runnable(){
			public void run(){
				checkForDisconnections();
			}
		});
		thread.start();
		
		Tournament.loadTournaments();
		
		IRCCommand.registerCommands();
		loadIRC();
	}
	
	private void login(){
		try{
			Listener l = new Listener();
			api = new JDABuilder(discordEmail, discordPassword).addListener(l).buildBlocking();
			l.setAPI(api);
			Listener.loadGuilds(api);
			Utils.infoBypass(Main.api.getUserById("91302128328392704").getPrivateChannel(), "I am now logged in!"); //Sends the developer a message on login
			IRCChatListener.yieldPMs = new Configuration(new File("login.txt")).getBoolean("yield-pms");
		}catch(Exception e){
			Log.logger.log(Level.INFO, e.getMessage(), e);
			return;
		}
	}
	
	private void loadIRC(){
		if(ircBot != null){
			ircBot.stopBotReconnect();
			while(ircBot.isConnected())
				try{Thread.sleep(100);
				}catch(InterruptedException e){}
			ircBot = null;
		}
		
		ircBot = new PircBotX(new org.pircbotx.Configuration.Builder<PircBotX>()
				  .setName(osuUser)
				  .setServer("irc.ppy.sh", 6667)
				  .setServerPassword(osuPassword)
				  .addListener(new IRCChatListener())
				  .setAutoReconnect(true)
				  .buildConfiguration());
		
		try{
			ircBot.startBot();
			loadIRC();
		}catch(Exception e){
			Log.logger.log(Level.SEVERE, "Could not start irc bot!");
			Log.logger.log(Level.SEVERE, e.getMessage(), e);
		}
	}
	
	public static void stop(int code){
		server.stop();
		writeCodes(code);
		api.shutdown();
		System.exit(0);
	}
	
	public static String getCommandPrefix(String server){
 		if(!Main.serverConfigs.containsKey(server)) return "~/";
 		String prefix = Main.serverConfigs.get(server).getValue("command-prefix");
		if(prefix == "") return "~/";
		else return prefix;
	}
	
	private void keepAlive(){
		File f = new File("keepalive.txt");
		write("" + System.currentTimeMillis(), f);
	}
	
	private static void write(String str, File f){
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
	
	private static void writeCodes(int retCode){
		File f = new File("codes.txt");
		write(retCode + "", f);
	}
	
	private void checkForDisconnections(){
		while(true){
	        try{
	            Field wsc = api.getClass().getDeclaredField("client");
	            wsc.setAccessible(true);
	            Object wscObj = wsc.get(api);

	            Field thread = wscObj.getClass().getDeclaredField("keepAliveThread");
	            thread.setAccessible(true);
	            Object threadObj = thread.get(wscObj);

	            if(threadObj instanceof Thread){
	            	((Thread) threadObj).join();
	            	reconnect();
	            }
	        }catch(Exception e){
	        	Log.logger.log(Level.INFO, "Reflection failure on disconnection checks, please report this to the developer!");
	            Log.logger.log(Level.SEVERE, e.getMessage(), e);
	        }	
		}
	}
	
	private void reconnect(){
    	Log.logger.log(Level.INFO, "JDA lost connection!");
        while(true){
            try{
            	Log.logger.log(Level.INFO, "Attempting to reconnect to JDA...");
                login();
                break;
            }catch(Exception e){
                Log.logger.log(Level.INFO, "Unable to reconnect! Trying again in 30 seconds...");
                Utils.sleep(30000);
            }
        }
	}
	
}
