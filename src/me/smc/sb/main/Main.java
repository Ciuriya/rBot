package me.smc.sb.main;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;

import org.pircbotx.PircBotX;
import org.pircbotx.cap.EnableCapHandler;

import me.smc.sb.communication.IncomingRequest;
import me.smc.sb.communication.Server;
import me.smc.sb.irccommands.IRCCommand;
import me.smc.sb.listeners.IRCChatListener;
import me.smc.sb.listeners.Listener;
import me.smc.sb.multi.Tournament;
import me.smc.sb.utils.Configuration;
import me.smc.sb.utils.Log;
import me.smc.sb.utils.OsuAPIRegulator;
import me.smc.sb.utils.TwitchRegulator;
import net.dv8tion.jda.JDA;
import net.dv8tion.jda.JDABuilder;

public class Main{
	
	//read suggestions
	//!createlist {list name} !addlist {list name} {text} !listprint {list name}
	//clean specific user
	//yt stats
	//cooldown on commands instead of pure lockdown IMPORTANT
	
	private static String discordToken, osuUser, osuPassword;
	public static JDA api;
	public static PircBotX ircBot = null;
	public static PircBotX twitchBot = null;
	public static HashMap<String, Configuration> serverConfigs;
	public static final double version = 0.01;
	public static int messagesReceivedThisSession = 0, messagesSentThisSession = 0, commandsUsedThisSession = 0;
	public static long bootTime = 0;
	public static Server server;
	public static Connection tourneySQL, rpgSQL;
	public static OsuAPIRegulator osuRequestManager;
	public static TwitchRegulator twitchRegulator;
	public static String defaultPrefix = "~/";
	
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
		
		setupSQL();
	
		serverConfigs = new HashMap<String, Configuration>();
		
		Configuration login = new Configuration(new File("login.txt"));
		discordToken = login.getValue("discordtoken");
		osuUser = login.getValue("osuuser");
		osuPassword = login.getValue("osupass");
		
		login();
		
		IncomingRequest.registerRequests();
		server = new Server(login.getValue("STwebIP"), 
				            login.getInt("STwebPortIn"),
				            login.getInt("STwebPortOut"));
		
		Tournament.loadTournaments();
		
		IRCCommand.registerCommands();
		
		osuRequestManager = new OsuAPIRegulator();
		
		twitchRegulator = new TwitchRegulator();
		
		new Thread(new Runnable(){ //osu irc
			public void run(){
				loadBot(ircBot, new org.pircbotx.Configuration.Builder<PircBotX>()
					    .setName(osuUser)
					    .setServer("irc.ppy.sh", 6667)
					    .setServerPassword(osuPassword)
					    .addListener(new IRCChatListener())
					    .setAutoReconnect(true)
					    .buildConfiguration(), false);
			}
		}).start();
		
		new Thread(new Runnable(){ //twitch irc
			public void run(){
				loadBot(twitchBot, new org.pircbotx.Configuration.Builder<PircBotX>()
					    .setName(login.getValue("twitch-user"))
					    .setServer("irc.chat.twitch.tv", 6667)
					    .setServerPassword("oauth:" + login.getValue("twitch-oauth"))
					    .addListener(new IRCChatListener())
					    .setAutoReconnect(true)
					    .setAutoNickChange(false)
					    .setCapEnabled(true)
					    .addCapHandler(new EnableCapHandler("twitch.tv/membership"))
					    .buildConfiguration(), true);
			}
		}).start();
	}
	
	private void login(){
		try{
			Listener l = new Listener();
			api = new JDABuilder().setBotToken(discordToken).addListener(l).buildAsync();
		}catch(Exception e){
			Log.logger.log(Level.INFO, e.getMessage(), e);
			return;
		}
	}
	
	private void loadBot(PircBotX bot, org.pircbotx.Configuration<PircBotX> config, boolean twitch){
		if(bot != null){
			bot.stopBotReconnect();
			
			while(bot.isConnected())
				try{Thread.sleep(100);
				}catch(InterruptedException e){}
			
			bot = null;
		}
		
		bot = new PircBotX(config);
		
		if(twitch) twitchBot = bot;
		else ircBot = bot;
		
		try{
			bot.startBot();
			loadBot(bot, config, twitch);
		}catch(Exception e){
			Log.logger.log(Level.SEVERE, "Could not start bot!");
			Log.logger.log(Level.SEVERE, e.getMessage(), e);
		}
	}
	
	private void setupSQL(){
		String tUrl = "jdbc:mysql://localhost/Tournament_DB";
		String rUrl = "jdbc:mysql://localhost/DRPG";
		String pass = new Configuration(new File("login.txt")).getValue("rootPass");
		
		try{
			Class.forName("com.mysql.jdbc.Driver");
			tourneySQL = DriverManager.getConnection(tUrl, "root", pass);
			rpgSQL = DriverManager.getConnection(rUrl, "root", pass);
		}catch(Exception e){
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
 		if(!Main.serverConfigs.containsKey(server)) return defaultPrefix;
 		
 		String prefix = Main.serverConfigs.get(server).getValue("command-prefix");
 		
		if(prefix == "") return defaultPrefix;
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
}
