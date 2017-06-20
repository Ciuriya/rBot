package me.smc.sb.discordcommands;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.json.JSONObject;

import me.smc.sb.utils.Utils;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

public class SearchCommand extends GlobalCommand{

	public SearchCommand(){
		super(null, 
			  " - Lets you search many different sites (including nsfw sites)", 
			  "{prefix}search\nThis command lets you search the internet.\n\n" +
			  "----------\nUsage\n----------\n{prefix}search list - Returns all possible search sites\n" +
			  "{prefix}search konachan (search tags) - Finds a random konachan picture\n" +
			  "{prefix}search hentai (search tags) - Finds a random e-hentai gallery\n" +
			  "{prefix}search hentai (search tags) ({type={e-hentai type w/o spaces}}) - Finds a random e-hentai gallery using types\n" + 
			  "{prefix}search catgirl (search tags) - Finds a random catgirl picture\n\n" +
			  "----------\nAliases\n----------\n{prefix}lookup",  
			  true, 
			  "search", "lookup");
	}

	@Override
	public void onCommand(MessageReceivedEvent e, String[] args){
		Utils.deleteMessage(e.getChannel(), e.getMessage());
		if(!Utils.checkArguments(e, args, 1)) return;
		
		Thread t = new Thread(new Runnable(){
			@SuppressWarnings("deprecation")
			public void run(){
				String query = "";
				
				for(int i = 1; i < args.length; i++) query += " " + args[i];
				
				if(query.length() > 0) query = query.substring(1);
				
				switch(args[0].toLowerCase()){
					case "list": list(e); break;
					case "konachan": konachan(e, query); break;
					case "hentai": hentai(e, query); break;
					case "e621": e621(e, query); break;
					case "catgirl": catgirl(e, query); break;
					case "danbooru": danbooru(e, query); break;
					default: break;
				}
				
				Thread.currentThread().stop();
			}
		});
		
		t.start();
	}
	
	private void list(MessageReceivedEvent e){
		EmbedBuilder builder = new EmbedBuilder();
		
		builder.setTitle("Searchable Websites");
		
		builder.addField("SFW", "[konachan](https://konachan.net) (using {domain=net})", false);
		
		builder.addField("NSFW", "[konachan](https://konachan.com)\n" +
								 "[hentai](http://e-hentai.org)\n" +
								 "[danbooru](http://danbooru.donmai.us)\n" +
								 "[catgirl](http://catgirls.brussell98.tk)", false);
		
		Utils.info(e.getChannel(), builder.build());
	}
	
	private void konachan(MessageReceivedEvent e, String query){
		String domain = "com";
		
		if(query.contains("{domain=")){
			domain = query.split("\\{domain=")[1].split("}")[0];
			query = query.replaceFirst("\\{domain=" + domain + "}", "");
		}
		
		if(!domain.equalsIgnoreCase("com") && !domain.equalsIgnoreCase("net")){
			Utils.info(e.getChannel(), "Invalid domain! Use com or net!");
			return;
		}
		
		checkRandomPost(e, domain, query, 0);
	}
	
	private void checkRandomPost(MessageReceivedEvent e, String domain, String query, int hops){
		if(hops > 50){
			Utils.info(e.getChannel(), "Could not find matching image within 50 tries.");
			return;
		}
		
		String[] imagePage = Utils.getHTMLCode("http://konachan." + domain + "/post/random");

		query = Utils.removeStartSpaces(query);
		if(hasTags(imagePage, query)){
			if(exists(imagePage)){
				ArrayList<String> highres = Utils.getNextLineCodeFromLink(imagePage, 0, "Click on the <a class=\"highres-show\" href=\"");
				
				if(highres.size() > 0)
					Utils.infoBypass(e.getChannel(), "https://" + highres.get(0).split("href=\"\\/\\/")[1].split("\">View larger version")[0]);
				else{
					ArrayList<String> line = Utils.getNextLineCodeFromLink(imagePage, 0, "property=\"og\\:image\"");
					
					if(line.size() > 0)
						Utils.infoBypass(e.getChannel(), "https://" + line.get(0).split("meta content=\"\\/\\/")[1].split("\" property=\"")[0]);
					else{
						ArrayList<String> highresUncensored = Utils.getNextLineCodeFromLink(imagePage, 0, "unchanged highres\" href=\"");
						
						if(highresUncensored.size() > 0)
							Utils.infoBypass(e.getChannel(), "https://" + highresUncensored.get(0).split("href=\"\\/\\/")[1].split("\" id=")[0]);
						else{
							ArrayList<String> uncensored = Utils.getNextLineCodeFromLink(imagePage, 0, "unchanged\" href=\"");
							
							if(uncensored.size() == 0){
								checkRandomPost(e, domain, query, hops + 1);
								return;
							}
							
							String image = uncensored.get(0).split("href=\"\\/\\/")[1].split("\" id=")[0];
							
							Utils.infoBypass(e.getChannel(), "https://" + image);
						}
					}
				}
				return;
			}else checkRandomPost(e, domain, query, hops + 1);
		}else checkRandomPost(e, domain, query, hops + 1);
	}
	
	private boolean hasTags(String[] html, String query){
		if(query.replaceAll(" ", "").length() == 0) return true;
		
		ArrayList<String> line = Utils.getNextLineCodeFromLink(html, 0, "property=\"og:description\" />");
		if(line.size() == 0) return false;
		
		String ftags = line.get(0).split("content=\"")[1].split("\"")[0];
		
		for(String tag : query.split(" ")){
			String[] tags = ftags.split(" ");
			
			for(String t : tags){
				if(t.equalsIgnoreCase(tag)) return true;
			}
			
			return false;
		}
		
		return true;
	}
	
	private boolean exists(String[] html){
		ArrayList<String> line = Utils.getNextLineCodeFromLink(html, 0, "This post does not exist.");
		
		return line.isEmpty();
	}
	
	private void hentai(MessageReceivedEvent e, String query){	
		String url = "http://e-hentai.org/";
		String params = "";
		String[] types = null;
		
		if(query.contains("{type=")){
			types = query.split("\\{type=")[1].split("}")[0].split(",");
			query = query.replaceFirst("\\{type=" + query.split("\\{type=")[1].split("}")[0] + "}", "");
		}
		
		for(String str : getHentaiTypes()){
			int on = 0;
			
			if(types != null) 
				for(String s : types)
					if(s.toLowerCase().replaceAll(" ", "").equalsIgnoreCase(str))
						on = 1;
			
			params += "&f_" + str + "=" + on;
		}
		
		params += "&f_search=" + query.replaceAll(" ", "+") + "&f_apply=Apply+Filter";
		
		String gallery = findHentai(url, params);
		
		Utils.infoBypass(e.getChannel(), gallery);
		Utils.infoBypass(e.getChannel(), gallery.replace("e-", "ex"));
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
	
	private void e621(MessageReceivedEvent e, String query){ //to improve?
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

	    Utils.infoBypass(e.getChannel(), "http://e621.net" + line.split("href=\"")[1].split("\" onclick=")[0]);
	}
	
	private void catgirl(MessageReceivedEvent e, String query){
		String BASEURL = "http://catgirls.brussell98.tk/api/random";
		String NSFWURL = "http://catgirls.brussell98.tk/api/nsfw/random";
		boolean nsfw = false;
		
		if (query.contains("nsfw")) nsfw = true;
		String[] post = Utils.getHTMLCode(nsfw ? NSFWURL : BASEURL);
		JSONObject obj = new JSONObject(post[0]);
		
		if (!obj.has("url")) {
			Utils.info(e.getChannel(), "Unable to find image");
		} else {
			Utils.info(e.getChannel(), obj.getString("url"));
		}
	}
	
	private void danbooru(MessageReceivedEvent e, String query){
		String url = "http://danbooru.donmai.us/posts/random";
		
		if(query.split(" ").length > 0) url += "?tags=" + query.replaceAll(" ", "%20");
		
		String[] post = Utils.getHTMLCode(url);
		List<String> imageLine = Utils.getNextLineCodeFromLink(post, 0, "<meta property=\"og:image\" content=\"");
		
		if(imageLine.size() > 0) 
			Utils.info(e.getChannel(), imageLine.get(0).split("content=\"")[1].split("\"")[0]);
		else Utils.info(e.getChannel(), "Could not find anything!");
		
	}
}
