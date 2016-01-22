package me.smc.sb.discordcommands;

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

import me.itsghost.jdiscord.events.UserChatEvent;
import me.smc.sb.utils.Utils;

public class SearchCommand extends GlobalCommand{

	public SearchCommand(){
		super(null, 
			  " - Lets you search many different sites (including nsfw sites)", 
			  "{prefix}search\nThis command lets you search the internet.\n\n" +
			  "----------\nUsage\n----------\n{prefix}search google {search} - Sends the first google search result\n" + 
			  "{prefix}search google {search} ({result={result number under 64}) - Sends the nth google search result\n" +
			  "{prefix}search konachan (search tags) - Finds a random konachan picture\n" +
			  "{prefix}search hentai (search tags) - Finds a random e-hentai gallery\n" +
			  "{prefix}search hentai (search tags) ({type={e-hentai type w/o spaces}}) - Finds a random e-hentai gallery using types\n" + 
			  "{prefix}search e621 {search} - Finds a random e621 post\n\n" +
			  "----------\nAliases\n----------\n{prefix}lookup",  
			  true, 
			  "search", "lookup");
	}

	@Override
	public void onCommand(UserChatEvent e, String[] args){
		e.getMsg().deleteMessage();
		if(!Utils.checkArguments(e, args, 1)) return;
		Thread t = new Thread(new Runnable(){
			@SuppressWarnings("deprecation")
			public void run(){
				String query = "";
				for(int i = 1; i < args.length; i++) query += " " + args[i];
				if(query.length() > 0) query = query.substring(1);
				switch(args[0].toLowerCase()){
					case "google": google(e, query); break;
					case "konachan": konachan(e, query); break;
					case "hentai": hentai(e, query); break;
					case "e621": e621(e, query); break;
					default: break;
				}
				Thread.currentThread().stop();
			}
		});
		t.start();
	}
	
	private void google(UserChatEvent e, String query){	
		int resultNum = 1;
		if(query.contains("{result=")){
			resultNum = Utils.stringToInt(query.split("\\{result=")[1].split("}")[0]);
			query = query.replaceFirst("\\{result=" + resultNum + "}", "");
		}
		
		if(resultNum > 64){
			Utils.infoBypass(e.getGroup(), "Result #" + resultNum + " is out of range!");
			return;
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
        
        JsonObject result = results.get(0).getAsJsonObject();
        Utils.infoBypass(e.getGroup(), "Showing result #**" + resultNum + "** out of **" + 
                                 Utils.fixString(responseData.getAsJsonObject("cursor").get("estimatedResultCount").toString()) + "** results in "
        		                 + "**" + Utils.fixString(responseData.getAsJsonObject("cursor").get("searchResultTime").toString()) + "** seconds.\n\n" +
        	                     "**" + Utils.fixString(result.get("titleNoFormatting").toString()) + "** \n" + Utils.fixString(result.get("url").toString()) + "\n\n" +
        	                     Utils.fixString(result.get("content").toString()));
        return;
	}
	
	private void konachan(UserChatEvent e, String query){
		String domain = "com";
		if(query.contains("{domain=")){
			domain = query.split("\\{domain=")[1].split("}")[0];
			query = query.replaceFirst("\\{domain=" + domain + "}", "");
		}
		if(!domain.equalsIgnoreCase("com") && !domain.equalsIgnoreCase("net")){
			Utils.info(e.getGroup(), "Invalid domain! Use com or net!");
			return;
		}
		String[] page = Utils.getHTMLCode("https://konachan." + domain +  "/post");
		int maxId = Integer.parseInt(Utils.getNextLineCodeFromLink(page, 1, "Post.register_tags").get(0).split("register\\(\\{\"id\"")[1].split(",\"tags\"")[0].substring(1));
		checkRandomPost(e, maxId, domain, query, 0);
	}
	
	private void checkRandomPost(UserChatEvent e, int maxId, String domain, String query, int hops){ //fix the post errors
		if(hops >= 50){
			Utils.infoBypass(e.getGroup(), "Could not find matching image in 50 tries!");
			return;
		}

		int id = (int) (new Random().nextDouble() * (maxId - 1) + 1);
		String[] imagePage = Utils.getHTMLCode("https://konachan." + domain + "/post/show/" + id + "/");
		query = Utils.removeStartSpaces(query);
		if(hasTags(imagePage, query)){
			ArrayList<String> highres = Utils.getNextLineCodeFromLink(imagePage, 0, "Click on the <a class=\"highres-show\" href=\"");
			if(highres.size() > 0)
				Utils.infoBypass(e.getGroup(), highres.get(0).split("href=\"")[1].split("\">View larger version")[0]);
			else{
				ArrayList<String> line = Utils.getNextLineCodeFromLink(imagePage, 0, "property=\"og\\:image\"");
				if(line.size() > 0)
					Utils.infoBypass(e.getGroup(), line.get(0).split("meta content=\"")[1].split("\" property=\"")[0]);
				else{
					ArrayList<String> highresUncensored = Utils.getNextLineCodeFromLink(imagePage, 0, "unchanged highres\" href=\"");
					if(highresUncensored.size() > 0)
						Utils.infoBypass(e.getGroup(), highresUncensored.get(0).split("href=\"")[1].split("\" id=")[0]);
					else{
						ArrayList<String> uncensored = Utils.getNextLineCodeFromLink(imagePage, 0, "unchanged\" href=\"");
						if(uncensored.size() == 0){
							checkRandomPost(e, maxId, domain, query, hops + 1);
							return;
						}
						String image = uncensored.get(0).split("href=\"")[1].split("\" id=")[0];
						Utils.infoBypass(e.getGroup(), image);
					}
				}
			}
			return;
		}else checkRandomPost(e, maxId, domain, query, hops + 1);
	}
	
	private boolean hasTags(String[] html, String query){
		if(query.replaceAll(" ", "").length() == 0) return true;
		ArrayList<String> line = Utils.getNextLineCodeFromLink(html, 0, "property=\"og:description\" />");
		if(line.size() == 0) return false;
		String tags = line.get(0);
		for(String tag : query.split(" "))
			if(!tags.contains(tag)) return false;
		return true;
	}
	
	private void hentai(UserChatEvent e, String query){	
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
		Utils.infoBypass(e.getGroup(), gallery);
	}
	
	private String findHentai(String url, String params){
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
	
	private String[] getHentaiTypes(){
		return new String[]{"doujinshi", "manga", "artistcg", "gamecg", "western", "non-h", "imageset", "cosplay", "asianporn", "misc"};
	}
	
	private void e621(UserChatEvent e, String query){ //to improve?
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

	    Utils.infoBypass(e.getGroup(), "http://e621.net" + line.split("href=\"")[1].split("\" onclick=")[0]);
	}
	
}
