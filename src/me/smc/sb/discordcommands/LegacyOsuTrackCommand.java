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
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageHistory;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

public class LegacyOsuTrackCommand extends GlobalCommand{

	public static HashMap<String, ArrayList<String>> trackedPlayers;
	private static HashMap<String, String> lastUpdated;
	private static HashMap<String, String> lastUpdateMessageSent;
	private static HashMap<String, Thread> usersUpdating;
	private static HashMap<String, HashMap<String, String>> lastPlayerUpdates;
	private static HashMap<String, String> lastPlayerPosted;
	private static ArrayList<Thread> allRunningThreads;
	private static HashMap<String, String> playerStatUpdates;
	private static ArrayList<String> debugServers;
	private static int requestsPerMinute = 90;
	public static double currentRefreshRate = 0;
	private static Timer refresh;
	private static Thread update;
	
	public LegacyOsuTrackCommand(){
		super(null, 
			  " - Lets you track osu! players", 
			  "{prefix}osutrack\nThis command lets you track osu! players' recent performance\n\n" +
		      "----------\nUsage\n----------\n{prefix}osutrack {player} ({mode={0/1/2/3}}) - Tracks or untracks the player's recent statistics for this mode\n" + 
			  "{prefix}osutrack {player} ({ext}) - Tracks or untracks the player in a separate channel\n" +
		      "{prefix}osutrack ({best}) - Tracks only players' best plays in this server\n" +
			  "{prefix}osutrack ({pp=XXX}) - Tracks every play above XXX pp\n\n" +
		      "----------\nAliases\n----------\nThere are no aliases.", 
			  false, 
			  "osutrack");
		
		trackedPlayers = new HashMap<>();
		lastUpdated = new HashMap<>();
		lastUpdateMessageSent = new HashMap<>();
		usersUpdating = new HashMap<>();
		lastPlayerUpdates = new HashMap<>();
		allRunningThreads = new ArrayList<>();
		playerStatUpdates = new HashMap<>();
		lastPlayerPosted = new HashMap<>();
		debugServers = new ArrayList<>();
		
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
		boolean checkStoredInfo = false;
		boolean scanServer = false;
		boolean addToDebug = false;
		boolean changeToBestPlayTracking = false;
		boolean ppCap = false;
		int ppAmount = 0;
		
		for(int i = 0; i < args.length; i++)
			if(args[i].contains("{mode="))
				mode = args[i].split("\\{mode=")[1].split("}")[0];
			else if(args[i].contains("{ext}")) ext = true;
			else if(args[i].contains("{check}")) checkStoredInfo = true;
			else if(args[i].contains("{scan}")) scanServer = true;
			else if(args[i].contains("{debug}")) addToDebug = true;
			else if(args[i].contains("{best}")) changeToBestPlayTracking = true;
			else if(args[i].contains("{pp=")){
				ppCap = true;
				ppAmount = Utils.stringToInt(args[i].split("pp=")[1].split("}")[0]);
			}else user += " " + args[i];
		
		if(ppCap){
			Main.serverConfigs.get(e.getGuild().getId()).writeValue("track-pp-minimum", ppAmount);
			
			Utils.info(e.getChannel(), "Only plays above " + ppAmount + "pp will be tracked!");
			return;
		}
		
		if(changeToBestPlayTracking){
			boolean wasTracking = false;
			
			if(Main.serverConfigs.get(e.getGuild().getId()).getBoolean("track-best-only"))
				wasTracking = true;
			
			Main.serverConfigs.get(e.getGuild().getId()).writeValue("track-best-only", !wasTracking);
			
			Utils.info(e.getChannel(), !wasTracking ? "Only personal bests will be tracked!" :
									   "All plays (except failed plays) will be tracked!");
			return;
		}
		
		if(addToDebug){
			if(debugServers.contains(e.getGuild().getId()))
				debugServers.remove(e.getGuild().getId());
			else debugServers.add(e.getGuild().getId());
			
			Utils.info(e.getChannel(), (debugServers.contains(e.getGuild().getId()) ? "Now" : "Stopped") + 
										" debugging the " + e.getGuild().getName() + " guild's tracking.");
			return;
		}
		
		if(scanServer){
			String trackingId = Main.serverConfigs.get(e.getGuild().getId()).getValue("track-update-group");
			String info = "```Stored info for server " + e.getGuild().getName() + "\n" +
						  "Tracking channel: " + e.getJDA().getTextChannelById(trackingId).getAsMention() + " (" + trackingId + ")\n" +
						  "Tracked users";
			
			Configuration sCfg = new Configuration(new File("Guilds/" + e.getGuild().getId() + ".txt"));
    		ArrayList<String> list = sCfg.getStringList("tracked-players");
			
    		for(String pl : list)
    			info += "\n- " + pl;
    		
			Utils.info(e.getChannel(), info + "```");
			return;
		}
		
		user = user.substring(1);
		mUser = user + "&m=" + mode;
		
		if(checkStoredInfo){
			Utils.info(e.getChannel(), "Stored info for " + user + " - " + playerStatUpdates.get(mUser.toLowerCase()));
			return;
		}
		
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
    		
    		if(list.size() > 0) trackedPlayers.put(guild.getId(), list);
    	}
	}
	
	@SuppressWarnings("deprecation")
	private void updateLoop(){
		if(update != null) update.stop();
		
		update = new Thread(new Runnable(){
			public void run(){
				loadTrackedPlayers();
				
				if(totalTrackedUsers() > 0){
					if(refresh != null) refresh.cancel();
					
					refresh = new Timer();
					double delay = Math.abs(currentRefreshRate / (double) trackedUsersWithoutDuplicates());
		
					final HashMap<String, ArrayList<String>> copied = new HashMap<>(trackedPlayers);
		
					usersUpdating.clear();
					
					synchronized(this){
						if(!allRunningThreads.isEmpty())
							for(Thread t : new ArrayList<Thread>(allRunningThreads))
								t.stop();
					}
					
					allRunningThreads.clear();
					
					refresh.scheduleAtFixedRate(new TimerTask(){
						public void run(){
							for(String server : new HashMap<String, ArrayList<String>>(copied).keySet()){
								final ArrayList<String> players = copied.get(server);
								
								if(debugServers.contains(server))
									Log.logger.log(Level.INFO, "sId " + server + " | players " + players.size());
								
								if(players.isEmpty()) continue;
								
								TextChannel channel = null;
								
								try{
									channel = Main.api.getTextChannelById(Main.serverConfigs.get(server).getValue("track-update-group"));
								}catch(Exception ex){
									continue;
								}
								
								final TextChannel fChannel = channel;
								
								Thread t = new Thread(new Runnable(){
									public void run(){
										for(String player : new ArrayList<String>(players)){										
											if(!usersUpdating.containsKey(player)) updateUser(server, player);
											
											try{
												usersUpdating.get(player).join();
											}catch (InterruptedException e){
												Log.logger.log(Level.SEVERE, e.getMessage(), e);
											}
											
											String msg = "";
											
											if(lastUpdateMessageSent.containsKey(player)) msg = lastUpdateMessageSent.get(player);
											
											synchronized(server){
												if(msg != "" && !getLastPlayerUpdate(player, server).equals(lastUpdated.get(player))){
													String rebuiltMsg = "";
													
													for(String play : msg.split("\\|\\|\\|")){
														if(!play.contains("~~~")) continue;
														
														boolean isPB = Boolean.parseBoolean(play.split("~~~&pb=")[1].split("&pp")[0]);
														double ppAmount = Utils.stringToDouble(play.split("~~~&pb=" + isPB + "&pp=")[1]);
														
														if((Main.serverConfigs.get(server).getBoolean("track-best-only") ? isPB : true) &&
														   ppAmount >= Main.serverConfigs.get(server).getInt("track-pp-minimum"))
															rebuiltMsg += play.split("~~~")[0] + "|||";
													}
													
													String spacing = "\n\n\n\n\n";
													MessageHistory history = null;
													
													try{
														history = new MessageHistory(fChannel);
													}catch(Exception e){}
													
													Message last = null;
													
													if(history != null){
														List<Message> pulledHistory = history.retrievePast(1).complete();
														
														if(pulledHistory.size() != 0) last = pulledHistory.get(0);
													}
													
													
													if(last == null || !last.getAuthor().getId().equalsIgnoreCase("120923487467470848")) spacing = "";
													setLastPlayerUpdate(player, server);

													TextChannel channel = fChannel;
													
													if(!Main.serverConfigs.get(server).getValue(player + "-update-group").equals(""))
														channel = Main.api.getTextChannelById(Main.serverConfigs.get(server).getValue(player + "-update-group"));
													
													String fMsg = spacing + rebuiltMsg.replaceAll("\\*", "\\*").replaceAll("_", "\\_").replaceAll("~", "\\~");
		
													if(lastPlayerPosted.containsKey(server) && last != null && 
													   last.getAuthor().getId().equalsIgnoreCase("120923487467470848"))
														if(lastPlayerPosted.get(server).equalsIgnoreCase(player) && fMsg.length() > 0){
															try{
																fMsg = fMsg.substring(fMsg.indexOf("__**"), fMsg.length() - 1);
															}catch(Exception e){
																Log.logger.log(Level.SEVERE, "fMsg error: " + e.getMessage() + " | fMsg: " + fMsg, e);
															}
														}
														
													if(fMsg.length() > 0)
														for(String splitMsg : fMsg.split("\\|\\|\\|")){
															if(splitMsg.length() > 5)
																Utils.info(channel, splitMsg.split("jpg")[0] + "jpg");
														}
																		
													lastPlayerPosted.put(server, player);
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
								updateLoop();
						}
					}, (long) 0, (long) (delay * 1000));
				}
			}
		});
		
		update.start();
	}
	
	@SuppressWarnings("deprecation")
	private void updateUser(String server, String player){	
		Thread t = new Thread(new Runnable(){
			public void run(){
				String mode = player.split("&m=")[1];
				String user = player.split("&m=")[0];
				String lastUpdate = "2000-01-01 00:00:01";
				int limit = 10;
				
				if(lastUpdated.containsKey(player)) lastUpdate = lastUpdated.get(player);
				else{
					String id = Utils.getOsuPlayerId(user);
					
					//playerStatUpdates.put(player.toLowerCase(), Utils.getOsuPlayerPPAndRank(id, Utils.stringToInt(mode)));
					
					lastUpdated.put(player, Utils.toDate(Utils.getCurrentTimeUTC(), "yyyy-MM-dd HH:mm:ss"));
					usersUpdating.remove(player);
					allRunningThreads.remove(Thread.currentThread());
					Thread.currentThread().stop();
					return;
				}
				
				String playerId = Utils.getOsuPlayerId(user);
				
				String[] pageHistory = Utils.getHTMLCode("https://osu.ppy.sh/pages/include/profile-history.php?u=" + playerId + "&m=" + mode);
				
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
				
				String post = ""; //Main.osuRequestManager.sendRequest("https://osu.ppy.sh/api/", "get_user_recent?k=" + OsuStatsCommand.apiKey + 
						          //                                   "&u=" + user + "&m=" + mode + "&limit=" + limit + "&type=string&event_days=1");
				
				if(post == "" || !post.contains("{")){
					usersUpdating.remove(player);
					allRunningThreads.remove(Thread.currentThread());
					Thread.currentThread().stop();
					return;
				}
				
				StringBuilder builder = new StringBuilder();
				
				post = "[" + post + "]";
				
				JSONArray jsonResponse = new JSONArray(post);
				
				boolean completeMessage = false;
				
				String[] pageGeneral = Utils.getHTMLCode("https://osu.ppy.sh/pages/include/profile-general.php?u=" + playerId + "&m=" + mode);
				List<RecentPlay> recentPlays = new ArrayList<>();
				
				List<String> userLine = Utils.getNextLineCodeFromLink(pageGeneral, 0, "&find=");
				
				String fixedUser = user;
				
				if(userLine.size() > 0){
					fixedUser = userLine.get(0).split("&find=")[1].split("&")[0];
				}
				
				builder.append("—————————————————\nMost recent plays for **" + fixedUser + "** in the " + convertMode(Utils.stringToInt(mode)) + " mode!");
				
				if(Utils.getNextLineCodeFromLink(pageGeneral, 0, "This user hasn't done anything notable recently!").size() == 0){
					List<String> list = Utils.getNextLineCodeFromLink(pageGeneral, 0, "<div class='profileStatHeader'>Recent Activity</div>");
					
					if(list.size() != 0){
						String line = list.get(0);
						String[] plays = line.split("<tr>");
						
						for(int i = 1; i < plays.length; i++){
							if(plays[i].contains("achieved") && plays[i].contains("rank #")){
								String date = plays[i].split("UTC")[0].split("Z'>")[1];
								int rank = Utils.stringToInt(plays[i].split("rank #")[1].split(" on")[0].replace("</b>", ""));
								int beatmapId = Utils.stringToInt(plays[i].split("href='\\/b\\/")[1].split("\\?m=")[0]);
								
								RecentPlay play = new RecentPlay(beatmapId, date, rank);
								
								boolean equal = false;
								
								if(recentPlays.size() > 0)
									for(RecentPlay r : recentPlays)
										if(r.eq(play))
											equal = true;
								
								if(equal) continue;
								
								if(dateGreaterThanDate(date, lastUpdate))
									recentPlays.add(play);
							}
						}
					}
				}
				
				String ppAndRank = ""; //Utils.getOsuPlayerPPAndRank(playerId, Utils.stringToInt(mode));
				double ppp = Utils.stringToDouble(ppAndRank.split("&r=")[0]);
				int rank = Utils.stringToInt(ppAndRank.split("&r=")[1].split("&cr=")[0]);
				int countryRank = Utils.stringToInt(ppAndRank.split("&cr=")[1]);
				
				if(limit != 1){ 
					for(int i = jsonResponse.length() - 1; i >= 0; i--){		
						JSONObject obj = jsonResponse.getJSONObject(i);
						String play = "";
						String osuDate = osuDateToCurrentDate(obj.getString("date"));
						
						if(!dateGreaterThanDate(lastUpdate, osuDate)){
							if(obj.getString("rank").equalsIgnoreCase("F")) continue;
							
							Utils.sleep(2500);
							
							JSONObject map = Map.getMapInfo(obj.getInt("beatmap_id"), Utils.stringToInt(mode), false);
							
							String pp = "";
							boolean isPB = false;
							double ppAmount = 0.0;
							
							if(mode.equals("0")){
								int combo = 0;
								if(obj.getInt("perfect") == 0) combo = obj.getInt("maxcombo");
								
								pp = fetchPPFromOppai(obj.getInt("beatmap_id"), 
													  map.getInt("beatmapset_id"), 
													  Utils.df(getAccuracy(obj, Utils.stringToInt(mode))),
													  combo, 
													  Mods.getMods(obj.getInt("enabled_mods")).replaceAll("\\*", ""), 
													  obj.getInt("countmiss"),
													  obj.getInt("count50"),
													  obj.getInt("count100"));
								
								ppAmount = Utils.stringToDouble(pp.split("~")[1].split("pp")[0]);
							}
							
							String hits = fetchHitText(Utils.stringToInt(mode), obj);
							
							int mapRank = 0;
							RecentPlay recent = null;
							
							for(RecentPlay rPlay : recentPlays)
								if(rPlay.getBeatmapId() == obj.getInt("beatmap_id") && rPlay.dateValid(osuDate, 5)){
									mapRank = rPlay.getRank();
									recent = rPlay;
									break;
								}
							
							if(recent != null) recentPlays.remove(recent);
							
							// this is not accurate if there's more than 1 play queued, please look into verifying if they are all in tops
							// and how much they are weighted to give an appropriate estimate.
							double ppDiff = ppp - Utils.stringToDouble(playerStatUpdates.get(player.toLowerCase()).split("&r=")[0]);
							String ppDifference = Utils.df(ppDiff, 2) + "pp";
							
							int rankDiff = rank - Utils.stringToInt(playerStatUpdates.get(player.toLowerCase()).split("&r=")[1].split("&cr=")[0]);
							String rankDifference = Utils.veryLongNumberDisplay(rankDiff);
							
							int countryDiff = countryRank - Utils.stringToInt(playerStatUpdates.get(player.toLowerCase()).split("&cr=")[1]);
							String countryDifference = Utils.veryLongNumberDisplay(countryDiff);
							
							int personalBest = 0;
							String country = Utils.getNextLineCodeFromLink(pageGeneral, 0, "&c=").get(0).split("&c=")[1].split("&find=")[0];
							
							if(ppDiff > 0){
								ppDifference = "+" + ppDifference;
							}
							
							if(Math.abs(ppDiff) > 0){
								String topPlays = ""; //Main.osuRequestManager.sendRequest("https://osu.ppy.sh/api/", "get_user_best?k=" + OsuStatsCommand.apiKey + 
                              		                  //"&u=" + user + "&m=" + mode + "&limit=100&type=string");
						
								if(topPlays.length() > 0 && topPlays.contains("{")){
									topPlays = "[" + topPlays + "]";
									
									JSONArray tpJsonResponse = new JSONArray(topPlays);
									
									for(int j = 0; j < tpJsonResponse.length(); j++){
										JSONObject topPlay = tpJsonResponse.getJSONObject(j);
										
										if(topPlay.getInt("beatmap_id") == obj.getInt("beatmap_id") &&
										   topPlay.getInt("enabled_mods") == obj.getInt("enabled_mods")){
											personalBest = j + 1;
											
											isPB = true;
											
											ppAmount = topPlay.getDouble("pp");
											
											if(!mode.equals("0") || !pp.contains("FC")) pp = "**" + Utils.df(topPlay.getDouble("pp"), 2) + "pp**";
											else if(pp.contains("FC")) pp = "**" + Utils.df(topPlay.getDouble("pp"), 2) + "pp**" + pp.replace(pp.split(" ")[0], "");
											
											break;
										}		
									}
								}
							}
							
							if(rankDiff < 0){
								if(rankDifference.startsWith("-"))
									rankDifference = rankDifference.substring(1);
								
								rankDifference = "+" + rankDifference;
							}else rankDifference = "-" + rankDifference;
							
							if(countryDiff < 0){
								if(countryDifference.startsWith("-"))
									countryDifference = countryDifference.substring(1);
								
								countryDifference = "+" + countryDifference;
							}else if(countryDiff == 0){
								countryDifference = "0";
							}else countryDifference = "-" + countryDifference;
							
							if(ppp == -1.0){
								ppDiff = 0;
								rankDiff = 0;
							}
							
							try{
								play += "\n\n__**" + osuDate + " UTC**__\n\n";
								play += escapeStar(map.getString("artist")) + " - " + escapeStar(map.getString("title")) + " [" +
										escapeStar(map.getString("version")) + "] " + Mods.getMods(obj.getInt("enabled_mods")) +
								        "\n" + Utils.df(getAccuracy(obj, Utils.stringToInt(mode))) + "%" + (hits.length() > 0 ? " • " + hits : "") + "\n" + 
								        Utils.veryLongNumberDisplay(obj.getInt("score")) + " • " + (obj.getInt("perfect") == 0 ? obj.getInt("maxcombo") +
								        (map.isNull("max_combo") ? "x" : "/" + map.get("max_combo").toString()) : "FC (" + map.get("max_combo").toString() + "x)") +
								        " • " + obj.getString("rank").replace("X", "SS") + " rank" + (mapRank > 0 ? " • **#" + mapRank + "** on map" : "") + 
								        (!pp.equals("") ? "\n" + pp : "") + "\n\n" + 
								        (Math.abs(ppDiff) >= 0.01 ? ppp + "pp (**" + ppDifference + "**)" + (personalBest != 0 ? " • **#" + personalBest + "** personal best\n" : "\n") : "") + 
								        (Math.abs(rankDiff) >= 1 ? "#" + Utils.veryLongNumberDisplay(rank) + " (**" + rankDifference + "**) • #" + 
								        Utils.veryLongNumberDisplay(countryRank) + " " + country + " (**" + countryDifference + "**)\n\n" : 
								        (Math.abs(ppDiff) >= 0.01 ? "\n" : "")) +
								        "Map • <http://osu.ppy.sh/b/" + obj.getInt("beatmap_id") + "> • " + analyzeMapStatus(map.getInt("approved")) + 
								        "\n" + fixedUser + " • <http://osu.ppy.sh/u/" + obj.getInt("user_id") + ">\nBG • http://b.ppy.sh/thumb/" + map.getInt("beatmapset_id") + "l.jpg";
								
								completeMessage = true;
								builder.append(play + "~~~&pb=" + isPB + "&pp=" + ppAmount + "|||");
							}catch(Exception ex){
								Log.logger.log(Level.SEVERE, "osu!track exception: " + ex.getMessage());
							}
						}
					}
				}
				
				builder.append("\n");
				
				if(completeMessage) 
					lastUpdateMessageSent.put(player, builder.toString());
				
				if(ppp != -1.0) playerStatUpdates.put(player.toLowerCase(), ppAndRank);
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
	
	public static String escapeStar(String str){
		return str.replaceAll("\\*", "\\*");
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
		
		//url = Utils.getFinalURL(url);
		
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
			case 4: return "Loved";
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
	
	public static Date addSecondsToDate(String cDate, int seconds){
		DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		
		try{
			return new Date(formatter.parse(cDate).getTime() + (seconds * 1000));
		}catch (ParseException e1){
			return null;
		}
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
							
							if(second > second1 || second == second1) return true;
						}
					}
				}
			}
		}
		
		return false;
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
		loadTrackedPlayers();
		
		ArrayList<String> scanned = new ArrayList<>();
		
		for(String server : trackedPlayers.keySet())
			for(String player : trackedPlayers.get(server))
				if(!scanned.contains(player.toLowerCase()))
					scanned.add(player.toLowerCase());
		
		return scanned.size();
	}
	
	private boolean isTracked(String server, String player){
		loadTrackedPlayers();
		
		if(!trackedPlayers.containsKey(server)) return false;
		
		for(String pl : trackedPlayers.get(server))
			if(pl.equalsIgnoreCase(player))
				return true;
		
		return false;
	}
	
	private void startTracking(String server, String player){
		loadTrackedPlayers();
		
		ArrayList<String> list = new ArrayList<String>();
		
		if(trackedPlayers.containsKey(server)) list = trackedPlayers.get(server);
		
		boolean contained = false;
		
		if(list != null && list.size() > 0)
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
		
		ArrayList<String> list = new ArrayList<String>();
		
		if(trackedPlayers.containsKey(server)) list = trackedPlayers.get(server);
		
		for(String pl : new ArrayList<>(list))
			if(pl.equalsIgnoreCase(player))
				list.remove(pl);

		Main.serverConfigs.get(server).writeStringList("tracked-players", list, true);
		
		loadTrackedPlayers();
		calculateRefreshRate();
	}
	
	class RecentPlay{
		int beatmapId;
		String date;
		int rank;
		
		public RecentPlay(int beatmapId, String date, int rank){
			this.beatmapId = beatmapId;
			this.date = date;
			this.rank = rank;
		}
		
		boolean dateValid(String otherDate, int secondsLeeway){
			Date beforeDate = addSecondsToDate(date, -secondsLeeway);
			Date afterDate = addSecondsToDate(date, secondsLeeway);
			Date oDate = addSecondsToDate(otherDate, 0);
			
			if(beforeDate == null || afterDate == null || oDate == null)
				return false;
			
			if(oDate.after(beforeDate) && oDate.before(afterDate))
				return true;
			
			return false;
		}
		
		public int getBeatmapId(){
			return beatmapId;
		}
		
		public int getRank(){
			return rank;
		}
		
		public boolean eq(RecentPlay other){
			return beatmapId == other.getBeatmapId() &&
				   date.equals(other.date) &&
				   rank == other.getRank();
		}
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
			
			return "**+" + display + "**";
		}
		
		public static Mods getMod(int bit){
			for(Mods mod : Mods.values())
				if(mod.getBit() == bit) return mod;
			return Mods.None;
		}
	}
	
}
