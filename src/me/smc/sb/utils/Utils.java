package me.smc.sb.utils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import org.apache.commons.lang3.StringEscapeUtils;
import org.pircbotx.PircBotX;
import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.PrivateMessageEvent;

import me.smc.sb.main.Main;
import me.smc.sb.multi.Tournament;
import net.dv8tion.jda.MessageBuilder;
import net.dv8tion.jda.Permission;
import net.dv8tion.jda.entities.Message;
import net.dv8tion.jda.entities.MessageChannel;
import net.dv8tion.jda.entities.PrivateChannel;
import net.dv8tion.jda.entities.TextChannel;
import net.dv8tion.jda.entities.User;
import net.dv8tion.jda.events.message.MessageReceivedEvent;

public class Utils{

    public static boolean checkArguments(MessageReceivedEvent e, String[] args, int length){
		if(args.length < length){
			Utils.error(e.getChannel(), e.getAuthor(), " Invalid arguments!");
			return false;
		}
		return true;
    }
    
    public static boolean checkArguments(MessageEvent<PircBotX> e, PrivateMessageEvent<PircBotX> pe, String discord, String[] args, int length){
		if(args.length < length){
			Utils.info(e, pe, discord, "Invalid arguments, use !help for more info!");
			return false;
		}
		return true;
    }
	
	public static void error(MessageChannel channel, User user, String message){
		channel.sendMessage(new MessageBuilder().appendString(message).build());
		Log.logger.log(Level.INFO, "{Error sent in " + getGroupLogString(channel) + " to " + user.getUsername() + " } " + message);
		Main.messagesSentThisSession++;
	}
	
	public static void infoBypass(MessageChannel channel, String message){
		channel.sendMessage(message);
		Log.logger.log(Level.INFO, "{Message sent in " + getGroupLogString(channel) + "} " + message);
		Main.messagesSentThisSession++;
	}
	
	public static void info(MessageChannel channel, String message){
		if(channel instanceof TextChannel){
			if(!Main.serverConfigs.get(((TextChannel) channel).getGuild().getId()).getBoolean("silent")){
				channel.sendMessage(message);
				Log.logger.log(Level.INFO, "{Message sent in " + getGroupLogString(channel) + "} " + message);
			}else Log.logger.log(Level.INFO, "{Silent message sent in " + getGroupLogString(channel) + "} " + message);
		}else{
			channel.sendMessage(message); 
			Log.logger.log(Level.INFO, "{Message sent in " + getGroupLogString(channel) + "} " + message);
		}
		Main.messagesSentThisSession++;
	}
	
	public static void info(MessageEvent<PircBotX> e, PrivateMessageEvent<PircBotX> pe, String discord, String message){
		if(e != null){
			e.respond(message);
			Log.logger.log(Level.INFO, "{IRC message sent in channel " + e.getChannel().getName() + "} " + message);
		}else if(pe != null){
			Main.ircBot.sendIRC().message(toUser(e, pe), message);
			Log.logger.log(Level.INFO, "{IRC PM sent to user " + toUser(e, pe) + "} " + message);
		}else
			if(Main.api.getPrivateChannelById(discord) != null)
				infoBypass(Main.api.getPrivateChannelById(discord), message);
			else infoBypass(Main.api.getTextChannelById(discord), message);
	}
	
	public static String toUser(MessageEvent<PircBotX> e, PrivateMessageEvent<PircBotX> pe){
		if(pe != null) return pe.getUser().getNick();
		else if(e != null) return e.getUser().getNick();
		else return null;
	}
	
	public static Message toMessage(String str){
		return new MessageBuilder().appendString(str).build();
	}
	
	public static String getGroupLogString(MessageChannel channel){
		if(channel instanceof PrivateChannel)
			return "Private/" + ((PrivateChannel) channel).getUser().getUsername();
		
		String serverName = ((TextChannel) channel).getGuild().getName() + "|||";
		return serverName + ((TextChannel) channel).getName();
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
	
	public static double stringToDouble(String str){
		try{
			return Double.parseDouble(str);
		}catch(Exception e){
			return -1.0;
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
			connection.setConnectTimeout(30000);
			connection.setReadTimeout(30000);
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
	
	public static long toTime(String date){
		long time = -1;
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy MM dd HH");
		sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
		try{
			time = sdf.parse(date).getTime();
		}catch(ParseException e){
			Log.logger.log(Level.SEVERE, e.getMessage(), e);
		}
		return time;
	}
	
	public static long getCurrentTimeUTC(){
		Calendar c = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		return c.getTime().getTime();
	}
	
	public static String fixString(String str){
		String s = StringEscapeUtils.unescapeJava(StringEscapeUtils.unescapeHtml4(str.replaceAll("\\s+", " ").replaceAll("\\<.*?>", "").replaceAll("\"", "")));
		return s;
	}
	
	public static double df(double num){
		DecimalFormat df = new DecimalFormat("#.##", new DecimalFormatSymbols(Locale.US));
		return stringToDouble(df.format(num));
	}
	
	public static String toTwoDigits(int num){
		if(num < 10) return "0" + num;
		return String.valueOf(num);
	}
	
	public static String validateTournamentAndTeam(MessageEvent<PircBotX> e, PrivateMessageEvent<PircBotX> pe, String discord, String[] args){
		boolean valid = false;
		for(String arg : args) 
			if(arg.contains("{"))
				valid = true;
		
		if(!valid){Utils.info(e, pe, discord, "Invalid team name!"); return "";}
		
		String tournamentName = "";
		int o = 0;
		
		for(int i = 0; i < args.length; i++) 
			if(args[i].contains("{")){o = i; break;} 
			else tournamentName += args[i] + " ";
		Tournament t = Tournament.getTournament(tournamentName.substring(0, tournamentName.length() - 1));
		
		if(t == null){Utils.info(e, pe, discord, "Invalid tournament!"); return "";}
		
		String teamName = "";
		
		for(int i = o; i < args.length; i++) 
			if(args[i].contains("}")){
				teamName += args[i].replace("}", "").replace("}", "") + " ";
				break;
			}else teamName += args[i].replace("{", "") + " ";
		
		if(teamName.length() == 0){Utils.info(e, pe, discord, "Invalid team name!"); return "";}
		
		return teamName.substring(0, teamName.length() - 1) + "|" + tournamentName.substring(0, tournamentName.length() - 1);
	}
	
	public static void sleep(int ms){
		try{
			Thread.sleep(ms);
		}catch(Exception e){
			Log.logger.log(Level.SEVERE, e.getMessage(), e);
		}
	}
	
	public static void deleteMessage(MessageChannel channel, Message m){
		if(channel instanceof TextChannel)
			if(((TextChannel) channel).checkPermission(Main.api.getSelfInfo(), Permission.MESSAGE_MANAGE))
				m.deleteMessage();
	}
	
}
