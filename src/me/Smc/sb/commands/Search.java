package me.Smc.sb.commands;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Random;
import java.util.UUID;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import me.Smc.sb.utils.Utils;
import me.itsghost.jdiscord.events.UserChatEvent;
import me.itsghost.jdiscord.message.Message;
import me.itsghost.jdiscord.message.MessageBuilder;

public class Search{

	public static void execute(UserChatEvent e, String searchService, String query, boolean dm){
		Message msg = null;
		switch(searchService.toLowerCase()){
			case "google": msg = google(e, query, dm).build(); break;
			case "konachan": msg = konachan(e).build(); break;
			default: break;
		}
		if(msg != null) Utils.infoBypass(e.getGroup(), msg.getMessage());
	}
	
	private static MessageBuilder google(UserChatEvent e, String query, boolean dm){
		int resultNum = 1;
		if(query.contains("{result=")){
			resultNum = Utils.stringToInt(query.split("\\{result=")[1].split("}")[0]);
			query = query.replaceFirst("\\{result=" + resultNum + "}", "");
		}
		
		if(resultNum > 64){
			if(!dm) Utils.error(e.getGroup(), e.getUser().getUser(), " Result #" + resultNum + " is out of range!");
			else Utils.infoBypass(e.getGroup(), "Result #" + resultNum + " is out of range!");
			return null;
		}
		
		String searchQuery = "";
		String[] split = query.split(" ");
		for(int i = 0; i < split.length; i++)
			searchQuery += " " + split[i];
		searchQuery = searchQuery.substring(1).replaceAll(" ", "+");
		
		JsonArray results = null;
		JsonObject responseData = null;
        try{
            StringBuilder searchURLString = new StringBuilder();
            searchURLString.append("https://ajax.googleapis.com/ajax/services/search/web?v=1.0&q=" + searchQuery + "&rsz=1&start=" + (resultNum - 1));
            
            URL searchURL = new URL(searchURLString.toString());
            URLConnection conn = searchURL.openConnection();
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:39.0) Gecko/20100101 " + UUID.randomUUID().toString().substring(0, 10));

            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder json = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                json.append(line).append("\n");
            }
            in.close();

            JsonElement element = new JsonParser().parse(json.toString());
            responseData = element.getAsJsonObject().getAsJsonObject("responseData");
            results = responseData.getAsJsonArray("results");
        }catch(IOException ex){
            ex.printStackTrace();
        }
        
        MessageBuilder builder = new MessageBuilder();
        JsonObject result = results.get(0).getAsJsonObject();
        builder.addString("Showing result #**" + resultNum + "** out of **" + Utils.fixString(responseData.getAsJsonObject("cursor").get("estimatedResultCount").toString()) + "** results in "
        		          + "**" + Utils.fixString(responseData.getAsJsonObject("cursor").get("searchResultTime").toString()) + "** seconds.\n\n")
        	   .addString("**" + Utils.fixString(result.get("titleNoFormatting").toString()) + "** \n" + Utils.fixString(result.get("url").toString()) + "\n\n")
        	   .addString(Utils.fixString(result.get("content").toString()));
        return builder;
	}
	
	private static MessageBuilder konachan(UserChatEvent e){
		String[] page = Utils.getHTMLCode("https://konachan.com/post");
		int maxId = Integer.parseInt(Utils.getNextLineCodeFromLink(page, 1, "Post.register_tags").get(0).split("register\\(\\{\"id\"")[1].split(",\"tags\"")[0].substring(1));
		int id = (int) (new Random().nextDouble() * (maxId - 1) + 1);
		String[] imagePage = Utils.getHTMLCode("https://konachan.com/post/show/" + id + "/");
		MessageBuilder builder = new MessageBuilder();
		ArrayList<String> highres = Utils.getNextLineCodeFromLink(imagePage, 0, "Click on the <a class=\"highres-show\" href=\"");
		if(highres.size() > 0) builder.addString(highres.get(0).split("href=\"")[1].split("\">View larger version")[0]);
		else builder.addString(Utils.getNextLineCodeFromLink(imagePage, 0, "meta content=\"http://konachan.com/image").get(0).split("meta content=\"")[1].split("\" property=\"")[0]);
		return builder;
	}
	
}
