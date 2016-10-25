package me.smc.sb.utils;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.InputStreamReader;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import javax.net.ssl.HttpsURLConnection;

import org.apache.commons.lang3.StringEscapeUtils;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.pircbotx.Channel;
import org.pircbotx.PircBotX;
import org.pircbotx.hooks.Event;
import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.PrivateMessageEvent;

import me.smc.sb.communication.Server;
import me.smc.sb.discordcommands.OsuStatsCommand;
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
			Utils.error(e.getChannel(), e.getAuthor(), "Invalid arguments!");
			return false;
		}
		
		return true;
    }
    
    public static String checkArguments(String[] args, int length){
		if(args.length < length){
			return "Invalid arguments, use !help for more info!";
		}
		
		return "";
    }
	
	public static void error(MessageChannel channel, User user, String message){
		channel.sendMessage(new MessageBuilder().appendString(message).build());
		
		Log.logger.log(Level.INFO, "{Error sent in " + getGroupLogString(channel) + " to " + user.getUsername() + " } " + message);
		Main.messagesSentThisSession++;
	}
	
	public static Message infoBypass(MessageChannel channel, String message){
		Message jdaMsg = channel.sendMessage(message);
		
		Log.logger.log(Level.INFO, "{Message sent in " + getGroupLogString(channel) + "} " + message);
		Main.messagesSentThisSession++;
		
		return jdaMsg;
	}
	
	public static Message info(MessageChannel channel, String message){
		Message jdaMsg = null;
		
		if(channel instanceof TextChannel){
			if(!Main.serverConfigs.get(((TextChannel) channel).getGuild().getId()).getBoolean("silent")){
				jdaMsg = channel.sendMessage(message);
				Log.logger.log(Level.INFO, "{Message sent in " + getGroupLogString(channel) + "} " + message);
			}else Log.logger.log(Level.INFO, "{Silent message sent in " + getGroupLogString(channel) + "} " + message);
		}else{
			jdaMsg = channel.sendMessage(message); 
			Log.logger.log(Level.INFO, "{Message sent in " + getGroupLogString(channel) + "} " + message);
		}
		
		Main.messagesSentThisSession++;
		
		return jdaMsg;
	}
	
	public static Message info(MessageEvent<PircBotX> e, PrivateMessageEvent<PircBotX> pe, String discord, String message){
		Message jdaMsg = null;
		
		if(message.length() == 0) return jdaMsg;
		
		if(isTwitch(e)){
			Main.twitchRegulator.sendMessage(e.getChannel().getName().replace("#", ""), message);
			return jdaMsg;
		}
		
		if(e != null && verifyChannel(e)){
			e.getChannel().send().message(message);
			Log.logger.log(Level.INFO, "{IRC message sent in channel " + e.getChannel().getName() + "} " + message);
		}else if(pe != null){
			Main.ircBot.sendIRC().message(toUser(e, pe), message);
			Log.logger.log(Level.INFO, "{IRC PM sent to user " + toUser(e, pe) + "} " + message);
		}else if(discord != null)
			if(Main.api.getPrivateChannelById(discord) != null)
				jdaMsg = infoBypass(Main.api.getPrivateChannelById(discord), message);
			else jdaMsg = infoBypass(Main.api.getTextChannelById(discord), message);
		else{
			for(Server server : Main.servers)
				server.sendMessage(message.replaceAll("\n", "|"));
			
			Log.logger.log(Level.INFO, "{Message sent to websites} " + message);
		}
		
		return jdaMsg;
	}
	
	public static boolean verifyChannel(MessageEvent<PircBotX> e){
		if(!e.getChannel().getName().startsWith("#mp_")){
			e.getChannel().send().part();
			return false;
		}
		
		return true;
	}
	
	public static boolean verifyChannel(String channel){
		if(!channel.startsWith("#mp_")){
			for(Channel c : Main.ircBot.getUserBot().getChannels())
				if(c.getName().equalsIgnoreCase(channel))
					c.send().part();
			return false;
		}
		
		return true;
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
		return sendPost(urlString, urlParameters, "");
	}
	
	public static String sendPost(String urlString, String urlParameters, String query){
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
			connection.setDoOutput(true);
			
			if(query.length() > 0) connection.getOutputStream().write(query.getBytes("UTF8"));
			
			BufferedReader inputStream = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			StringBuffer response = new StringBuffer();
			char[] buffer = new char[1024];
			int charsRead = 0;
			
			while((charsRead  = inputStream.read(buffer, 0, 1024)) != -1)
				response.append(buffer, 0, charsRead);
			
			/*String inputLine;
			
			while((inputLine = inputStream.readLine()) != null) response.append(inputLine);*/
			
			inputStream.close();
			
			response.deleteCharAt(0);
			response.deleteCharAt(response.length() - 1);
			
			answer = response.toString();
		}catch(Exception e){
			e.printStackTrace();
		}
		
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
			try{
				if(in != null) in.close();
			}catch(Exception e){
				e.printStackTrace();
			}
		}
		
		Main.htmlScrapes++;
		if(link.contains("osu.ppy.sh")) Main.osuHtmlScrapes++;
		
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
					try{
						allLines.add(lines[i + offsetLine]);
					}catch(Exception e){
						e.printStackTrace();
					}
					break;
				}
		
		if(allLines.size() > 0) return allLines;
		else return new ArrayList<String>();
	}
	
	public static String veryLongNumberDisplay(long number){
		if(number < 1000 && number > -1000) return String.valueOf(number);
		
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
	
	public static String veryLongNumberDisplay(double number){
		String[] splitNumber = String.valueOf(number).split("\\.");
		return veryLongNumberDisplay(Utils.stringToInt(splitNumber[0])) + "." + splitNumber[1];
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
		return toTime(date, "yyyy MM dd HH mm");
	}
	
	public static long toTime(String date, String format){
		long time = -1;
		SimpleDateFormat sdf = new SimpleDateFormat(format);
		sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
		try{
			time = sdf.parse(date).getTime();
		}catch(ParseException e){
			Log.logger.log(Level.SEVERE, e.getMessage(), e);
		}
		return time;
	}
	
	public static String toDate(long time){
		return toDate(time, "yyyy/MM/dd HH:mm");
	}
	
	public static String toDate(long time, String format){
		String date = "";
		SimpleDateFormat sdf = new SimpleDateFormat(format);
		sdf.setCalendar(getCalendar(time));
		sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
		date = sdf.format(time);
		return date;
	}
	
	private static Calendar getCalendar(long time){
		Calendar calendar = new GregorianCalendar();
		calendar.setTimeZone(TimeZone.getTimeZone("UTC"));
		calendar.setTimeInMillis(time);
		return calendar;
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
		return stringToDouble(df(num, 2));
	}
	
	public static String df(double num, double decimals){
		String format = "#";
		
		if(decimals > 0){
			format += ".";
			
			for(int i = 0; i < decimals; i++)
				format += "#";
		}
		
		DecimalFormat df = new DecimalFormat(format, new DecimalFormatSymbols(Locale.US));
		df.setNegativePrefix("-");
		return df.format(num);
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
		
		if(!valid) return "Invalid team name!";
		
		String tournamentName = "";
		int o = 0;
		
		for(int i = 0; i < args.length; i++) 
			if(args[i].contains("{")){o = i; break;} 
			else tournamentName += args[i] + " ";
		Tournament t = Tournament.getTournament(tournamentName.substring(0, tournamentName.length() - 1));
		
		if(t == null) return "Invalid tournament!";
		
		String teamName = "";
		
		for(int i = o; i < args.length; i++) 
			if(args[i].contains("}")){
				teamName += args[i].replace("}", "").replace("}", "") + " ";
				break;
			}else teamName += args[i].replace("\\{", "") + " ";
		
		if(teamName.length() == 0) return "Invalid team name!";
		
		return teamName.substring(0, teamName.length() - 1).replaceAll("\\{", "") + "|" + tournamentName.substring(0, tournamentName.length() - 1);
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
	
	public static int fetchRandom(int min, int max){
		return new Random().nextInt(max - min + 1) + min;
	}
	
	public static float fetchRandom(float min, float max){
		return new Random().nextFloat() * (max - min + 1) + min;
	}
	
	public static String takeOffExtrasInBeatmapURL(String url){
		if(url.endsWith("m=0") || url.endsWith("m=1") || url.endsWith("m=2") || url.endsWith("m=3"))
			return url.substring(0, url.length() - 4);
		return url;
	}
	
	public static String removeExcessiveSpaces(String message){
		if(message.contains("  ")){
			message = message.replaceAll("  ", " ");
			if(message.contains("  ")) return removeExcessiveSpaces(message);
			else return message;
		}else return message;
	}
	
	public static String getFinalURL(String url){
		try{
		    HttpURLConnection con = (HttpURLConnection) new URL(url).openConnection();
		    con.setInstanceFollowRedirects(false);
		    con.connect();
		    con.getInputStream();

		    if(con.getResponseCode() == HttpURLConnection.HTTP_MOVED_PERM || con.getResponseCode() == HttpURLConnection.HTTP_MOVED_TEMP){
		        String redirectUrl = con.getHeaderField("Location");
		        return getFinalURL(redirectUrl);
		    }
		    
		    return url;	
		}catch(Exception e){
			Log.logger.log(Level.SEVERE, e.getMessage(), e);
			return url;
		}
	}
	
	public static String getOsuPlayerPPAndRank(String id, int mode){
		String post = Utils.sendPost("https://osu.ppy.sh/api/", "get_user?k=" + OsuStatsCommand.apiKey + 
				  	  "&u=" + id + "&m=" + mode + "&type=id&event_days=1");
		
		if(post == "" || !post.contains("{")) return "-1&r=-1&cr=-1";
		
		JSONObject jsonResponse = new JSONObject(post);
		return jsonResponse.getDouble("pp_raw") + "&r=" + jsonResponse.getInt("pp_rank") + "&cr=" + jsonResponse.getInt("pp_country_rank");
	}
	
	public static int getOsuPlayerRank(String name, int mode){
		String id = getOsuPlayerId(name);
		
		if(id.equals("-1")) return -1;
		
		String[] pageProfile = Utils.getHTMLCode("https://osu.ppy.sh/pages/include/profile-general.php?u=" + id + "&m=" + mode);
		ArrayList<String> line = getNextLineCodeFromLink(pageProfile, 0, "Performance</a>: ");
		
		if(line.isEmpty()) return getOsuPlayerRankByAPI(name, id, mode);
		else{
			int rank = -1;
			
			try{
				rank = stringToInt(line.get(0).split("\\(#")[1].split("\\)")[0].replaceAll(",", ""));
			}catch(Exception e){
				return -1;
			}
			
			if(rank == -1) return getOsuPlayerRankByAPI(name, id, mode);
			else return rank;
		}
	}
	
	private static int getOsuPlayerRankByAPI(String name, String id, int mode){
		String post = Utils.sendPost("https://osu.ppy.sh/api/", "get_user?k=" + OsuStatsCommand.apiKey + 
					  "&u=" + name.replaceAll(" ", "%20") + "&m=" + mode + "&type=string&event_days=1");
		
		if(post == "" || !post.contains("{")) return -1;
		
		JSONObject jsonResponse = new JSONObject(post);
		return jsonResponse.getInt("pp_rank");
	}
	
	public static String getOsuPlayerId(String name){
		String[] pageProfile = Utils.getHTMLCode("https://osu.ppy.sh/u/" + name);
		ArrayList<String> line = getNextLineCodeFromLink(pageProfile, 0, "var userId =");
		
		try{
			if(line.isEmpty()) return "-1";
			else return line.stream().findFirst().orElse("= -1;").split("= ")[1].split(";")[0];	
		}catch(Exception e){
			return "-1";
		}
	}
	
	public static boolean isTwitch(Event<PircBotX> e){
		if(e != null && e.getBot().getBotId() == Main.twitchBot.getBotId())
			return true;
		
		return false;
	}
	
	public static class Login{
		private List<String> cookies;
		private HttpsURLConnection conn;
		
		public static void osu(){
			CookieHandler.setDefault(new CookieManager());
			
			Login login = new Login();
			
			String url = "https://osu.ppy.sh/forum/ucp.php?mode=login";
			
			Configuration cfg = new Configuration(new File("login.txt"));
			
			String page = login.getPageContent(url);
			
			String postParams = login.getFormParams(page, cfg.getValue("osuWebUser"), 
														  cfg.getValue("osuWebPass"));
			
			login.sendPost(url, "osu.ppy.sh", postParams);
		}
		
		private void sendPost(String url, String host, String postParams){
			try{
				URL obj = new URL(url);
				conn = (HttpsURLConnection) obj.openConnection();

				conn.setUseCaches(false);
				conn.setRequestMethod("POST");
				conn.setRequestProperty("Host", host);
				conn.setRequestProperty("User-Agent", "Mozilla/5.0");
				conn.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
				conn.setRequestProperty("Accept-Language", "fr-FR,fr;q=0.8,en-US;q=0.6,en;q=0.4");
				
				for(String cookie : this.cookies) 
					conn.addRequestProperty("Cookie", cookie.split(";", 1)[0]);
				
				conn.setRequestProperty("Connection", "keep-alive");
				conn.setRequestProperty("Referer", "https://osu.ppy.sh/forum/ucp.php?mode=login");
				conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
				conn.setRequestProperty("Content-Length", Integer.toString(postParams.length()));

				conn.setDoOutput(true);
				conn.setDoInput(true);

				DataOutputStream wr = new DataOutputStream(conn.getOutputStream());
				wr.writeBytes(postParams);
				wr.flush();
				wr.close();

				BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
				String inputLine;
				StringBuffer response = new StringBuffer();

				while((inputLine = in.readLine()) != null) response.append(inputLine);
				
				in.close();	
			}catch(Exception ex){
				Log.logger.log(Level.SEVERE, ex.getMessage(), ex);
			}
		}
		
		private String getPageContent(String url){
			try{
				URL obj = new URL(url);
				conn = (HttpsURLConnection) obj.openConnection();

				conn.setRequestMethod("GET");
				conn.setUseCaches(false);

				conn.setRequestProperty("User-Agent", "Mozilla/5.0");
				conn.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
				conn.setRequestProperty("Accept-Language", "fr-FR,fr;q=0.8,en-US;q=0.6,en;q=0.4");
				
				if(cookies != null)
					for(String cookie : this.cookies)
						conn.addRequestProperty("Cookie", cookie.split(";", 1)[0]);

				BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
				String inputLine;
				StringBuffer response = new StringBuffer();

				while ((inputLine = in.readLine()) != null) response.append(inputLine);
				
				in.close();

				setCookies(conn.getHeaderFields().get("Set-Cookie"));

				return response.toString();
			}catch(Exception ex){
				Log.logger.log(Level.SEVERE, ex.getMessage(), ex);
				return "";
			}
		}
		
		public String getFormParams(String html, String username, String password){
			Document doc = Jsoup.parse(html);

			Element loginForm = null;
			
			for(Element element : doc.getAllElements())
				if(element.attr("action").equalsIgnoreCase("/forum/ucp.php?mode=login")){
					loginForm = element;
					break;
				}
			
			if(loginForm == null) return "";
					
			Elements inputElements = loginForm.getElementsByTag("input");
			List<String> paramList = new ArrayList<String>();
			
			for(Element inputElement : inputElements){
				String key = inputElement.attr("name");
				String value = inputElement.attr("value");

				if (key.equals("username"))
					value = username;
				else if (key.equals("password"))
					value = password;
				else if(key.equals("autologin") || key.equals("viewonline"))
					value = "do not";
				
				try{
					if(!value.equalsIgnoreCase("do not")) 
						paramList.add(key + "=" + URLEncoder.encode(value, "UTF-8"));
				}catch(Exception ex){
					Log.logger.log(Level.SEVERE, ex.getMessage(), ex);
				}
			}

			StringBuilder result = new StringBuilder();
			for(String param : paramList)
				if(result.length() == 0) result.append(param);
				else result.append("&" + param);
			
			return result.toString();
		}
		
		public List<String> getCookies(){
			return cookies;
		}

		public void setCookies(List<String> cookies){
			this.cookies = cookies;
		}
	}
	
}
