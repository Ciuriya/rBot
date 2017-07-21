package me.smc.sb.tourney;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import me.smc.sb.irccommands.RandomCommand;
import me.smc.sb.utils.FinalInt;
import me.smc.sb.utils.Utils;

public class LobbyManager{

	private Game game;
	private List<Player> playersRankChecked;
	
	public LobbyManager(Game game){
		this.game = game;
		this.playersRankChecked = new ArrayList<>();
	}
	
	public void join(String message){
		String sPlayer = message.split(" joined in")[0].replaceAll(" ", "_");
		
		if(!verify(sPlayer)){
			game.banchoHandle.kickPlayer(sPlayer, sPlayer + " tried to join, but they are not on either team! " +
												  "Contact a tournament organizer if you believe this to be an error.");
			
			return;
		}
		
		Player player = findPlayer(sPlayer);
		int slot = Utils.stringToInt(message.split("joined in slot ")[1].split(" for team")[0]);
		String color = message.split(" for team ")[1].split("\\.")[0];
		
		if(player.getUserID().equals("-1")){
			String userID = Utils.getOsuPlayerId(player.getName(), true);
			
			if(!userID.equals("-1")) player.setUserID(userID);
		}
		
		player.setSlot(slot);
		
		PlayingTeam team = findTeam(player);

		if(!team.addPlayer(player)){
			game.banchoHandle.kickPlayer(player.getIRCTag(), "");
			
			return;
		}
		
		boolean isOnFirstTeam = isOnFirstTeam(player);
		int count = isOnFirstTeam ? 1 : (game.match.getMatchSize() / 2 + 1);
		int upperBound = isOnFirstTeam ? (game.match.getMatchSize() / 2 + 1) : (game.match.getMatchSize() + 1);
		
		if(player.getSlot() < upperBound && player.getSlot() >= count){ // if player is already in a good slot
			joinFinalChecks(player, color, isOnFirstTeam);
			
			return;
		}
		
		player.setSlot(-1);
		
		List<Integer> usedSlots = new ArrayList<>();
		
		for(Player p : team.getCurrentPlayers())
			if(p.getSlot() != -1) usedSlots.add(p.getSlot());
		
		// find an empty slot and settle there
		for(; count < upperBound; count++){
			if(!usedSlots.contains(count)){
				player.setSlot(count);
				game.banchoHandle.sendMessage("!mp move " + player.getIRCTag() + " " + count, false);
				joinFinalChecks(player, color, isOnFirstTeam);
				
				return;
			}
		}
		
		game.banchoHandle.kickPlayer(player.getIRCTag(), ""); // invalid slot joined
		leave(player.getName());
	}
	
	private void joinFinalChecks(Player player, String color, boolean fTeam){
		String expectedColor = fTeam ? "blue" : "red";
		
		if(!color.equalsIgnoreCase(expectedColor))
			game.banchoHandle.sendMessage("!mp team " + player.getIRCTag() + " " + expectedColor, false);
		
		int lower = game.match.getTournament().getInt("lowerRankBound");
		int upper = game.match.getTournament().getInt("upperRankBound");
		
		if(lower > upper){
			int temp = upper;
			
			upper = lower;
			lower = temp;
		}
		
		int rank = -1;
		
		if(lower != upper && lower >= 1 && upper >= 1 && !playersRankChecked.contains(player)){			
			rank = Utils.getOsuPlayerRank(player.getName(), game.match.getTournament().getInt("mode"), true);
			
			if(rank != -1 && (rank < lower || rank > upper)){
				game.banchoHandle.kickPlayer(player.getIRCTag(), player.getName() + "'s rank is out of range. His rank is " + Utils.veryLongNumberDisplay(rank) + 
						   										 " while the range is " + Utils.veryLongNumberDisplay(lower) + " to " + 
						   										 Utils.veryLongNumberDisplay(upper) + "!");
				leave(player.getName());
				game.banchoHandle.sendMessage(player.getName(), "You were kicked because your rank is out of range. You are #" + Utils.veryLongNumberDisplay(rank) +
																  " while the range is " + Utils.veryLongNumberDisplay(lower) + " to " + 
																  Utils.veryLongNumberDisplay(upper) + "!", false);
				player.setSlot(-1);
				
				return;
			}
			
			playersRankChecked.add(player);
		}else if(game.match.getTournament().getBool("usingMapStats"))
			rank = Utils.getOsuPlayerRank(player.getName(), game.match.getTournament().getInt("mode"), true);
		
		if(rank > 0) player.setRank(rank);
		
		if(game.firstTeam.getCurrentPlayers().size() > 0 && game.secondTeam.getCurrentPlayers().size() > 0 && game.state.eq(GameState.WAITING)){
			setupRolling();
		}
	}
	
	public void setupRolling(){
		game.state = GameState.ROLLING;
		boolean teamTourney = game.match.getTournament().getInt("type") == 0;
		
		if(teamTourney) 		
			game.messageUpdater("Invite your teammates through osu! and use !random to settle which team goes first. ",
						   	   (game.match.getTournament().get("alertDiscord").length() > 0 ? "If you need help, use !alert <message>" : ""));
		else game.messageUpdater("Both players, use !random to settle who goes first. " + 
						   		(game.match.getTournament().get("alertDiscord").length() > 0 ? "If you need help, use !alert <message>" : ""));
		
		RandomCommand.waitingForRolls.add(game.firstTeam);
		RandomCommand.waitingForRolls.add(game.secondTeam);
	}
	
	public void leave(String sPlayer){
		Player player = findPlayer(sPlayer);
		PlayingTeam team = findTeam(player);
		
		team.removePlayer(player);
		player.setSlot(-1);
		
		if(game.state.eq(GameState.PLAYING)){
			if((System.currentTimeMillis() - game.readyManager.startTime <= game.match.getTournament().getInt("rematchCutoff") * 1000 &&
				team.canRematch()) || (game.match.getTournament().getInt("type") == 1 && team.canRematch())){
				game.banchoHandle.sendMessage("Someone has disconnected, there will be a rematch!", false);
				game.feed.updateTwitch("There was a disconnection, the match will be replayed!");
				game.resultManager.rematch(team);
				
				return;
			}
		}else if(game.state.eq(GameState.PRESTART)){
			game.banchoHandle.sendMessage("!mp aborttimer", true);
			game.readyManager.startReadyWait();
			
			return;
		}
		
		if(game.match.getTournament().getInt("type") == 1 && game.match.getTournament().getBool("usingDQs") &&
			!game.state.eq(GameState.ROLLING) && !game.state.eq(GameState.WAITING)){ // solo, dq check
			final FinalInt timeLeft = new FinalInt(game.match.getTournament().getInt("dqTime") * 1000); // 2.5mins dq time for now?
			
			Timer t = new Timer();
			t.scheduleAtFixedRate(new TimerTask(){
				public void run(){
					timeLeft.sub(10000);
					
					int players = getCurrentPlayers().size();
					
					if(players == game.match.getMatchSize()){
						t.cancel();
						
						return;
					}
					
					if(timeLeft.get() <= 0){
						if(players == 0){
							game.banchoHandle.sendMessage("No one has shown up! Closing lobby...", false);
						}else{
							PlayingTeam winningTeam = findTeam(getCurrentPlayers().get(0));
							PlayingTeam losingTeam = game.getOppositeTeam(winningTeam);
							
							winningTeam.addPoint();
							
							game.banchoHandle.sendMessage(losingTeam.getTeam().getTeamName() + " has been disqualified!", false);
							game.banchoHandle.sendMessage("The lobby is ending in 30 seconds, thanks for playing!", false);
							game.banchoHandle.sendMessage("!mp timer", false);
							
							game.feed.updateTwitch(winningTeam.getTeam().getTeamName() + " has won this game! " + game.mpLink, 20);
						}
						
						game.selectionManager.clearPickTimer();
						
						Timer twitchCloseDelay = new Timer();
						twitchCloseDelay.schedule(new TimerTask(){
							public void run(){
								game.match.getTournament().getTwitchHandler().stopStreaming(game);
							}
						}, 25500);
						
						Timer time = new Timer();
						time.schedule(new TimerTask(){
							public void run(){
								game.stop();
							}
						}, 30000);
						
						t.cancel();
					}
				}
			}, 10000, 10000);
		}
	}
	
	// to note that the match size is changed in advance, roomSize is used since it has the old value
	public void resize(){
		if(game.match.getMatchSize() > game.roomSize){ // resizing to a bigger room
			game.banchoHandle.sendMessage("!mp size " + game.match.getMatchSize(), false);
			game.roomSize = game.match.getMatchSize();
			
			ArrayList<Integer> freeSlots = new ArrayList<>();
			ArrayList<Player> toMove = new ArrayList<>();
			
			for(int i = game.match.getMatchSize() / 2 + 1; i <= game.match.getMatchSize(); i++)
				freeSlots.add(i);

			for(Player p : game.secondTeam.getCurrentPlayers()){
				if(p.getSlot() > game.match.getMatchSize() / 2)
					freeSlots.remove((Integer) p.getSlot());
				else toMove.add(p);
			}
			
			for(Player p : new ArrayList<>(toMove)){
				int slot = freeSlots.stream().findFirst().orElse(-1);
				
				if(slot != -1){
					freeSlots.remove((Integer) slot);
					game.banchoHandle.sendMessage("!mp move " + p.getIRCTag() + " " + slot, false);
					p.setSlot(slot);
					toMove.remove(p);
				}else game.banchoHandle.kickPlayer(p.getIRCTag(), "");
			}				
		}else{ // resizing to a smaller room
			ArrayList<Integer> freeSlots = new ArrayList<>();
			ArrayList<Player> toMove = new ArrayList<>();
			
			for(int i = 1; i <= game.match.getMatchSize() / 2; i++)
				freeSlots.add(i);

			for(Player p : game.firstTeam.getCurrentPlayers()){
				if(p.getSlot() <= game.match.getMatchSize() / 2)
					freeSlots.remove((Integer) p.getSlot());
				else toMove.add(p);
			}
			
			for(Player p : new ArrayList<>(toMove)){
				int slot = freeSlots.stream().findFirst().orElse(-1);
				
				if(slot != -1){
					freeSlots.remove((Integer) slot);
					game.banchoHandle.sendMessage("!mp move " + p.getIRCTag() + " " + slot, false);
					p.setSlot(slot);
					toMove.remove(p);
				}else game.banchoHandle.kickPlayer(p.getIRCTag(), "");
			}				
			
			freeSlots.clear();
			toMove.clear();
			
			for(int i = game.match.getMatchSize() / 2 + 1; i <= game.match.getMatchSize(); i++)
				freeSlots.add(i);

			for(Player p : game.secondTeam.getCurrentPlayers()){
				if(p.getSlot() > game.match.getMatchSize() / 2 && p.getSlot() <= game.match.getMatchSize())
					freeSlots.remove((Integer) p.getSlot());
				else toMove.add(p);
			}
			
			for(Player p : new ArrayList<>(toMove)){
				int slot = freeSlots.stream().findFirst().orElse(-1);
				
				if(slot != -1){
					freeSlots.remove((Integer) slot);
					game.banchoHandle.sendMessage("!mp move " + p.getIRCTag() + " " + slot, false);
					p.setSlot(slot);
					toMove.remove(p);
				}else game.banchoHandle.kickPlayer(p.getIRCTag(), "");
			}
			
			game.banchoHandle.sendMessage("!mp size " + game.match.getMatchSize(), false);
			game.roomSize = game.match.getMatchSize();
		}
	}
	
	public List<Player> getCurrentPlayers(){
		List<Player> current = new ArrayList<>(game.firstTeam.getCurrentPlayers());
		
		current.addAll(game.secondTeam.getCurrentPlayers());
		
		return current;
	}
	
	public boolean verify(String playerName){
		return game.firstTeam.getTeam().has(playerName.replaceAll(" ", "_")) ||
			   game.secondTeam.getTeam().has(playerName.replaceAll(" ", "_"));
	}
	
	public boolean isOnFirstTeam(Player player){
		return game.firstTeam.getTeam().has(player);
	}
	
	public PlayingTeam findTeam(Player player){
		if(game.firstTeam.getTeam().has(player)) return game.firstTeam;
		if(game.secondTeam.getTeam().has(player)) return game.secondTeam;
		
		return null;
	}
	
	public Player findPlayer(String playerName){
		List<Player> fullList = new ArrayList<>(game.firstTeam.getTeam().getPlayers());
		
		fullList.addAll(game.secondTeam.getTeam().getPlayers());
		
		return fullList.stream().filter(p -> p.getName().replaceAll(" ", "_").equalsIgnoreCase(playerName.replaceAll(" ", "_"))).findFirst().orElse(null);
	}
}
