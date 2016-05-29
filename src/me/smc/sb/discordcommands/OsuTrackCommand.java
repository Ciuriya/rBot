package me.smc.sb.discordcommands;

import java.io.File;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;

import org.json.JSONArray;
import org.json.JSONObject;

import me.smc.sb.main.Main;
import me.smc.sb.multi.Map;
import me.smc.sb.utils.Configuration;
import me.smc.sb.utils.Log;
import me.smc.sb.utils.Utils;
import net.dv8tion.jda.MessageBuilder;
import net.dv8tion.jda.MessageHistory;
import net.dv8tion.jda.entities.Guild;
import net.dv8tion.jda.entities.Message;
import net.dv8tion.jda.entities.TextChannel;
import net.dv8tion.jda.events.message.MessageReceivedEvent;

public class OsuTrackCommand extends GlobalCommand{

	public static HashMap<String, ArrayList<String>> trackedPlayers;
	private static HashMap<String, String> lastUpdated;
	private static HashMap<String, String> lastUpdateMessageSent;
	private static HashMap<String, Thread> usersUpdating;
	private static ArrayList<Thread> allRunningThreads;
	private static int requestsPerMinute = 60;
	private static double currentRefreshRate = 0;
	private static Timer update, refresh;
	
	public OsuTrackCommand(){
		super(null, 
			  " - Lets you track osu! players", 
			  "{prefix}osutrack\nThis command lets you track osu! players' recent performance\n\n" +
		      "----------\nUsage\n----------\n{prefix}osutrack {player} ({mode={0/1/2/3}}) - Tracks or untracks the player's recent statistics for this mode\n\n" + 
		      "----------\nAliases\n----------\nThere are no aliases.", 
			  false, 
			  "osutrack");
		trackedPlayers = new HashMap<>();
		lastUpdated = new HashMap<>();
		lastUpdateMessageSent = new HashMap<>();
		usersUpdating = new HashMap<>();
		allRunningThreads = new ArrayList<>();
		
		loadTrackedPlayers();
		calculateRefreshRate();
	}

	@Override
	public void onCommand(MessageReceivedEvent e, String[] args){
		Utils.deleteMessage(e.getChannel(), e.getMessage());
		if(!Utils.checkArguments(e, args, 1)) return;
		
		String user = "", mUser = "";
		String mode = "0";
		for(int i = 0; i < args.length; i++)
			if(args[i].contains("{mode=")){
				mode = args[i].split("\\{mode=")[1].split("}")[0];
			}else user += " " + args[i];
		user = user.substring(1);
		mUser = user + "&m=" + mode;
		
		if(isTracked(e.getGuild().getId(), mUser)){
			stopTracking(e.getGuild().getId(), mUser);
			Main.serverConfigs.get(e.getGuild().getId()).writeValue("track-update-group", e.getTextChannel().getId());
			Utils.info(e.getChannel(), "Stopped tracking " + user + " in the " + convertMode(Utils.stringToInt(mode)) + " mode!\nA full refresh cycle now takes " + currentRefreshRate + " seconds!");
			return;
		}
		
		startTracking(e.getGuild().getId(), mUser);
		Main.serverConfigs.get(e.getGuild().getId()).writeValue("track-update-group", e.getTextChannel().getId());
		Utils.info(e.getChannel(), "Started tracking " + user + " in the " + convertMode(Utils.stringToInt(mode)) + " mode!\nA full refresh cycle now takes " + currentRefreshRate + " seconds!");
	}
	
	private void loadTrackedPlayers(){
    	for(Guild guild : Main.api.getGuilds()){
    		Configuration sCfg = new Configuration(new File("Guilds/" + guild.getId() + ".txt"));
    		ArrayList<String> list = sCfg.getStringList("tracked-players");
    		trackedPlayers.put(guild.getId(), list);
    	}
	}
	
	private void updateLoop(){
		if(update != null) update.cancel();
		
		update = new Timer();
		
		update.scheduleAtFixedRate(new TimerTask(){
			@SuppressWarnings("deprecation")
			public void run(){
				loadTrackedPlayers();
				
				if(totalTrackedUsers() > 0){
					if(refresh != null) refresh.cancel();
					
					refresh = new Timer();
					int delay = Math.abs((int) (currentRefreshRate / (double) trackedUsersWithoutDuplicates()));
					
					final HashMap<String, ArrayList<String>> copied = new HashMap<>();
					copied.putAll(trackedPlayers);
					
					ArrayList<String> updatedUsers = new ArrayList<>();
				
					lastUpdateMessageSent.clear();
					usersUpdating.clear();
					
					if(!allRunningThreads.isEmpty())
						for(Thread t : allRunningThreads)
							t.stop();
					
					allRunningThreads.clear();
					
					refresh.scheduleAtFixedRate(new TimerTask(){
						public void run(){
							for(String server : new HashMap<String, ArrayList<String>>(copied).keySet()){
								final ArrayList<String> players = copied.get(server);
								
								if(players.isEmpty()) continue;
								
								TextChannel channel = Main.api.getTextChannelById(Main.serverConfigs.get(server).getValue("track-update-group"));
								Thread t = new Thread(new Runnable(){
									public void run(){
										for(String player : new ArrayList<String>(players)){
											boolean skip = false;
											
											if(!updatedUsers.contains(player)){
												if(!usersUpdating.containsKey(player)) updateUser(player);
												
												try{
													usersUpdating.get(player).join();
												}catch (InterruptedException e){
													Log.logger.log(Level.SEVERE, e.getMessage(), e);
												}
											}else skip = true;
											
											String msg = "";
											
											if(lastUpdateMessageSent.containsKey(player)) msg = lastUpdateMessageSent.get(player);
											
											synchronized(server){
												if(msg != "" && copied.get(server).contains(player)){
													String spacing = "\n\n\n\n\n";
													MessageHistory history = new MessageHistory(channel);
													Message last = history == null ? null : history.retrieve(1).get(0);
													
													if(last == null || !last.getAuthor().getId().equalsIgnoreCase("120923487467470848")) spacing = "";
													
													Utils.info(channel, spacing + msg.replaceAll("\\*", "\\*").replaceAll("_", "\\_").replaceAll("~", "\\~"));
												}	
												
												players.remove(player);
												
												copied.put(server, players);
											}
											
											if(!skip) break;
										}
										
										if(players.isEmpty())
											copied.remove(server);
										
										allRunningThreads.remove(Thread.currentThread());
										Thread.currentThread().stop();
									}
								});
									
								allRunningThreads.add(t);
								t.start();
								
								break;
							}
							
							if(copied.isEmpty())
								refresh.cancel();
						}
					}, (long) 0, delay * 1000);
				}
			}
		}, 0, (int) (currentRefreshRate * 1000));
	}
	
	@SuppressWarnings("deprecation")
	private void updateUser(String player){	
		Thread t = new Thread(new Runnable(){
			public void run(){
				String mode = player.split("&m=")[1];
				String user = player.split("&m=")[0];
				String lastUpdate = "2000-01-01 00:00:00";
				int limit = 50;
				
				if(lastUpdated.containsKey(player)) lastUpdate = lastUpdated.get(player);
				else{
					lastUpdated.put(player, Utils.toDate(Utils.getCurrentTimeUTC(), "yyyy-MM-dd HH:mm:ss"));
					usersUpdating.remove(player);
					allRunningThreads.remove(Thread.currentThread());
					Thread.currentThread().stop();
					return;
				}
				
				String[] pageHistory = Utils.getHTMLCode("https://osu.ppy.sh/pages/include/profile-history.php?u=" + Utils.getOsuPlayerId(user) + "&m=" + mode);
				
				if(pageHistory.length == 0 || !pageHistory[0].contains("<div class='profileStatHeader'>Recent Plays (last 24h):")){
					lastUpdated.put(player, Utils.toDate(Utils.getCurrentTimeUTC(), "yyyy-MM-dd HH:mm:ss"));
					usersUpdating.remove(player);
					allRunningThreads.remove(Thread.currentThread());
					Thread.currentThread().stop();
					return;
				}
				
				String[] splitTime = pageHistory[0].split("<\\/time>");
				
				boolean valid = false;
				
				for(int i = 0; i < splitTime.length - 1; i++){
					String date = splitTime[i].split("time class=")[1].split(">")[1].replace(" UTC", "");
					
					if(!dateGreaterThanDate(lastUpdate, date)){
						valid = true;
						break;
					}
				}
				
				if(!valid){
					lastUpdated.put(player, Utils.toDate(Utils.getCurrentTimeUTC(), "yyyy-MM-dd HH:mm:ss"));
					usersUpdating.remove(player);
					allRunningThreads.remove(Thread.currentThread());
					Thread.currentThread().stop();
					return;
				}
				
				String post = Main.osuRequestManager.sendRequest("https://osu.ppy.sh/api/", "get_user_recent?k=" + OsuStatsCommand.apiKey + 
						                                         "&u=" + user + "&m=" + mode + "&limit=" + limit + "&type=string&event_days=1");
				
				if(post == "" || !post.contains("{")){
					lastUpdated.put(player, Utils.toDate(Utils.getCurrentTimeUTC(), "yyyy-MM-dd HH:mm:ss"));
					usersUpdating.remove(player);
					allRunningThreads.remove(Thread.currentThread());
					Thread.currentThread().stop();
					return;
				}
				
				MessageBuilder builder = new MessageBuilder();
				
				builder.appendString("—————————————————\nMost recent plays for " + user + " in the " + convertMode(Utils.stringToInt(mode)) + " mode!");
				
				post = "[" + post + "]";
				
				JSONArray jsonResponse = new JSONArray(post);
				
				boolean completeMessage = false;
				
				if(limit != 1){ 
					for(int i = jsonResponse.length() - 1; i >= 0; i--){
						JSONObject obj = jsonResponse.getJSONObject(i);
						String play = "";
						String osuDate = osuDateToCurrentDate(obj.getString("date"));
						
						if(!dateGreaterThanDate(lastUpdate, osuDate)){
							if(obj.getString("rank").equalsIgnoreCase("F")) continue;
							JSONObject map = Map.getMapInfo(obj.getInt("beatmap_id"), false);
							
							play += "\n\n" + osuDate + " UTC\n";
							play += map.getString("artist") + " - " + map.getString("title") + " [" +
							        map.getString("version") + "] " + Mods.getMods(obj.getInt("enabled_mods")) +
							        "\n" + (mode.equals("2") ? "" : Utils.df(getAccuracy(obj)) + "% | ") + 
							        (obj.getInt("perfect") == 0 ? obj.getInt("maxcombo") + "/" + (map.isNull("max_combo") ? "null" : map.getInt("max_combo")) : "FC") +
							        " | " + obj.getString("rank") + " rank\n" + (mode.equals("2") ? "" : (obj.getInt("count100") > 0 ? obj.getInt("count100") + "x100 " : "") +
							        (obj.getInt("count50") > 0 ? obj.getInt("count50") + "x50 " : "")) + (obj.getInt("countmiss") > 0 ? obj.getInt("countmiss") + "x miss " : "") +
							        "\nMap: http://osu.ppy.sh/b/" + obj.getInt("beatmap_id") + " | Status: " + 
							        analyzeMapStatus(map.getInt("approved")) + "\nPlayer: http://osu.ppy.sh/u/" + obj.getInt("user_id");
							
							completeMessage = true;
							builder.appendString(play);
						}
					}
				}
				
				builder.appendString("\n");
				
				if(completeMessage) lastUpdateMessageSent.put(player, builder.build().getStrippedContent());
				
				lastUpdated.put(player, Utils.toDate(Utils.getCurrentTimeUTC(), "yyyy-MM-dd HH:mm:ss"));
				
				usersUpdating.remove(player);
				allRunningThreads.remove(Thread.currentThread());
				Thread.currentThread().stop();
			}
		});
		
		allRunningThreads.add(t);
		usersUpdating.put(player, t);
		t.start();
	}
	
	private String analyzeMapStatus(int code){
		switch(code){
			case -2: return "Graveyard";
			case -1: return "WIP";
			case 0: return "Pending";
			case 1: return "Ranked";
			case 2: return "Approved";
			case 3: return "Qualified";
			default: return "Unsubmitted";
		}
	}
	
	private String convertMode(int mode){
		switch(mode){
			case 0: return "Standard";
			case 1: return "Taiko";
			case 2: return "Catch the Beat";
			case 3: return "Mania";
			default: return "Unknown";
		}
	}
	
	private double getAccuracy(JSONObject play){
		double acc = play.getInt("count300") * 300 + play.getInt("count100") * 100 + play.getInt("count50") * 50;
		return (acc / ((play.getInt("count300") + play.getInt("count100") + play.getInt("count50") + play.getInt("countmiss")) * 300)) * 100;
	}
	
	private String osuDateToCurrentDate(String sDate){
		DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		formatter.setTimeZone(TimeZone.getTimeZone("Asia/Singapore"));
		
		Date date = null;
		try{
			date = formatter.parse(sDate);
		}catch (ParseException e){
			Log.logger.log(Level.SEVERE, e.getMessage(), e);
		}
		
		formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
		return formatter.format(date);
	}
	
	private boolean dateGreaterThanDate(String date, String date1){
		int year = Utils.stringToInt(date.split("-")[0]), year1 = Utils.stringToInt(date1.split("-")[0]);
		
		if(year > year1) return true;
		else if(year == year1){
			int month =  Utils.stringToInt(date.split("-")[1]), month1 = Utils.stringToInt(date1.split("-")[1]);
			
			if(month > month1) return true;
			else if(month == month1){
				int day =  Utils.stringToInt(date.split("-")[2].split(" ")[0]), day1 = Utils.stringToInt(date1.split("-")[2].split(" ")[0]);
				
				if(day > day1) return true;
				else if(day == day1){
					int hour =  Utils.stringToInt(date.split(":")[0].split(" ")[1]), hour1 = Utils.stringToInt(date1.split(":")[0].split(" ")[1]);
					
					if(hour > hour1) return true;
					else if(hour == hour1){
						int minute =  Utils.stringToInt(date.split(":")[1]), minute1 = Utils.stringToInt(date1.split(":")[1]);
						
						if(minute > minute1) return true;
						else if(minute == minute1){
							int second =  Utils.stringToInt(date.split(":")[2]), second1 = Utils.stringToInt(date1.split(":")[2]);
							
							if(second > second1) return true;
							else return false;
						}else return false;
					}else return false;
				}else return false;
			}else return false;
		}else return false;
	}
	
	private void calculateRefreshRate(){
		for(double i = 5; ; i += 0.25)
			if(calculateRequestsPerMinute(i) <= requestsPerMinute){
				currentRefreshRate = i;
				break;
			}
		updateLoop();
	}
	
	private double calculateRequestsPerMinute(double refreshRate){
		int users = trackedUsersWithoutDuplicates();
		return 60.0 / refreshRate * (double) users;
	}
	
	private int totalTrackedUsers(){
		int total = 0;
		for(String server : trackedPlayers.keySet())
			total += trackedPlayers.get(server).size();
		return total;
	}
	
	private int trackedUsersWithoutDuplicates(){
		ArrayList<String> scanned = new ArrayList<>();
		for(String server : trackedPlayers.keySet())
			for(String player : trackedPlayers.get(server))
				if(!scanned.contains(player))
					scanned.add(player);
		return scanned.size();
	}
	
	private boolean isTracked(String server, String player){
		return trackedPlayers.get(server).contains(player);
	}
	
	private void startTracking(String server, String player){
		loadTrackedPlayers();
		
		ArrayList<String> list = trackedPlayers.get(server);
		if(!list.contains(player)) list.add(player);
		Main.serverConfigs.get(server).writeStringList("tracked-players", list, true);
		
		loadTrackedPlayers();
		calculateRefreshRate();
	}
	
	private void stopTracking(String server, String player){
		loadTrackedPlayers();
		
		ArrayList<String> list = trackedPlayers.get(server);
		if(list.contains(player)) list.remove(player);
		Main.serverConfigs.get(server).writeStringList("tracked-players", list, true);
		
		loadTrackedPlayers();
		calculateRefreshRate();
	}
	
	enum Mods{
		None(0, "NoMod"), NoFail(1, "NF"), Easy(2, "EZ"), Hidden(8, "HD"), HardRock(16, "HR"), SuddenDeath(32, "SD"), DoubleTime(64, "DT"),
		Relax(128, "RL"), HalfTime(256, "HT"), Nightcore(512, "NC"), Flashlight(1024, "FL"), SpunOut(4096, "SO"), Autopilot(8192, "AP"),
		Perfect(16384, "PF");
		
		int bit;
		String shortName;
		
		Mods(int bit, String shortName){
			this.bit = bit;
			this.shortName = shortName;
		}
		
		public int getBit(){
			return bit;
		}
		
		public String getShortName(){
			return shortName;
		}
		
		public static String getMods(int modsUsed){
			if(modsUsed == 0) return "";
			String display = "";
			int used = modsUsed;
			List<Mods> mods = new ArrayList<>();
			for(int i = 16384; i >= 1; i /= 2){
				Mods mod = Mods.getMod(i);
				if(used >= i){
					mods.add(mod);
					used -= i;
				}
			}
			
			if(mods.contains(Mods.None)) mods.remove(Mods.None);
			if(mods.contains(Mods.Nightcore)) mods.remove(Mods.DoubleTime);
			
			for(Mods mod : mods)
				display = mod.getShortName() + display;
			
			return "+" + display;
		}
		
		public static Mods getMod(int bit){
			for(Mods mod : Mods.values())
				if(mod.getBit() == bit) return mod;
			return Mods.None;
		}
	}
	
}
