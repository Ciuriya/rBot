package me.smc.sb.discordcommands;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
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
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;

import me.smc.sb.main.Main;
import me.smc.sb.multi.Map;
import me.smc.sb.utils.Configuration;
import me.smc.sb.utils.Log;
import me.smc.sb.utils.Utils;
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
	private static HashMap<String, HashMap<String, String>> lastPlayerUpdates;
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
		lastPlayerUpdates = new HashMap<>();
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
		boolean ext = false;
		
		for(int i = 0; i < args.length; i++)
			if(args[i].contains("{mode="))
				mode = args[i].split("\\{mode=")[1].split("}")[0];
			else if(args[i].contains("{ext}")) ext = true;
			else user += " " + args[i];
			
		
		user = user.substring(1);
		mUser = user + "&m=" + mode;
		
		if(isTracked(e.getGuild().getId(), mUser)){
			stopTracking(e.getGuild().getId(), mUser);
			
			Main.serverConfigs.get(e.getGuild().getId()).writeValue("track-update-group", e.getTextChannel().getId());
			Main.serverConfigs.get(e.getGuild().getId()).deleteKey(mUser + "-update-group");
			
			Utils.info(e.getChannel(), "Stopped tracking " + user + " in the " + convertMode(Utils.stringToInt(mode)) + " mode!\nA full refresh cycle now takes " + currentRefreshRate + " seconds!");
			return;
		}
		
		startTracking(e.getGuild().getId(), mUser);
		
		if(!ext) Main.serverConfigs.get(e.getGuild().getId()).writeValue("track-update-group", e.getTextChannel().getId());
		else Main.serverConfigs.get(e.getGuild().getId()).writeValue(mUser + "-update-group", e.getTextChannel().getId());
		
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
											if(!updatedUsers.contains(player)){
												if(!usersUpdating.containsKey(player)) updateUser(player);
												
												try{
													usersUpdating.get(player).join();
												}catch (InterruptedException e){
													Log.logger.log(Level.SEVERE, e.getMessage(), e);
												}
											}
											
											String msg = "";
											
											if(lastUpdateMessageSent.containsKey(player)) msg = lastUpdateMessageSent.get(player);
											
											synchronized(server){
												if(msg != "" && !getLastPlayerUpdate(player, server).equals(lastUpdated.get(player))){
													String spacing = "\n\n\n\n\n";
													MessageHistory history = new MessageHistory(channel);
													Message last = history == null ? null : history.retrieve(1).get(0);
													
													if(last == null || !last.getAuthor().getId().equalsIgnoreCase("120923487467470848")) spacing = "";
													setLastPlayerUpdate(player, server);
													
													TextChannel fChannel = channel;
													if(!Main.serverConfigs.get(server).getValue(player + "-update-group").equals(""))
														fChannel = Main.api.getTextChannelById(Main.serverConfigs.get(server).getValue(player + "-update-group"));
													
													Utils.info(fChannel, spacing + msg.replaceAll("\\*", "\\*").replaceAll("_", "\\_").replaceAll("~", "\\~"));
												}	
												
												players.remove(player);
												
												copied.put(server, players);
											}
											
											break;
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
				String lastUpdate = "2000-01-01 00:00:01";
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
					usersUpdating.remove(player);
					allRunningThreads.remove(Thread.currentThread());
					Thread.currentThread().stop();
					return;
				}
				
				String post = Main.osuRequestManager.sendRequest("https://osu.ppy.sh/api/", "get_user_recent?k=" + OsuStatsCommand.apiKey + 
						                                         "&u=" + user + "&m=" + mode + "&limit=" + limit + "&type=string&event_days=1");
				
				if(post == "" || !post.contains("{")){
					usersUpdating.remove(player);
					allRunningThreads.remove(Thread.currentThread());
					Thread.currentThread().stop();
					return;
				}
				
				StringBuilder builder = new StringBuilder();
				
				builder.append("—————————————————\nMost recent plays for " + user + " in the " + convertMode(Utils.stringToInt(mode)) + " mode!");
				
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
							
							String pp = "";
							
							if(mode.equals("0")){
								int combo = 0;
								if(obj.getInt("perfect") == 0) combo = obj.getInt("maxcombo");
								
								pp = fetchPPFromOppai(obj.getInt("beatmap_id"), 
													  map.getInt("beatmapset_id"), 
													  Utils.df(getAccuracy(obj, Utils.stringToInt(mode))),
													  combo, 
													  Mods.getMods(obj.getInt("enabled_mods")), 
													  obj.getInt("countmiss"),
													  obj.getInt("count50"),
													  obj.getInt("count100"));
							}
							
							String hits = fetchHitText(Utils.stringToInt(mode), obj);
							
							play += "\n\n" + osuDate + " UTC\n";
							play += map.getString("artist") + " - " + map.getString("title") + " [" +
							        map.getString("version") + "] " + Mods.getMods(obj.getInt("enabled_mods")) +
							        "\n" + Utils.df(getAccuracy(obj, Utils.stringToInt(mode))) + "% | " + 
							        (obj.getInt("perfect") == 0 ? obj.getInt("maxcombo") + "/" + (map.isNull("max_combo") ? "null" : map.getInt("max_combo")) : "FC") +
							        " | " + obj.getString("rank").replace("X", "SS") + " rank\n" + hits +
							        (mode.equals("0") ? "\n" + pp : "") + "\n\nMap: <http://osu.ppy.sh/b/" + obj.getInt("beatmap_id") + "> | Status: " + 
							        analyzeMapStatus(map.getInt("approved")) + "\nPlayer: <http://osu.ppy.sh/u/" + obj.getInt("user_id") + ">\n" +
							        "BG: http://b.ppy.sh/thumb/" + map.getInt("beatmapset_id") + "l.jpg";
							
							completeMessage = true;
							builder.append(play);
						}
					}
				}
				
				builder.append("\n");
				
				if(completeMessage) lastUpdateMessageSent.put(player, builder.toString());
				
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
	
	public static String fetchHitText(int mode, JSONObject obj){
		switch(mode){
			case 0:
				return (obj.getInt("count100") > 0 ? obj.getInt("count100") + "x100 " : "") +
		        	   (obj.getInt("count50") > 0 ? obj.getInt("count50") + "x50 " : "") + 
		        	   (obj.getInt("countmiss") > 0 ? obj.getInt("countmiss") + "x miss " : "");
			case 1:
				return (obj.getInt("count100") > 0 ? obj.getInt("count100") + "x100 " : "") +
	        	   	   (obj.getInt("countmiss") > 0 ? obj.getInt("countmiss") + "x miss " : "");
			case 2:
				return (obj.getInt("countkatu") > 0 ? obj.getInt("countkatu") + "x droplets " : "") +
        	   	   	   (obj.getInt("countmiss") > 0 ? obj.getInt("countmiss") + "x miss " : "");
			case 3:
				return (obj.getInt("countgeki") > 0 ? obj.getInt("countgeki") + "xMAX " : "") +
					   (obj.getInt("count300") > 0 ? obj.getInt("count300") + "x300 " : "") +
					   (obj.getInt("countkatu") > 0 ? obj.getInt("countkatu") + "x200 " : "") +
	        	   	   (obj.getInt("count100") > 0 ? obj.getInt("count100") + "x100 " : "") + 
	        	   	   (obj.getInt("count50") > 0 ? obj.getInt("count50") + "x50 " : "") + 
	        	   	   (obj.getInt("countmiss") > 0 ? obj.getInt("countmiss") + "x miss " : "");
			default: return "";
		}
	}
	
	private String fetchPPFromOppai(int beatmapId, int setId, double accuracy, int combo, String mods, int misses, int fifties, int hundreds){
		Utils.Login.osu();
		File osuFile = fetchOsuFile(beatmapId, setId);
		
		if(osuFile == null) return "**~0.0pp**";
		
		osuFile.renameTo(new File(beatmapId + ".osu"));
		
		osuFile = new File(beatmapId + ".osu");
		
		String actual = "./oppai " + osuFile.getName() + (accuracy == 100 ? "" : " " + accuracy + "%") +
						 (mods.length() != 0 ? " " + mods : "") +
						 (combo == 0 ? "" : " " + combo + "x") +
						 (misses == 0 ? "" : " " + misses + "m");
		
		String forFC = "./oppai " + osuFile.getName() + " " + hundreds + "x100 " + fifties + "x50" +
					   (mods.length() != 0 ? " " + mods : "");
		
		try{
			Process p = Runtime.getRuntime().exec(actual);
			double pp = fetchOppaiPP(p);
			
			double fc = 0.0;
			if(combo != 0){
				Process p2 = Runtime.getRuntime().exec(forFC);
				fc = fetchOppaiPP(p2);	
			}
			
			osuFile.delete();
			
			return "**~" + Utils.df(pp, 2) + "pp**" + (fc == 0.0 ? "" : " (" + Utils.df(fc, 2) + "pp for FC)");
		}catch(Exception e){
			Log.logger.log(Level.SEVERE, e.getMessage(), e);
			osuFile.delete();
			
			return "**~0.0pp**";
		}
	}
	
	private double fetchOppaiPP(Process p){
		try{
			BufferedReader pIn = new BufferedReader(new InputStreamReader(p.getInputStream()));
			
			String s = null;
			String t = null;
			
			while((t = pIn.readLine()) != null)
				if(t != null) s = t;
			
			if(s == null) throw new Exception("string is null");
			
			double pp = Utils.stringToDouble(s.split("pp")[0]);
			return pp;	
		}catch(Exception e){
			Log.logger.log(Level.SEVERE, e.getMessage(), e);
			
			return 0.0;
		}
	}
	
	private File fetchOsuFile(int beatmapId, int setId){
		String[] html = Utils.getHTMLCode("https://osu.ppy.sh/b/" + beatmapId);

		ArrayList<String> line = Utils.getNextLineCodeFromLink(html, 0, "beatmapTab active");
		if(line.isEmpty()) return null;

		String diffName = Jsoup.parse(line.get(0).split("<span>")[1].split("</span>")[0]).text();
		
		String url = "https://osu.ppy.sh/d/" + setId + "n";
		
		url = Utils.getFinalURL(url);
		
		URLConnection connection = establishConnection(url);
		boolean bloodcat = false;
		
		if(connection.getContentLength() <= 100){
			connection = establishConnection("http://bloodcat.com/osu/b/" + beatmapId);
			bloodcat = true;
		}
		
		File file = null;
		
        try{
			InputStream in = connection.getInputStream();
			FileOutputStream out = new FileOutputStream((bloodcat ? beatmapId + ".osu" : setId + ".zip"));
			
	        byte[] b = new byte[1024];
	        int count;
	        
	        while((count = in.read(b)) >= 0)
	        	out.write(b, 0, count);
	        
			in.close();
			out.close();
			
			file = new File((bloodcat ? beatmapId + ".osu" : setId + ".zip"));
			
			if(bloodcat && file != null) return file;
			if(file == null) return null;
			
			ZipInputStream zis = new ZipInputStream(new FileInputStream(file));
			ZipEntry entry = zis.getNextEntry();
			File finalFile = null;
			
			while(entry != null){
				if(entry.getName().endsWith(".osu")){
					BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(entry.getName()));
					
					byte[] buffer = new byte[4096];
					int read = 0;
					
					while((read = zis.read(buffer)) >= 0)
						bos.write(buffer, 0, read);
					
					bos.close();  
					
					BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(entry.getName()), "UTF-8"));
					
					String l = br.readLine();
					boolean found = false;
					
					while(l != null){
						if(l.startsWith("Version:") && l.replaceFirst("Version:", "").equalsIgnoreCase(diffName)){
							found = true;
							break;
						}
						
						l = br.readLine();
					}
					
					br.close();
					
					if(found){
						zis.closeEntry();
						zis.close();
						
						finalFile = new File(entry.getName());
						break;
					}
					
					new File(entry.getName()).delete();
				}
				
				zis.closeEntry();
				entry = zis.getNextEntry();
			}
			
			zis.close();
			file.delete();
			
			return finalFile;
        }catch(Exception e){
			Log.logger.log(Level.SEVERE, e.getMessage(), e);
			if(file != null && file.exists()) file.delete();
			
			return null;
		}
	}
	
	private URLConnection establishConnection(String url){
		URLConnection connection = null;
		
		try{
			connection = new URL(url).openConnection();
		}catch(Exception e){
			Log.logger.log(Level.SEVERE, e.getMessage(), e);
		}
		
        connection.setDoOutput(true);
        connection.setDoInput(true);
        connection.setRequestProperty("User-Agent", "Mozilla/5.0");
        connection.setRequestProperty("content-type", "binary/data");
        return connection;
	}
	
	private String getLastPlayerUpdate(String player, String server){
		HashMap<String, String> servers = new HashMap<>();
		
		if(lastPlayerUpdates.containsKey(player)) servers = lastPlayerUpdates.get(player);
		
		if(!servers.containsKey(server)) return "2000-01-01 00:00:00";
		
		return servers.get(server);
	}
	
	private void setLastPlayerUpdate(String player, String server){
		HashMap<String, String> servers = new HashMap<>();
		
		if(lastPlayerUpdates.containsKey(player)) servers = lastPlayerUpdates.get(player);
		
		servers.put(server, lastUpdated.get(player));
		lastPlayerUpdates.put(player, servers);
	}
	
	public static String analyzeMapStatus(int code){
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
	
	public static String convertMode(int mode){
		switch(mode){
			case 0: return "Standard";
			case 1: return "Taiko";
			case 2: return "Catch the Beat";
			case 3: return "Mania";
			default: return "Unknown";
		}
	}
	
	public static double getAccuracy(JSONObject play, int mode){
		int totalHits = 0;
		int points = 0;
		
		switch(mode){
			case 0:
				totalHits = play.getInt("count300") + play.getInt("count100") + play.getInt("count50") + play.getInt("countmiss");
				points = play.getInt("count300") * 300 + play.getInt("count100") * 100 + play.getInt("count50") * 50;
				
				totalHits *= 300;
				return ((double) points / (double) totalHits) * 100;
			case 1:
				totalHits = play.getInt("countmiss") + play.getInt("count100") + play.getInt("count300");
				double dPoints = play.getInt("count100") * 0.5 + play.getInt("count300");
				
				dPoints *= 300;
				totalHits *= 300;
				
				return (dPoints / (double) totalHits) * 100;
			case 2:
				int caught = play.getInt("count50") + play.getInt("count100") + play.getInt("count300");
				int fruits = play.getInt("countmiss") + caught + play.getInt("countkatu");
				
				return ((double) caught / (double) fruits) * 100;
			case 3:
				totalHits = play.getInt("countmiss") + play.getInt("count50") + play.getInt("count100") + 
						    play.getInt("count300") + play.getInt("countgeki") + play.getInt("countkatu");
				points = play.getInt("count50") * 50 + play.getInt("count100") * 100 + play.getInt("countkatu") * 200 +
						 play.getInt("count300") * 300 + play.getInt("countgeki") * 300;
				
				totalHits *= 300;
				return ((double) points / (double) totalHits) * 100;
			default: return 0.0;
		}
	}
	
	public static String osuDateToCurrentDate(String sDate){
		DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		formatter.setTimeZone(TimeZone.getTimeZone("Asia/Singapore"));
		
		Date date = null;
		
		try{
			date = formatter.parse(sDate);
		}catch(ParseException e){
			Log.logger.log(Level.SEVERE, e.getMessage(), e);
		}
		
		formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
		return formatter.format(date);
	}
	
	public static boolean dateGreaterThanDate(String date, String date1){
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
				if(!scanned.contains(player.toLowerCase()))
					scanned.add(player.toLowerCase());
		
		return scanned.size();
	}
	
	private boolean isTracked(String server, String player){
		for(String pl : trackedPlayers.get(server))
			if(pl.equalsIgnoreCase(player))
				return true;
		
		return false;
	}
	
	private void startTracking(String server, String player){
		loadTrackedPlayers();
		
		ArrayList<String> list = trackedPlayers.get(server);
		
		boolean contained = false;
		
		for(String pl : list)
			if(pl.equalsIgnoreCase(player))
				contained = true;
		
		if(!contained) list.add(player);
		
		Main.serverConfigs.get(server).writeStringList("tracked-players", list, true);
		
		loadTrackedPlayers();
		calculateRefreshRate();
	}
	
	private void stopTracking(String server, String player){
		loadTrackedPlayers();
		
		ArrayList<String> list = trackedPlayers.get(server);
		
		for(String pl : new ArrayList<>(list))
			if(pl.equalsIgnoreCase(player))
				list.remove(pl);

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
			if(mods.contains(Mods.Perfect)) mods.remove(Mods.SuddenDeath);
			
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
