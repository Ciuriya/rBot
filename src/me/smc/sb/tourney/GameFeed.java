package me.smc.sb.tourney;

import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;
import java.util.function.Consumer;

import me.smc.sb.main.Main;
import me.smc.sb.tourney.Player;
import me.smc.sb.utils.Utils;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageChannel;

public class GameFeed{

	private Game game;
	private Message discordFeed;
	private MessageChannel resultDiscord;
	
	public GameFeed(Game game){
		this.game = game;
		
		postDiscordFeed();
	}
	
	public void postDiscordFeed(){
		String discord = game.match.getTournament().get("resultDiscord");
		
		String gameMessage = buildFeed() + "```";
		
		if(resultDiscord != null){
			if(Main.api.getTextChannelById(discord) != null){
				resultDiscord = Main.api.getTextChannelById(discord);
			}else resultDiscord = Main.api.getPrivateChannelById(discord);
			
			if(resultDiscord != null){
				Utils.fakeInfo(resultDiscord, gameMessage);
				
				resultDiscord.sendMessage(gameMessage).queue(new Consumer<Message>(){
					@Override
					public void accept(Message t){
						discordFeed = t;
					}
				});
			}
		}
	}
	
	public void updateDiscord(){
		new Thread(new Runnable(){
			public void run(){
				String localMessage = buildFeed();
				
				if(resultDiscord != null && discordFeed != null){
					/*if(bansWithNames.size() > 0){
						localMessage += (game.finished ? "\n" : "") + "\nBans\n";
						
						for(String banned : bansWithNames)
							localMessage += banned + "\n";
					}*/
					
					discordFeed.editMessage(localMessage + "```").queue(new Consumer<Message>(){
						@Override
						public void accept(Message t){
							discordFeed = t;
						}
					});
				}
			}
		}).start();
	}
	
	public void updateTwitch(String message){
		new Thread(new Runnable(){
			public void run(){
				if(connectToStream(game)){
					String channel = game.match.getTournament().get("twitchChannel");
					
					Timer t = new Timer();
					t.schedule(new TimerTask(){
						public void run(){
							if(!Main.twitchBot.getUserBot().getChannels().stream().anyMatch(c -> c.getName().equals("#" + channel)))
								Main.twitchBot.sendIRC().joinChannel("#" + channel);
						}
					}, (long) (30000.0 / 20.0 + 500));
					
					Main.twitchRegulator.sendMessage(channel, message);
				}
			}
		}).start();
	}
	
	private boolean connectToStream(Game game){
		boolean streamed = game.match.getTournament().getTwitchHandler().startStreaming(game);
		
		if(streamed)
			Main.twitchRegulator.sendMessage(game.match.getTournament().get("twitchChannel"),
											 "Game switched to " + game.match.getLobbyName());
		
		return streamed;
	}
	
	public String buildFeed(){
		String firstTeamName = game.firstTeam.getTeam().getTeamName();
		String secondTeamName = game.secondTeam.getTeam().getTeamName();
		String message = "**Match #" + game.match.getMatchNum() + " in " + game.match.getTournament().get("name") + 
						 "**\n" + game.mpLink + " ```\n" + firstTeamName + " - " + secondTeamName + "\n";

		for(int i = 0; i < firstTeamName.length() - 1; i++)
			message += " ";
				
		message += game.firstTeam.getPoints() + " | " + game.secondTeam.getPoints() + "\n";
		
		for(int i = 0; i < firstTeamName.length() - 3; i++)
			message += " ";
		
		message += "Best of " + game.match.getBestOf() + "\n\n";
		
		message += "Status: " + (game.finished ? "ended" : getMatchStatus());
		
		List<Player> currentPlayers = game.getCurrentPlayers();
		
		if(!game.finished){
			message += "\n\nLobby\n";
			
			java.util.Map<Integer, Player> players = new HashMap<>();
			
			if(!currentPlayers.isEmpty())
				for(Player pl : currentPlayers) players.put(pl.getSlot(), pl);
			
			for(int i = 1; i <= game.match.getMatchSize(); i++)
				if(!players.containsKey(i))
					players.put(i, null);
			
			java.util.Map<Integer, Player> orderedMap = new TreeMap<>(players);
			
			for(Player pl : orderedMap.values())
				message += (pl == null ? "----" : pl.getName()) + "\n";
		}
		
		return message;
	}
	
	public String getMatchStatus(){
		return game.finished ? "ended" : "ongoing";
	}
}
