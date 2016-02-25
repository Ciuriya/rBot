package me.smc.sb.multi;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;

import org.json.JSONObject;

import me.smc.sb.irccommands.BanMapCommand;
import me.smc.sb.irccommands.InvitePlayerCommand;
import me.smc.sb.irccommands.JoinMatchCommand;
import me.smc.sb.irccommands.RandomCommand;
import me.smc.sb.irccommands.SelectMapCommand;
import me.smc.sb.listeners.IRCChatListener;
import me.smc.sb.main.Main;
import me.smc.sb.utils.Log;
import me.smc.sb.utils.Utils;

public class Game{ //cannot invite before captains have joined

	private Match match;
	private String multiChannel;
	private String mpLink; //somehow make it available
	private int waitingForCaptains = 2;
	private int fTeamPoints = 0, sTeamPoints = 0;
	private int warmupsLeft = 2;
	private int rollsLeft = 2;
	private int previousRoll = 0;
	private boolean mapSelected = false;
	private boolean fTeamFirst = true;
	private int disconnectsAllowed = 1;
	private List<String> invitesSent;
	private List<String> playersInRoom;
	private List<Map> bans;
	private LinkedList<String> banchoFeedback;
	private Team selectingTeam;
	private Team banningTeam;
	private Map map;
	private Map previousMap;
	private Timer mapUpdater;
	private Player captain1, captain2, captain3, captain4;
	
	public Game(Match match){
		this.match = match;
		this.multiChannel = "";
		this.mpLink = "";
		this.invitesSent = new ArrayList<>();
		this.playersInRoom = new ArrayList<>();
		this.bans = new ArrayList<>();
		this.match.setGame(this);
		
		setupLobby();
	}
	
	public void start(String multiChannel, String mpLink){
		this.multiChannel = multiChannel;
		this.mpLink = mpLink;
		
		IRCChatListener.gamesListening.put(multiChannel, this);
		
		sendMessage("!mp lock");
		sendMessage("!mp size " + match.getPlayers());
		sendMessage("!mp set 2 " + (match.getTournament().isScoreV2() ? "4" : "0"));
		
		captain1 = match.getFirstTeam().getPlayers().get(0);
		captain2 = match.getSecondTeam().getPlayers().get(0);
		invitePlayer(captain1.getName().replaceAll(" ", "_"));
		invitePlayer(captain2.getName().replaceAll(" ", "_"));
		
		Timer captainFallback = new Timer();
		captainFallback.schedule(new TimerTask(){
			public void run(){
				if(waitingForCaptains > 0 && match.getPlayers() >= 4){
					if(!playersInRoom.isEmpty()){
						if(playersInRoom.get(0).equalsIgnoreCase(captain1.getName().replaceAll(" ", "_"))){
							if(match.getSecondTeam().getPlayers().size() > 1){
								captain4 = match.getSecondTeam().getPlayers().get(1);
								invitePlayer(captain4.getName().replaceAll(" ", "_"));
							}
						}else
							if(match.getFirstTeam().getPlayers().size() > 1){
								captain3 = match.getFirstTeam().getPlayers().get(1);
								invitePlayer(captain3.getName().replaceAll(" ", "_"));
							}
					}
				}
			}
		}, 300000);
		
		Timer timeout = new Timer();
		timeout.schedule(new TimerTask(){
			public void run(){
				if(waitingForCaptains > 0){
					if(!playersInRoom.isEmpty())
						if(findTeam(playersInRoom.get(0)).getTeamName().equalsIgnoreCase(match.getFirstTeam().getTeamName())){
							fTeamPoints++;
						}else sTeamPoints++;
					
					stop();
				}
			}
		}, 600000);
	}
	
	public void resize(){
		sendMessage("!mp size " + match.getPlayers());
		//do some shit here I guess?
	}
	
	private void mapSelection(int part){
		switch(part){
			case 1:
				waitingForCaptains = -1;
				sendMessage("You may now invite your teammates. Use !invite <player name> to invite them.");
				sendMessage("Both captains, please use !random to settle which team goes first.");
				
				InvitePlayerCommand.allowedInviters.put(match.getFirstTeam(), this);
				InvitePlayerCommand.allowedInviters.put(match.getSecondTeam(), this);
				
				RandomCommand.waitingForRolls.put(match.getFirstTeam().getPlayers().getFirst().getName().replaceAll(" ", "_"), this);
				RandomCommand.waitingForRolls.put(match.getSecondTeam().getPlayers().getFirst().getName().replaceAll(" ", "_"), this);
				break;
			case 2:
				selectingTeam = findNextTeamToPick();
				map = null;
				
				sendMessage(selectingTeam.getTeamName() + ", please pick a warmup map using !select <map url>.");
				SelectMapCommand.gamesInSelection.add(this);
				
				startMapUpdater();
				break;
			case 3: 
				banningTeam = findNextTeamToBan();
				
				if(bans.size() >= 4) mapSelection(4);
				else{
					BanMapCommand.banningTeams.put(banningTeam, this);
					sendMessage(banningTeam.getTeamName() + ", please ban a map using !ban <map url> or !ban <map #>" +
							   (match.getMapPool().getSheetUrl().length() > 0 ? "[" + match.getMapPool().getSheetUrl() + 
							   " You can find the maps here]" : ""));
				}
				
				break;
			case 4:
				selectingTeam = findNextTeamToPick();
				map = null;
				
				sendMessage(selectingTeam.getTeamName() + ", please pick a map using !select <map url> or !select <map #>." +
						   (match.getMapPool().getSheetUrl().length() > 0 ? "[" + match.getMapPool().getSheetUrl() + 
						   " You can find the maps here]" : ""));
				
				SelectMapCommand.gamesInSelection.add(this);
				
				startMapUpdater();
				break;
			default: break;
		}
	}
	
	private void startMapUpdater(){
		previousMap = null;
		
		mapUpdater = new Timer();
		mapUpdater.scheduleAtFixedRate(new TimerTask(){
			public void run(){
				if(map != null){
					if(previousMap != null && previousMap.getURL().equalsIgnoreCase(map.getURL())) return;
					
					previousMap = map;
					changeMap(map);
				}
			}
		}, 10000, 10000);
	}
	
	private void changeMap(Map map){
		sendMessage("!mp map " + map.getBeatmapID() + " 0");
		
		String mods = "";
		
		switch(map.getCategory()){
			case 1: mods = "Freemod"; break;
			case 2: mods = "HD"; break;
			case 3: mods = "HR"; break;
			case 4: mods = "DT"; break;
			default: mods = "None"; break;
		}
		
		sendMessage("!mp mods " + mods);
	}
	
	public void acceptRoll(String player, int roll){
		rollsLeft--;
		
		if(rollsLeft == 0){
			boolean fTeam = false;
			for(Player p : match.getFirstTeam().getPlayers())
				if(p.getName().replaceAll(" ", "_").equalsIgnoreCase(player)){
					fTeam = true;
					break;
				}
			
			if(roll == previousRoll){
				sendMessage("Rolls were equal! Please reroll using !random.");
				RandomCommand.waitingForRolls.put(match.getFirstTeam().getPlayers().getFirst().getName().replaceAll(" ", "_"), this);
				RandomCommand.waitingForRolls.put(match.getSecondTeam().getPlayers().getFirst().getName().replaceAll(" ", "_"), this);
				rollsLeft = 2;
				return;
			}
			
			if(roll > previousRoll) fTeamFirst = fTeam;
			else fTeamFirst = !fTeam;
			
			mapSelection(2);
		}else previousRoll = roll;
	}
	
	public void acceptBan(String ban){
		Map toBan = null;
		
		if(Utils.stringToInt(ban) != -1){
			int num = 1;
			for(Map m : match.getMapPool().getMaps()){
				if(num == Utils.stringToInt(ban)){
					toBan = m;
					break;
				}
				num++;
			}
		}else
			for(Map m : match.getMapPool().getMaps())
				if(m.getURL().equalsIgnoreCase(ban)){
					toBan = m;
					break;
				}
		
		if(toBan != null && !bans.contains(toBan)){
			BanMapCommand.banningTeams.remove(banningTeam);
			bans.add(toBan);
			mapSelection(3);
		}else sendMessage("Invalid ban!");
	}
	
	public void stop(){
		for(String player : invitesSent)
			JoinMatchCommand.gameInvites.remove(player);
		
		InvitePlayerCommand.allowedInviters.remove(match.getFirstTeam());
		InvitePlayerCommand.allowedInviters.remove(match.getSecondTeam());
		
		IRCChatListener.gamesListening.remove(multiChannel);
		
		SelectMapCommand.gamesInSelection.remove(this);
		
		sendMessage("!mp close");
		
		Log.logger.log(Level.INFO, "Game ended: " + match.getFirstTeam().getTeamName() + " (" + fTeamPoints + ") vs (" +
								   sTeamPoints + ")" + match.getSecondTeam().getTeamName());
		match.delete();
	}
	
	private void setupLobby(){
		try{
			Main.ircBot.sendIRC().joinChannel("BanchoBot");
		}catch(Exception ex){
			Log.logger.log(Level.INFO, "Could not talk to BanchoBot!");
		}
		
		Main.ircBot.sendIRC().message("BanchoBot", "!mp make " + match.getLobbyName());
	}
	
	public Team getSelectingTeam(){
		return selectingTeam;
	}
	
	public int getMpNum(){
		return Utils.stringToInt(multiChannel.replace("#mp_", ""));
	}
	
	public void handleSelect(String map){
		if(Utils.stringToInt(map) != -1){
			int num = 1;
			for(Map m : match.getMapPool().getMaps()){
				if(num == Utils.stringToInt(map)){
					if(warmupsLeft == 0 && bans.contains(m)) break;
					
					this.map = m;
					mapSelected = true;
					break;
				}
				num++;
			}
		}else{
			for(Map m : match.getMapPool().getMaps())
				if(m.getURL().equalsIgnoreCase(map)){
					if(warmupsLeft == 0 && bans.contains(m)) break;
					
					this.map = m;
					mapSelected = true;
					return;
				}
			
			if(warmupsLeft > 0){
				JSONObject jsMap = Map.getMapInfo(new Map(map, 1).getBeatmapID());
				int length = jsMap.getInt("total_length");
				if(length > 270) return;
				this.map = new Map(map, 1);
				
				mapSelected = true;
			}
		}
	}
	
	public void handleBanchoFeedback(String message){
		if(message.contains("All players are ready")) readyCheck();
		else if(message.contains("left the game.")) playerLeft(message);
		else if(message.contains("The match has started!")) banchoFeedback.clear();
		else if(message.contains("The match has finished!")) playEnded();
		else banchoFeedback.add(message);
	}
	
	private void readyCheck(){
		if(playersInRoom.size() == match.getPlayers() && mapSelected){ 
			mapUpdater.cancel();
			SelectMapCommand.gamesInSelection.remove(this);
			
			if(previousMap == null || !previousMap.getURL().equalsIgnoreCase(map.getURL())){
				changeMap(map);
				Utils.sleep(2500);
			}
			
			sendMessage("!mp start 5");
			mapSelected = false;
		}
		
		banchoFeedback.clear();
	}
	
	private void playerLeft(String message){
		playersInRoom.remove(message.replace(" left the game.", "").replaceAll(" ", "_"));
	}
	
	private void playEnded(){
		float fTeamScore = 0, sTeamScore = 0;
		int fTeamPlayers = 0, sTeamPlayers = 0;
		
		for(String feedback : banchoFeedback){
			if(feedback.contains("finished playing")){
				String player = feedback.substring(0, feedback.indexOf(" finished"));
				Team team = findTeam(player);
				boolean fTeam = false;
				
				if(team.getTeamName().equalsIgnoreCase(match.getFirstTeam().getTeamName()))
					fTeam = true;
				
				if(fTeam) fTeamPlayers++;
				else sTeamPlayers++;
				
				if(feedback.split(",")[1].substring(1).split(")")[0].equalsIgnoreCase("PASSED")){
					int score = Utils.stringToInt(feedback.split("Score: ")[1].split(",")[0]);
					
					if(fTeam) fTeamScore += score;
					else sTeamScore += score;
				}
			}
		}
		
		if(warmupsLeft > 0){
			if(fTeamScore == sTeamScore) sendMessage("Both teams have tied!");
			else
				sendMessage((fTeamScore > sTeamScore ? match.getFirstTeam().getTeamName() :
							match.getSecondTeam().getTeamName()) + " won by " + Math.abs(fTeamScore - sTeamScore) +
							" points!");
			warmupsLeft--;
			
			if(warmupsLeft == 0) mapSelection(3);
		}else{
			if((int) Math.abs(fTeamPlayers - sTeamPlayers) != 0){	
				boolean fTeamDC = sTeamPlayers > fTeamPlayers;
				
				if((fTeamDC && fTeamScore < sTeamScore) || (!fTeamDC && sTeamScore < fTeamScore)){
					if(disconnectsAllowed > 0){
						disconnectsAllowed--;
						
						sendMessage("Someone has disconnected, there will be a rematch!");
						mapSelected = true;
						banchoFeedback.clear();
						return;
					}	
				}
			}
			
			disconnectsAllowed = 1;
			
			boolean fTeamWon = fTeamScore > sTeamScore;
			
			sendMessage((fTeamScore > sTeamScore ? match.getFirstTeam().getTeamName() :
				match.getSecondTeam().getTeamName()) + " won by " + Math.abs(fTeamScore - sTeamScore) +
				" points!");
			
			if(fTeamWon) fTeamPoints++;
			else sTeamPoints++;
			
			sendMessage(match.getFirstTeam().getTeamName() + ": " + fTeamPoints + " | " +
						sTeamPoints + " " + match.getSecondTeam().getTeamName());
			
			if(fTeamPoints + sTeamPoints == match.getBestOf() - 1 && fTeamPoints == sTeamPoints){
				changeMap(match.getMapPool().findTiebreaker());
				
				mapSelected = true;
			}else if(fTeamPoints + sTeamPoints == match.getBestOf()){
				sendMessage(fTeamWon ? match.getFirstTeam().getTeamName() : match.getSecondTeam().getTeamName() +
							" has won this game!");
				sendMessage("The lobby is ending in 30 seconds, thanks for playing!");
				sendMessage("!mp timer");
				
				Timer t = new Timer();
				t.schedule(new TimerTask(){
					public void run(){
						stop();
					}
				}, 30000);
			}else mapSelection(4);
		}
		
		banchoFeedback.clear();
	}
	
	private void sendMessage(String command){
		Main.ircBot.sendIRC().message(multiChannel, command);
	}
	
	public void invitePlayer(String player){
		if(invitesSent.contains(player.replaceAll(" ", "_"))) return;
		
		invitesSent.add(player.replaceAll(" ", "_"));
		JoinMatchCommand.gameInvites.put(player.replaceAll(" ", "_"), this);
		
		try{
			Main.ircBot.sendIRC().joinChannel(player.replaceAll(" ", "_"));
		}catch(Exception ex){
			Log.logger.log(Level.INFO, "Could not talk to " + player + "!");
		}
		
		Main.ircBot.sendIRC().message(player.replaceAll(" ", "_"), "You have been invited to join: " + match.getLobbyName());
		Main.ircBot.sendIRC().message(player.replaceAll(" ", "_"), "Reply with !join to join the game. Please note that this command is reusable.");
	}
	
	public void acceptInvite(String player){
		if(playersInRoom.contains(player.replaceAll(" ", "_")) || playersInRoom.size() >= match.getPlayers()) return;
		if(!verifyPlayer(player.replaceAll(" ", "_"))) return;
		
		playersInRoom.add(player.replaceAll(" ", "_"));
		assignSlotAndTeam(player.replaceAll(" ", "_"));
	}
	
	private Team findTeam(String player){
		for(Player p : match.getFirstTeam().getPlayers())
			if(p.getName().replaceAll(" ", "_").equalsIgnoreCase(player.replaceAll(" ", "_")))
				return match.getFirstTeam();
		
		for(Player p : match.getSecondTeam().getPlayers())
			if(p.getName().replaceAll(" ", "_").equalsIgnoreCase(player.replaceAll(" ", "_")))
				return match.getSecondTeam();
		
		return null;
	}
	
	private Team findNextTeamToPick(){
		int totalPicked = fTeamPoints + sTeamPoints;
		
		if(warmupsLeft == 1) totalPicked = 1;
		
		boolean fTeam = true;
		
		fTeam = (totalPicked + 2) % 2 == 0 ? fTeamFirst : !fTeamFirst; 
		
		return fTeam ? match.getFirstTeam() : match.getSecondTeam();
	}
	
	private Team findNextTeamToBan(){
		int totalBanned = bans.size();
		
		boolean fTeam = true;
		
		fTeam = (totalBanned + 2) % 2 == 0 ? fTeamFirst : !fTeamFirst;
		
		return fTeam ? match.getFirstTeam() : match.getSecondTeam();
	}
	
	private boolean verifyPlayer(String player){
		return findTeam(player) != null;
	}
	
	private boolean isCaptain(String player){
		if(captain1.getName().replaceAll(" ", "_").equalsIgnoreCase(player)) return true;
		if(captain2.getName().replaceAll(" ", "_").equalsIgnoreCase(player)) return true;
		if(captain3 != null && captain3.getName().replaceAll(" ", "_").equalsIgnoreCase(player)) return true;
		if(captain4 != null && captain4.getName().replaceAll(" ", "_").equalsIgnoreCase(player)) return true;
		
		return false;
	}
	
	private void assignSlotAndTeam(String player){
		LinkedList<Player> team = new LinkedList<>();
		String color = "blue";
		Player pl = null;
		int i = 1;
		
		for(Player p : match.getFirstTeam().getPlayers())
			if(p.getName().replaceAll(" ", "_").equalsIgnoreCase(player)){
				p.setSlot(-1);
				pl = p;
				team = match.getFirstTeam().getPlayers();
				break;
			}
		
		for(Player p : match.getSecondTeam().getPlayers())
			if(p.getName().replaceAll(" ", "_").equalsIgnoreCase(player)){
				p.setSlot(-1);
				pl = p;
				team = match.getSecondTeam().getPlayers();
				color = "red";
				i = match.getPlayers() / 2 + 1;
				break;
			}
		
		List<Integer> usedSlots = new ArrayList<Integer>();
		if(!team.isEmpty()){
			for(Player p : team)
				if(playersInRoom.contains(p.getName().replaceAll(" ", "_")) && p.getSlot() != -1){
					usedSlots.add(p.getSlot());
				}
		
			for(; i < match.getPlayers(); i++)
				if(!usedSlots.contains(i)){
					sendMessage("!mp move " + player.replaceAll(" ", "_") + " " + i);
					pl.setSlot(i);
					sendMessage("!mp team " + player.replaceAll(" ", "_") + " " + color);
					
					if(isCaptain(player.replaceAll(" ", "_")))
						waitingForCaptains--;
					
					if(waitingForCaptains == 0 && playersInRoom.size() < 2)
						waitingForCaptains++;
					else if(waitingForCaptains == 0) mapSelection(1);
					
					break;
				}
		}
	}
}
