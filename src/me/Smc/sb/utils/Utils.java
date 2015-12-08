package me.Smc.sb.utils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.concurrent.TimeUnit;

import me.Smc.sb.main.Main;
import me.itsghost.jdiscord.message.MessageBuilder;
import me.itsghost.jdiscord.talkable.Group;
import me.itsghost.jdiscord.talkable.User;

public class Utils{

	public static void error(Group group, User user, String message){
		group.sendMessage(new MessageBuilder()
				.addUserTag(user, group)
				.addString(message).build());
	}
	
	public static void info(Group group, User user, String message){
		info(group, new MessageBuilder()
				.addUserTag(user, group)
				.addString(message)
				.build()
				.getMessage());
	}
	
	public static void infoBypass(Group group, User user, String message){
		infoBypass(group, new MessageBuilder()
				.addUserTag(user, group)
				.addString(message)
				.build()
				.getMessage());
	}
	
	public static void infoBypass(Group group, String message){
		group.sendMessage(message); 
	}
	
	public static void info(Group group, String message){
		if(group.getServer() != null){
			if(!Main.serverConfigs.get(group.getServer().getId()).getBoolean("silent"))
				group.sendMessage(message);
		}else group.sendMessage(message); 
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
			try{in.close();
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
	
}
