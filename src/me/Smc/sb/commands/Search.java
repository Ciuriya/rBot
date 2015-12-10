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
		String fQuery = query.replaceFirst(searchService, "");
		Thread t = new Thread(new Runnable(){
			@SuppressWarnings("deprecation")
			public void run(){
				Message msg = null;
				switch(searchService.toLowerCase()){
					case "google": msg = google(e, fQuery, dm).build(); break;
					case "konachan": msg = konachan(e, fQuery).build(); break;
					case "hentai": msg = hentai(e, fQuery).build(); break;
					case "e621": msg = e621(e, fQuery).build(); break;
					default: break;
				}
				if(msg != null) Utils.infoBypass(e.getGroup(), msg.getMessage());
				Thread.currentThread().stop();
			}
		});
		t.start();
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
	
	private static MessageBuilder konachan(UserChatEvent e, String query){
		String domain = "com";
		if(query.contains("{domain=")){
			domain = query.split("\\{domain=")[1].split("}")[0];
			query = query.replaceFirst("\\{domain=" + domain + "}", "");
		}
		if(!domain.equalsIgnoreCase("com") && !domain.equalsIgnoreCase("net")) return new MessageBuilder().addString("Invalid domain! Use com or net!");
		String[] page = Utils.getHTMLCode("https://konachan." + domain +  "/post");
		int maxId = Integer.parseInt(Utils.getNextLineCodeFromLink(page, 1, "Post.register_tags").get(0).split("register\\(\\{\"id\"")[1].split(",\"tags\"")[0].substring(1));
		return checkRandomPost(e, maxId, domain, query, 0);
	}
	
	private static MessageBuilder checkRandomPost(UserChatEvent e, int maxId, String domain, String query, int hops){
		if(hops >= 50) return new MessageBuilder().addString("Could not find matching image in 50 tries!"); 
		int id = (int) (new Random().nextDouble() * (maxId - 1) + 1);
		String[] imagePage = Utils.getHTMLCode("https://konachan." + domain + "/post/show/" + id + "/");
		query = Utils.removeStartSpaces(query);
		if(hasTags(imagePage, query)){
			MessageBuilder builder = new MessageBuilder();
			ArrayList<String> highres = Utils.getNextLineCodeFromLink(imagePage, 0, "Click on the <a class=\"highres-show\" href=\"");
			if(highres.size() > 0) builder.addString("Searched by " + e.getUser().getUser().getUsername() + "\n" + highres.get(0).split("href=\"")[1].split("\">View larger version")[0]);
			else{
				ArrayList<String> line = Utils.getNextLineCodeFromLink(imagePage, 0, "meta content=\"http://konachan.com/image");
				if(line.size() > 0) builder.addString("Searched by " + e.getUser().getUser().getUsername() + "\n" + line.get(0).split("meta content=\"")[1].split("\" property=\"")[0]);
				else builder.addString("Searched by " + e.getUser().getUser().getUsername() + "\nPost error on https://konachan." + domain + "/post/show/" + id + "/");
			}
			return builder;
		}else return checkRandomPost(e, maxId, domain, query, hops + 1);
	}
	
	private static boolean hasTags(String[] html, String query){
		if(query.replaceAll(" ", "").length() == 0) return true;
		ArrayList<String> line = Utils.getNextLineCodeFromLink(html, 0, "property=\"og:description\" />");
		if(line.size() == 0) return false;
		String tags = line.get(0);
		for(String tag : query.split(" "))
			if(!tags.contains(tag)) return false;
		return true;
	}
	
	private static MessageBuilder hentai(UserChatEvent e, String query){
		String url = "http://g.e-hentai.org/";
		String params = "";
		String[] types = null;
		if(query.contains("{type=")){
			types = query.split("\\{type=")[1].split("}")[0].split(",");
			query = query.replaceFirst("\\{type=" + query.split("\\{type=")[1].split("}")[0] + "}", "");
		}
		for(String str : getHentaiTypes()){
			int on = 0;
			if(types != null) for(String s : types)
				if(s.toLowerCase().replaceAll(" ", "").equalsIgnoreCase(str))
					on = 1;
			params += "&f_" + str + "=" + on;
		}
		params += "&f_search=" + query.replaceAll(" ", "+") + "&f_apply=Apply+Filter";
		String gallery = findHentai(url, params);
		return new MessageBuilder().addString("Searched by " + e.getUser().getUser().getUsername() + "\n" + gallery);
	}
	
	private static String findHentai(String url, String params){
		String[] html = Utils.getHTMLCode(url + "?page=0" + params);
		int pages = (int) Math.floor(Utils.stringToInt(Utils.getNextLineCodeFromLink(html, 0, "Showing 1-25 of ").get(0).split("<p class=\"ip\" style=\"margin-top\\:5px\">Showing 1-25 of ")[1].split("</p>")[0].replace(",", "")) / 25);
		int page = (int) (new Random().nextDouble() * pages);
		String[] randomHtml = Utils.getHTMLCode(url + "?page=" + page + params);
		String line = Utils.getNextLineCodeFromLink(randomHtml, 0, "<tr class=\"gtr0\"><td class=\"itdc\">").get(0);
		String[] split = line.split("<div class=\"it5\"><a href=\"");
		int random = (int) (new Random().nextDouble() * 25 + 1);
		String gallery = "Error!";
		for(int i = 1; i < split.length; i += 2)
			if((i + 1) / 2 >= random){
				gallery = split[i].split("\" onmouseover")[0];
				break;
			}
		if(gallery.equalsIgnoreCase("Error!")) return findHentai(url, params);
		else return gallery;
	}
	
	private static String[] getHentaiTypes(){
		return new String[]{"doujinshi", "manga", "artistcg", "gamecg", "western", "non-h", "imageset", "cosplay", "asianporn", "misc"};
	}
	
	private static MessageBuilder e621(UserChatEvent e, String query){
		int page = 1;
		String url = "https://e621.net/post/index/";
		String params = "";
		if(query.split(" ").length > 0) params += "/" + query;
		String[] html = Utils.getHTMLCode(url);
		int lastPage = Utils.stringToInt(Utils.getNextLineCodeFromLink(html, 0, "rel=\"last\" title=\"Last Page\">").get(0).split("href=\"/post/index/")[1].split("\" rel=\"")[0]);
		page = (int) (new Random().nextDouble() * (lastPage - 1) + 1);
		String[] pageHtml = Utils.getHTMLCode(url + page + params);
		int num = (int) (new Random().nextDouble() * 74);
	    int numToLookFor = Utils.stringToInt(Utils.getNextLineCodeFromLink(pageHtml, 0, "<span class=\"thumb\" id=\"p").get(0).split("\"thumb\" id=\"p")[1].split("\"><a")[0]) - num;
	    String line = Utils.getNextLineCodeFromLink(pageHtml, 0, "<span class=\"thumb\" id=\"p" + numToLookFor + "\">").get(0);
	    return new MessageBuilder().addString("Searched by " + e.getUser().getUser().getUsername() + "\nhttp://e621.net" + line.split("href=\"")[1].split("\" onclick=")[0]);
	}
	
}
