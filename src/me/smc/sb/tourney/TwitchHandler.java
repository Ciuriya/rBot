package me.smc.sb.tourney;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;

import me.smc.sb.utils.Configuration;
import me.smc.sb.utils.Log;

public class TwitchHandler{

	private String channel;
	private List<Tournament> tournaments;
	private Game streamed;
	private List<Game> streamQueue;
	public static List<TwitchHandler> handlers = new ArrayList<>();
	
	public TwitchHandler(String channel){
		this.channel = channel;
		tournaments = new ArrayList<>();
		streamQueue = new ArrayList<>();
		
		handlers.add(this);
	}
	
	public String getChannel(){
		return channel;
	}
	
	public Game getStreamed(){
		return streamed;
	}
	
	public void add(Tournament tournament){
		if(!tournaments.contains(tournament))
			tournaments.add(tournament);
	}
	
	public void remove(Tournament tournament){
		if(tournaments.contains(tournament))
			tournaments.remove(tournament);
	}
	
	public boolean startStreaming(Game game){
		if(isStreamed(game)) return true;
		
		if(streamed != null && streamed.match != null){
			if(game.match.getStreamPriority() >= streamed.match.getStreamPriority()){
				streamQueue.add(game);
				
				return false;
			}
		}
		
		streamed = game;
		changeStreamTitle(streamed);
		
		return true;
	}
	
	public void forceStream(Game game){
		if(streamed != null)
			streamQueue.add(streamed);
		
		streamed = game;
		changeStreamTitle(streamed);
	}
	
	public void stopStreaming(Game game){
		if(!isStreamed(game)) streamQueue.remove(game);
		else{
			Game next = null;
			
			if(!streamQueue.isEmpty())
				next = streamQueue.stream().min(new Comparator<Game>(){
					@Override
					public int compare(Game o1, Game o2){
						int firstPriority = o1.match.getStreamPriority();
						int secondPriority = o2.match.getStreamPriority();
						
						if(firstPriority > secondPriority) return 1;
						else if(firstPriority == secondPriority) return 0;
						else return -1;
					}
				}).orElse(null);
			
			streamed = null;
			if(next != null) startStreaming(next);
		}
	}
	
	// fix one day? lol
	public void changeStreamTitle(Game game){
		String accessToken = new Configuration(new File("login.txt")).getValue("twitch-access");
		String title = game.match.getTournament().get("displayName") + ":+" + 
					   game.match.getFirstTeam().getTeamName() + "+vs+" + game.match.getSecondTeam().getTeamName();
		
		try{
		    ProcessBuilder pb = new ProcessBuilder(
		            "curl",
		            "-H 'Accept: application/vnd.twitchtv.v2+json'",
		            "-H 'Authorization: OAuth " + accessToken + "'",
		            "-d \"channel[status]=" + title.replaceAll(" ", "+") + "\"",
		            "-X PUT https://api.twitch.tv/kraken/channels/" + channel);
		    
		    Process p = pb.start();
			
			p.waitFor();
		}catch(Exception e){
			Log.logger.log(Level.SEVERE, e.getMessage(), e);
		}
	}
	
	public boolean isStreaming(){
		return streamed != null;
	}
	
	public boolean isStreamed(Game game){
		return streamed != null && streamed.getMpNum() == game.getMpNum();
	}
	
	public static TwitchHandler add(String channel, Tournament tournament){
		TwitchHandler handler = handlers.stream().filter(h -> h.getChannel().equalsIgnoreCase(channel)).findFirst().orElse(null);
		
		if(handler == null) handler = new TwitchHandler(channel);
		
		handler.add(tournament);
		
		return handler;
	}
	
	public static TwitchHandler get(Tournament tournament){
		return handlers.stream().filter(h -> h.getChannel().equalsIgnoreCase(tournament.get("twitchChannel"))).findFirst().orElse(null);
	}
	
	public static TwitchHandler get(String channel){
		return handlers.stream().filter(h -> h.getChannel().equalsIgnoreCase(channel)).findFirst().orElse(null);
	}
}
