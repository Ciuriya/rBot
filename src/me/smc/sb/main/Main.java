package me.smc.sb.main;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;

import org.pircbotx.PircBotX;
import org.pircbotx.cap.EnableCapHandler;

import me.smc.sb.charts.ChartGenerator;
import me.smc.sb.charts.ChartType;
import me.smc.sb.communication.IncomingRequest;
import me.smc.sb.communication.Server;
import me.smc.sb.discordcommands.VoiceCommand;
import me.smc.sb.irccommands.IRCCommand;
import me.smc.sb.listeners.IRCChatListener;
import me.smc.sb.listeners.Listener;
import me.smc.sb.multi.Tournament;
import me.smc.sb.parsable.ParsableValue;
import me.smc.sb.tracking.HybridRegulator;
import me.smc.sb.tracking.PlayFormat;
import me.smc.sb.utils.BanchoRegulator;
import me.smc.sb.utils.Configuration;
import me.smc.sb.utils.DiscordPlayStatusManager;
import me.smc.sb.utils.Log;
import me.smc.sb.utils.TwitchRegulator;
import me.smc.sb.utils.Utils;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.OnlineStatus;

public class Main{
	
	private static String discordToken, osuUser, osuPassword;
	public static JDA api;
	public static PircBotX ircBot = null;
	public static PircBotX twitchBot = null;
	public static HashMap<String, Configuration> serverConfigs;
	public static int messagesReceivedThisSession = 0, messagesSentThisSession = 0, commandsUsedThisSession = 0;
	public static int requestsSent = 0, highestBurstRequestsSent = 0, requestHtmlSent = 0, failedRequests = 0;
	public static int htmlScrapes = 0, osuHtmlScrapes = 0;
	public static boolean discordConnected = false;
	public static long bootTime = 0;
	public static List<Server> servers;
	public static Connection tourneySQL, rpgSQL;
	public static TwitchRegulator twitchRegulator;
	public static BanchoRegulator banchoRegulator;
	public static HybridRegulator hybridRegulator;
	public static ChartGenerator chartGenerator;
	public static String defaultPrefix = "~/";
	public static boolean debug = false;
	private int lastRequestCount = 0;
	
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
		
		ParsableValue.init();
		IncomingRequest.registerRequests();

		try{
			servers = new ArrayList<Server>();
			
			for(String server : login.getStringList("servers"))
				servers.add(new Server(server.split(":")[0],
									   Utils.stringToInt(server.split(":")[1]),
									   Utils.stringToInt(server.split(":")[2])));	
		}catch(Exception e){
			Log.logger.log(Level.INFO, e.getMessage(), e);
		}
		
		hybridRegulator = new HybridRegulator();
		
		ChartType.load();
		chartGenerator = new ChartGenerator(login.getValue("chart-apiKey"));
		
		PlayFormat.loadFormats();
		Tournament.loadTournaments();
		IRCCommand.registerCommands();
		
		Timer burstUpdate = new Timer();
		burstUpdate.scheduleAtFixedRate(new TimerTask(){
			public void run(){
				int requests = requestsSent - lastRequestCount;
				
				lastRequestCount = requestsSent;
				
				if(requests > highestBurstRequestsSent)
					highestBurstRequestsSent = requests;
				
				long uptime = System.currentTimeMillis() - Main.bootTime;
				
				if(htmlScrapes == 0 && uptime >= 120000 && Tournament.matchesRunning == 0){
					//Utils.info(api.getUserById("91302128328392704").getPrivateChannel(), "I am restarting due to the lack of html scrapes.");
					//stop(2);
				}
			}
		}, 120000, 120000);
		
		new DiscordPlayStatusManager();
		
		twitchRegulator = new TwitchRegulator();
		banchoRegulator = new BanchoRegulator();
		
		new Thread(new Runnable(){ //osu irc
			public void run(){
				loadBot(ircBot, new org.pircbotx.Configuration.Builder()
					    .setName(osuUser)
					    .addServer("irc.ppy.sh", 6667)
					    .setServerPassword(osuPassword)
					    .addListener(new IRCChatListener())
					    .setAutoReconnect(true)
					    .buildConfiguration(), false);
			}
		}).start();
		
		new Thread(new Runnable(){ //twitch irc
			public void run(){
				loadBot(twitchBot, new org.pircbotx.Configuration.Builder()
					    .setName(login.getValue("twitch-user"))
					    .addServer("irc.chat.twitch.tv", 6667)
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
			api = new JDABuilder(AccountType.BOT).setToken(discordToken).addEventListener(l).buildAsync();
		}catch(Exception e){
			Log.logger.log(Level.INFO, e.getMessage(), e);
			return;
		}
	}
	
	private void loadBot(PircBotX bot, org.pircbotx.Configuration config, boolean twitch){
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
		VoiceCommand.saveAllRadios();
		api.getPresence().setStatus(OnlineStatus.OFFLINE);
		
		if(!servers.isEmpty())
			for(Server server : servers)
				if(server != null)
					server.stop();
		
		writeCodes(code);
		api.shutdown();
		
		new Timer().schedule(new TimerTask(){
			public void run(){
				System.exit(0);
			}
		}, 1000);
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
