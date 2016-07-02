package me.smc.sb.discordcommands;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
import java.util.logging.Level;

import org.json.JSONArray;
import org.json.JSONObject;

import me.smc.sb.discordcommands.OsuTrackCommand.Mods;
import me.smc.sb.main.Main;
import me.smc.sb.multi.Map;
import me.smc.sb.utils.Log;
import me.smc.sb.utils.Utils;
import net.dv8tion.jda.events.message.MessageReceivedEvent;

public class OsuLastTopPlays extends GlobalCommand{

	public OsuLastTopPlays(){
		super(null, 
			  " - Lets you see the player's latest top plays", 
			  "{prefix}osulastplays\nThis command lets you see an osu! player's recent top plays\n\n" +
		      "----------\nUsage\n----------\n{prefix}osulastplays {player} (number of top plays) ({mode={0/1/2/3}}) - Shows the player's recent top plays (default 5 latest) \n\n" + 
		      "----------\nAliases\n----------\nThere are no aliases.", 
			  true, 
			  "osulastplays");
	}
	
	@Override
	public void onCommand(MessageReceivedEvent e, String[] args){
		Utils.deleteMessage(e.getChannel(), e.getMessage());
		if(!Utils.checkArguments(e, args, 1)) return;
		
		String original = e.getMessage().getContent().replace("!osulastplays ", ""); //make modular kthx
		int plays = 5;
		String mode = "0";
		String player = "";
		
		for(int i = 0; i < args.length; i++)
			if(args[i].contains("{mode=")){
				mode = args[i].split("\\{mode=")[1].split("}")[0];
				original = original.replace(" {mode=" + mode + "}", "");
				
				break;
			}else player += " " + args[i];
		
		player = player.substring(1);
		
		if(Utils.stringToInt(mode) == -1) mode = "0";
		
		if(args.length > 1 && Utils.stringToInt(original.split(" ")[original.split(" ").length - 1]) != -1){
			plays = Utils.stringToInt(original.split(" ")[original.split(" ").length - 1]);
			player = player.replace(" " + plays, "");
		}
		
		List<String> seperatePosts = new ArrayList<String>();
		
		StringBuilder builder = new StringBuilder();
		builder.append("**Latest " + plays + " top plays for " + player + " in the " + OsuTrackCommand.convertMode(Utils.stringToInt(mode)) + " mode**\n\n");
		
		String post = Main.osuRequestManager.sendRequest("https://osu.ppy.sh/api/", "get_user_best?k=" + OsuStatsCommand.apiKey + 
						                                "&u=" + player + "&m=" + mode + "&limit=100&type=string");
		
		if(post == "" || !post.contains("{")){
			Utils.info(e.getChannel(), "Could not fetch top plays (api unavailable?)");
			return;
		}
		
		post = "[" + post + "]";
		
		JSONArray jsonResponse = new JSONArray(post);
		
		HashMap<String, JSONObject> mostRecentPlays = new HashMap<>();
		String oldest = "";
		
		for(int i = jsonResponse.length() - 1; i >= 0; i--){
			JSONObject obj = jsonResponse.getJSONObject(i);
			String osuDate = OsuTrackCommand.osuDateToCurrentDate(obj.getString("date"));
			
			if(mostRecentPlays.size() >= plays){
				if(OsuTrackCommand.dateGreaterThanDate(osuDate, oldest)){
					mostRecentPlays.remove(oldest);
					mostRecentPlays.put(osuDate, obj);
					
					oldest = "";
					
					for(String date : mostRecentPlays.keySet())
						if(oldest.equals("")) oldest = date;
						else if(OsuTrackCommand.dateGreaterThanDate(oldest, date)) oldest = date;
				}
			}else{
				mostRecentPlays.put(osuDate, obj);
				
				oldest = "";
				
				for(String date : mostRecentPlays.keySet())
					if(oldest.equals("")) oldest = date;
					else if(OsuTrackCommand.dateGreaterThanDate(oldest, date)) oldest = date;
			}
		}
		
		if(mostRecentPlays.size() > 0){
			int size = mostRecentPlays.size();
			for(int i = 0; i < size; i++){
				String date = getMostRecentDate(mostRecentPlays.keySet());
				JSONObject obj = mostRecentPlays.get(date);
				String play = "";
				JSONObject map = Map.getMapInfo(obj.getInt("beatmap_id"), false);
				
				mostRecentPlays.remove(date);
				
				play += "**" + getTimeDifference(date) + "** | **" + Utils.df(obj.getDouble("pp"), 2) + "pp**\n" + 
						map.getString("artist") + " - " + map.getString("title") + " [" +
				        map.getString("version") + "] " + Mods.getMods(obj.getInt("enabled_mods")) +
				        "\n" + (mode.equals("2") ? "" : Utils.df(OsuTrackCommand.getAccuracy(obj)) + "% | ") + 
				        (obj.getInt("perfect") == 0 ? obj.getInt("maxcombo") + "/" + (map.isNull("max_combo") ? "null" : map.getInt("max_combo")) : "FC") +
				        " | " + obj.getString("rank").replace("X", "SS") + " rank | " + (mode.equals("2") ? "" : (obj.getInt("count100") > 0 ? obj.getInt("count100") + 
				        "x100 " : "") + (obj.getInt("count50") > 0 ? obj.getInt("count50") + "x50 " : "")) + 
				        (obj.getInt("countmiss") > 0 ? obj.getInt("countmiss") + "x miss " : "") + "\nMap: <http://osu.ppy.sh/b/" + obj.getInt("beatmap_id") + ">\n\n";
				
				if(builder.toString().length() > 1998){
					seperatePosts.add(builder.toString());
					builder = new StringBuilder();
				}
				
				builder.append(play);
			}
			
			seperatePosts.add(builder.toString());
			
			for(String msg : seperatePosts)
				Utils.infoBypass(e.getChannel(), msg);
		}else Utils.info(e.getChannel(), "Could not fetch top plays (api unavailable?)");
	}
	
	public String getMostRecentDate(Set<String> dates){
		String recent = "";
		
		for(String date : dates) 
			if(recent.equals("")) recent = date;
			else if(OsuTrackCommand.dateGreaterThanDate(date, recent)) recent = date;
		
		return recent;
	}
	
	public String getTimeDifference(String osuDate){
		DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
		
		Date date = null;
		
		try{
			date = formatter.parse(osuDate);
		}catch(ParseException e){
			Log.logger.log(Level.SEVERE, e.getMessage(), e);
		}
		
		long difference = Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTime().getTime() - date.getTime();
		String time = "";
		
		int years = (int) (difference / 31557600000L);
		if(years > 0) time = years + "y";
		
		difference -= years * 31557600000L;
		
		int months = (int) (difference / 2592000000L);
		if(months > 0) time += months + "M";
		
		difference -= months * 2592000000L;
		
		int days = (int) (difference / 86400000);
		if(days > 0) time += days + "d";
		
		difference -= (long) days * 86400000L;
		
		int hours = (int) (difference / 3600000);
		if(hours > 0) time += hours + "h";
		
		difference -= (long) hours * 3600000L;
		
		int minutes = (int) (difference / 60000);
		if(minutes > 0) time += minutes + "m";
		
		difference -= (long) minutes * 60000L;
		
		int seconds = (int) (difference / 1000);
		if(seconds > 0) time += seconds + "s";
		
		return time;
	}
	
}
