package me.smc.sb.utils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import org.apache.commons.lang3.StringEscapeUtils;

import me.itsghost.jdiscord.events.UserChatEvent;
import me.itsghost.jdiscord.message.Message;
import me.itsghost.jdiscord.message.MessageBuilder;
import me.itsghost.jdiscord.talkable.Group;
import me.itsghost.jdiscord.talkable.User;
import me.smc.sb.main.Main;

public class Utils{

    public static boolean checkArguments(UserChatEvent e, String[] args, int length){
		if(args.length < length){
			Utils.error(e.getGroup(), e.getUser().getUser(), " Invalid arguments!");
			return false;
		}
		return true;
    }
	
	public static void error(Group group, User user, String message){
		group.sendMessage(new MessageBuilder().addString(message).build());
		Log.logger.log(Level.INFO, "{Error sent in " + getGroupLogString(group) + " to " + user.getUsername() + " } " + message);
		Main.messagesSentThisSession++;
	}
	
	public static void infoBypass(Group group, String message){
		infoBypass(group, toMessage(message));
	}
	
	public static void infoBypass(Group group, Message message){
		group.sendMessage(message);
		Log.logger.log(Level.INFO, "{Message sent in " + getGroupLogString(group) + "} " + message);
		Main.messagesSentThisSession++;
	}
	
	public static void info(Group group, String message){
		info(group, toMessage(message));
	}
	
	public static void info(Group group, Message message){
		if(group.getServer() != null){
			if(!Main.serverConfigs.get(group.getServer().getId()).getBoolean("silent")){
				group.sendMessage(message);
				Log.logger.log(Level.INFO, "{Message sent in " + getGroupLogString(group) + "} " + message);
			}else Log.logger.log(Level.INFO, "{SILENT Message sent in " + getGroupLogString(group) + "} " + message);
		}else{
			group.sendMessage(message); 
			Log.logger.log(Level.INFO, "{Message sent in " + getGroupLogString(group) + "} " + message);
		}
		Main.messagesSentThisSession++;
	}
	
	public static Message toMessage(String str){
		return new MessageBuilder().addString(str).build();
	}
	
	public static String getGroupLogString(Group group){
		String serverName = isPM(group) ? "" : (group.getServer().getName() + "|||");
		return serverName + group.getName();
	}
	
	public static boolean isPM(Group group){
		if(group.getServer() == null) return true;
		return false;
	}
	
	public static String removeStartSpaces(String str){
		String s = new String(str);
		while(s.startsWith(" "))
			s = s.substring(1);
		return s;
	}
	
	public static int stringToInt(String str){
		try{
			return Integer.parseInt(str);
		}catch(Exception e){
			return -1;
		}
	}
	
	public static String addPrefix(String prefix, String command){
		return prefix + command;
	}

	public static String getDate(){
		Calendar c = Calendar.getInstance();
		return String.format("[%s-%s-%s | %s:%s:%s]", parse(c.get(Calendar.YEAR)), parse(c.get(Calendar.MONTH) + 1),
				                                      parse(c.get(Calendar.DAY_OF_MONTH)), parse(c.get(Calendar.HOUR_OF_DAY)),
				                                      parse(c.get(Calendar.MINUTE)), parse(c.get(Calendar.SECOND)));
	}
	
	private static String parse(int num){
		if(num < 10) return "0" + num;
		else return "" + num;
	}
	

	public static String sendPost(String urlString, String urlParameters){
		String answer = "";
		try{
			URL url = new URL(urlString + urlParameters);
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod("POST");
			connection.setConnectTimeout(5000);
			connection.setReadTimeout(5000);
			connection.setRequestProperty("User-Agent", "Mozilla/5.0");
			connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
			connection.setRequestProperty("charset", "utf-8");
			connection.setRequestProperty("Content-Length", "" + Integer.toString(urlParameters.getBytes().length));
			BufferedReader inputStream = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			String inputLine;
			StringBuffer response = new StringBuffer();
			while((inputLine = inputStream.readLine()) != null) response.append(inputLine);
			inputStream.close();
			response.deleteCharAt(0);
			response.deleteCharAt(response.length() - 1);
			answer = response.toString();
		}catch(Exception e){e.printStackTrace();}
		return answer;
	}
	
	public static String[] getHTMLCode(String link){
		BufferedReader in = null;
		String[] toReturn = new String[]{};
		try{
			URL url = new URL(link);
			StringBuilder page = new StringBuilder();
			String str = null;
			URLConnection connection = url.openConnection();
			connection.setRequestProperty("User-Agent", "Mozilla/5.0");
			connection.setRequestProperty("Accept-Language", "en-US");
			connection.setConnectTimeout(30000);
			connection.setReadTimeout(30000);
			in = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"));
			while((str = in.readLine()) != null) page.append(str + "\n");
			toReturn = page.toString().split("\n");
		}catch(Exception e){
			e.printStackTrace();
		}finally{
			try{if(in != null) in.close();
			}catch(Exception e){e.printStackTrace();}
		}
		return toReturn;
	}
	
	public static ArrayList<String> getNextLineCodeFromLink(final String link, final int offsetLine, final String... gets){
		return getNextLineCodeFromLink(getHTMLCode(link), offsetLine, gets);
	}

	public static ArrayList<String> getNextLineCodeFromLink(String[] lines, int offsetLine, String... gets){
		ArrayList<String> allLines = new ArrayList<String>();
		for(int i = 0; i < lines.length; i++)
			for(String get : gets)
				if(lines[i].contains(get)){
					try{allLines.add(lines[i + offsetLine]);
					}catch(Exception e){e.printStackTrace();}
					break;
				}
		if(allLines.size() > 0) return allLines;
		else return new ArrayList<String>();
	}
	
	public static String veryLongNumberDisplay(long number){
		String toDisplay = "";
		int remainder = String.valueOf(number).length() % 3;
		int untilSplit = 3;
		if(remainder > 0) untilSplit = remainder;
		for(char c : String.valueOf(number).toCharArray()){
			toDisplay += String.valueOf(c);
			if(untilSplit == 1){
				toDisplay += ",";
				untilSplit = 3;
			}else untilSplit--;
		}
		if(toDisplay.endsWith(",")) toDisplay = toDisplay.substring(0, toDisplay.length() - 1);
		return toDisplay;	
	}
	
	public static String toDuration(long time){
		long millis = time;
        long days = TimeUnit.MILLISECONDS.toDays(millis);
        millis -= TimeUnit.DAYS.toMillis(days);
        long hours = TimeUnit.MILLISECONDS.toHours(millis);
        millis -= TimeUnit.HOURS.toMillis(hours);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis);
        millis -= TimeUnit.MINUTES.toMillis(minutes);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis);
        return days + "d" + hours + "h" + minutes + "m" + seconds + "s";
	}
	
	public static String fixString(String str){
		String s = StringEscapeUtils.unescapeJava(StringEscapeUtils.unescapeHtml4(str.replaceAll("\\s+", " ").replaceAll("\\<.*?>", "").replaceAll("\"", "")));
		return s;
	}
	
}
