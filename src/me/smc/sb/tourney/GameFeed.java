package me.smc.sb.tourney;

import java.util.ArrayList;
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
	private List<String> bans;
	
	public GameFeed(Game game){
		this.game = game;
		this.bans = new ArrayList<>();
		
		postDiscordFeed();
	}
	
	public void postDiscordFeed(){
		String discord = game.match.getTournament().get("resultDiscord");
		
		String gameMessage = buildFeed() + "```";
		
		if(discord != null){
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
					if(bans.size() > 0){
						localMessage += (game.state.eq(GameState.ENDED) ? "\n" : "") + "\nBans\n";
						
						for(String banned : bans)
							localMessage += banned + "\n";
					}
					
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
		updateTwitch(message, 0);
	}
	
	public void updateTwitch(String message, int delay){
		new Timer().schedule(new TimerTask(){
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
		}, delay * 1000 + 1);
	}
	
	private boolean connectToStream(Game game){
		boolean isStreamed = game.match.getTournament().getTwitchHandler().isStreamed(game);
		boolean streamed = game.match.getTournament().getTwitchHandler().startStreaming(game);
		
		if(streamed && !isStreamed)
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
		
		message += "Status: " + (game.state.eq(GameState.ENDED) ? "ended" : getMatchStatus());
		
		List<Player> currentPlayers = game.lobbyManager.getCurrentPlayers();
		
		if(!game.state.eq(GameState.ENDED)){
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
	
	public void addBan(String ban){
		bans.add(ban);
	}
	
	public String getMatchStatus(){
		int totalWarmups = game.match.getTournament().getInt("warmupCount") * 2;
		int totalBans = game.match.getTournament().getInt("banCount") * 2;
		
		if(game.state.eq(GameState.ENDED)) return "ended";
		else if(game.firstTeam.getRoll() == -1 || game.secondTeam.getRoll() == -1) return "pre-warmup";
		else if(game.selectionManager.warmupsLeft > 0) return "warm-up (" + (totalWarmups - game.selectionManager.warmupsLeft + 1) + "/" + totalWarmups + ")";
		else if(game.selectionManager.bansLeft > 0) return "bans (" + (totalBans - game.selectionManager.bansLeft + 1) + "/" + totalBans + ")";
		else return "ongoing";
	}
}
