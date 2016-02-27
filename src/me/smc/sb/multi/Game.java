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

public class Game{

	private Match match;
	private String multiChannel;
	private String mpLink; //somehow make it available
	private int waitingForCaptains = 2;
	private int fTeamPoints = 0, sTeamPoints = 0;
	private int warmupsLeft = 2;
	private int rollsLeft = 2;
	private int previousRoll = 0;
	private int playersChecked = 0;
	private boolean mapSelected = false;
	private boolean fTeamFirst = true;
	private boolean verifyingMods = false;
	private boolean waitingForConfirm = false;
	private int rematchesAllowed = 1;
	private List<String> invitesSent;
	private List<String> playersInRoom;
	private List<Map> bans;
	private List<Map> mapsPicked;
	private LinkedList<String> banchoFeedback;
	private Team selectingTeam;
	private Team banningTeam;
	private Map map;
	private Map previousMap;
	private Timer mapUpdater;
	private List<String> captains;
	
	public Game(Match match){
		this.match = match;
		this.multiChannel = "";
		this.mpLink = "";
		this.invitesSent = new ArrayList<>();
		this.playersInRoom = new ArrayList<>();
		this.banchoFeedback = new LinkedList<>();
		this.bans = new ArrayList<>();
		this.mapsPicked = new ArrayList<>();
		this.captains = new ArrayList<>();
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
		
		captains.add(match.getFirstTeam().getPlayers().get(0).getName().replaceAll(" ", "_"));
		captains.add(match.getSecondTeam().getPlayers().get(0).getName().replaceAll(" ", "_"));
		
		for(String player : captains)
			invitePlayer(player);
		
		scheduleNextCaptainInvite();
		
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
	
	private void scheduleNextCaptainInvite(){
		Timer captainFallback = new Timer();
		captainFallback.schedule(new TimerTask(){
			public void run(){
				if(waitingForCaptains > 0){
					if(!playersInRoom.isEmpty()){
						int fTeamCaptains = 0, sTeamCaptains = 0;
						
						for(String player : playersInRoom)
							if(captains.contains(player.replaceAll(" ", "_"))){
								Team team = findTeam(player);	
								if(team.getTeamName().equalsIgnoreCase(match.getFirstTeam().getTeamName())) fTeamCaptains++;
								else sTeamCaptains++;
							}	
						
						Team missingTeam = null;
						
						if(fTeamCaptains == 0 && sTeamCaptains == 0){
							addNextCaptain(match.getFirstTeam());
							addNextCaptain(match.getSecondTeam());
							scheduleNextCaptainInvite();
						}else if(fTeamCaptains == 0) missingTeam = match.getFirstTeam();
						else if(sTeamCaptains == 0) missingTeam = match.getSecondTeam();
						else return;
						
						addNextCaptain(missingTeam);
						scheduleNextCaptainInvite();
					}
				}
			}
		}, 120000);
	}
	
	private void addNextCaptain(Team team){
		for(Player pl : team.getPlayers())
			if(!captains.contains(pl.getName().replaceAll(" ", "_"))){
				captains.add(pl.getName().replaceAll(" ", "_"));
				invitePlayer(pl.getName().replaceAll(" ", "_"));
				return;
			}
	}
	
	public void resize(){
		sendMessage("!mp size " + match.getPlayers());
		//do some shit here I guess?
	}
	
	private void mapSelection(int part){
		switch(part){
			case 1:
				waitingForCaptains = -1;
				sendMessage("Use !invite <player name> to invite your teammates.");
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
		ban = Utils.takeOffExtrasInBeatmapURL(ban);
		
		Map toBan = null;
		
		if(Utils.stringToInt(ban) != -1){
			int num = 1;
			for(Map m : match.getMapPool().getMaps()){
				if(num == Utils.stringToInt(ban)){
					if(m.getCategory() == 5){sendMessage("You cannot ban the tiebreaker!"); return;}
					toBan = m;
					break;
				}
				num++;
			}
		}else
			for(Map m : match.getMapPool().getMaps())
				if(m.getURL().equalsIgnoreCase(ban)){
					if(m.getCategory() == 5){sendMessage("You cannot select the tiebreaker!"); return;}
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
		waitingForCaptains = 0;
		
		for(String player : invitesSent)
			JoinMatchCommand.gameInvites.remove(player);
		
		InvitePlayerCommand.allowedInviters.remove(match.getFirstTeam());
		InvitePlayerCommand.allowedInviters.remove(match.getSecondTeam());
		
		IRCChatListener.gamesListening.remove(multiChannel);
		
		SelectMapCommand.gamesInSelection.remove(this);
		
		sendMessage("!mp close");
		
		Log.logger.log(Level.INFO, "Game ended: " + match.getFirstTeam().getTeamName() + " (" + fTeamPoints + ") vs (" +
								   sTeamPoints + ") " + match.getSecondTeam().getTeamName());
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
					if(warmupsLeft == 0 && bans.contains(m)){sendMessage("This map is banned! Please choose something else."); return;}
					else if(m.getCategory() == 5){sendMessage("You cannot select the tiebreaker!"); return;}
					else if(warmupsLeft == 0 && !checkMap(m)){sendMessage("This map was already picked once! Please choose something else."); return;}
					
					this.map = m;
					mapSelected = true;
					return;
				}
				num++;
			}
		}else{
			for(Map m : match.getMapPool().getMaps())
				if(m.getURL().equalsIgnoreCase(map)){
					if(warmupsLeft == 0 && bans.contains(m)){sendMessage("This map is banned! Please choose something else."); return;}
					else if(m.getCategory() == 5){sendMessage("You cannot select the tiebreaker!"); return;}
					else if(warmupsLeft == 0 && !checkMap(m)){sendMessage("This map was already picked once! Please choose something else."); return;}
					
					this.map = m;
					mapSelected = true;
					return;
				}
			
			if(warmupsLeft > 0){
				JSONObject jsMap = Map.getMapInfo(new Map(map, 1).getBeatmapID());
				int length = jsMap.getInt("total_length");
				if(length > 270){
					sendMessage("The warmup selected is too long! The maximum length is 4m30s.");
					return;
				}
				this.map = new Map(map, 1);
				
				mapSelected = true;
				return;
			}
		}
		
		sendMessage("Invalid map!");
	}
	
	private boolean checkMap(Map map){
		return !mapsPicked.contains(map);
	}
	
	public void handleBanchoFeedback(String message){
		if(message.contains("All players are ready")) readyCheck();
		else if(message.contains("left the game.")) playerLeft(message);
		else if(message.contains("The match has started!")) banchoFeedback.clear();
		else if(message.contains("The match has finished!")) playEnded();
		else if(message.startsWith("Slot ")) modVerification(message);
		else banchoFeedback.add(message);
	}
	
	private void readyCheck(){
		if(waitingForConfirm) return;
		
		if(playersInRoom.size() == match.getPlayers() && mapSelected){ 
			if(previousMap == null || !previousMap.getURL().equalsIgnoreCase(map.getURL())){
				changeMap(map);
				Utils.sleep(2500);
			}
			
			if(mapUpdater != null) mapUpdater.cancel();
			SelectMapCommand.gamesInSelection.remove(this);
			
			modVerification(null);
		}
		
		banchoFeedback.clear();
	}
	
	private void startMatch(int timer){
		if(verifyingMods) return;
		
		mapsPicked.add(map);
		
		if(timer != 0) sendMessage("!mp start " + timer);
		else sendMessage("!mp start");
		
		mapSelected = false;
	}
	
	private void modVerification(String message){
		if(message == null && map.getCategory() == 1 && warmupsLeft <= 0){
			verifyingMods = true;
			playersChecked = 0;
			
			LinkedList<Player> tempList = new LinkedList<Player>(match.getFirstTeam().getPlayers());
			tempList.addAll(match.getSecondTeam().getPlayers());
			
			for(Player pl : tempList) pl.setHasMod(false);
			
			sendMessage("!mp settings");
		}else if(map.getCategory() == 1 && warmupsLeft <= 0){
			if(message.endsWith("]")){
				String[] sBracketSplit = message.split("\\[");
				
				String mods = sBracketSplit[sBracketSplit.length - 1].split("\\]")[0];
				
				boolean validMod = true;
				
				if(mods.split(" ").length <= 2){
					playersChecked++; 
					finalModCheck();
					return;
				}
				
				loop: for(String mod : mods.split(", ")){
					if(mod.contains("/") && mod.contains("Team")) mod = mod.split("\\/")[1].substring(1);
					switch(mod.toLowerCase()){
						case "hidden": case "hardrock": case "flashlight": break;
						default: validMod = false; break loop;
					}
				}
				
				if(!validMod){
					verifyingMods = false;
					
					sendMessage("You can only use HD, HR or FL! Make sure you do not have any other mods!");
					sendMessage("!mp map " + map.getBeatmapID() + " 0");
					return;
				}
				
				message = Utils.removeExcessiveSpaces(message).replace(" [" + mods + "]", "");
				
				String[] spaceSplit = message.split(" ");
				
				int count = 0;
				for(String arg : spaceSplit){
					if(arg.contains("osu.ppy.sh")) break;
					count++;
				}
				
				String player = "";
				
				for(int i = count + 1; i < spaceSplit.length; i++){
					if(spaceSplit[i].equalsIgnoreCase("[Team")) break;
					player += spaceSplit[i] + "_";
				}
				
				player = player.substring(0, player.length() - 1);
				
				Team team = findTeam(player);
				for(Player pl : team.getPlayers())
					if(pl.getName().replaceAll(" ", "_").equalsIgnoreCase(player.replaceAll(" ", "_"))){
						pl.setHasMod(true);
						playersChecked++;
						break;
					}
				
				finalModCheck();
			}else{
				playersChecked++;
				finalModCheck();
			}
		}else startMatch(5);
	}
	
	private void finalModCheck(){
		if(playersChecked >= match.getPlayers()){
			verifyingMods = false;
			playersChecked = 0;
			int fTeamModCount = 0, sTeamModCount = 0;
			
			for(Player pl : match.getFirstTeam().getPlayers())
				if(pl.hasMod()) fTeamModCount++;
			
			for(Player pl : match.getSecondTeam().getPlayers())
				if(pl.hasMod()) sTeamModCount++;
			
			int countNeeded = (int) Math.ceil((double) match.getPlayers() / 4.0);
			
			if(fTeamModCount < countNeeded || sTeamModCount < countNeeded){
				sendMessage("You need to have at least " + countNeeded + " mod users per team!");
				sendMessage("!mp map " + map.getBeatmapID() + " 0");
			}else startMatch(0);
		}
	}
	
	private void playerLeft(String message){
		playersInRoom.remove(message.replace(" left the game.", "").replaceAll(" ", "_"));
	}
	
	private void playEnded(){
		waitingForConfirm = true;
		
		Timer t = new Timer();
		t.schedule(new TimerTask(){
			public void run(){
				waitingForConfirm = false;
				
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
						
						if(feedback.split(",")[1].substring(1).split("\\)")[0].equalsIgnoreCase("PASSED")){
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
					else mapSelection(2);
				}else{
					if((int) Math.abs(fTeamPlayers - sTeamPlayers) != 0){	
						boolean fTeamDC = sTeamPlayers > fTeamPlayers;
						
						if((fTeamDC && fTeamScore < sTeamScore) || (!fTeamDC && sTeamScore < fTeamScore)){
							if(rematchesAllowed > 0){
								rematchesAllowed--;
								
								sendMessage("Someone has disconnected, there will be a rematch!");
								mapSelected = true;
								banchoFeedback.clear();
								return;
							}	
						}
					}
					
					if(fTeamScore == sTeamScore){
						sendMessage("Both teams have tied, there will be a rematch!");
						mapSelected = true;
						banchoFeedback.clear();
						rematchesAllowed--;
						return;
					}else rematchesAllowed = 1;
					
					boolean fTeamWon = fTeamScore > sTeamScore;
					
					sendMessage((fTeamScore > sTeamScore ? match.getFirstTeam().getTeamName() :
						match.getSecondTeam().getTeamName()) + " won by " + Math.abs(fTeamScore - sTeamScore) +
						" points!");
					
					if(fTeamWon) fTeamPoints++;
					else sTeamPoints++;
					
					sendMessage(match.getFirstTeam().getTeamName() + " " + fTeamPoints + " | " +
								sTeamPoints + " " + match.getSecondTeam().getTeamName() +
								"      Best of " + match.getBestOf());
					
					if(fTeamPoints + sTeamPoints == match.getBestOf() - 1 && fTeamPoints == sTeamPoints){
						changeMap(match.getMapPool().findTiebreaker());
						
						mapSelected = true;
					}else if(fTeamPoints > Math.floor(match.getBestOf() / 2) || sTeamPoints > Math.floor(match.getBestOf() / 2)){
						sendMessage((fTeamWon ? match.getFirstTeam().getTeamName() : match.getSecondTeam().getTeamName()) +
									" has won this game!");
						sendMessage("The lobby is ending in 30 seconds, thanks for playing!");
						sendMessage("!mp timer");
						
						Timer time = new Timer();
						time.schedule(new TimerTask(){
							public void run(){
								stop();
							}
						}, 30000);
					}else mapSelection(4);
				}
				
				banchoFeedback.clear();
			}
		}, 10000);
	}
	
	private void sendMessage(String command){
		Main.ircBot.sendIRC().message(multiChannel, command);
	}
	
	public void invitePlayer(String player){
		if(invitesSent.contains(player.replaceAll(" ", "_"))) return;

		if(player.equalsIgnoreCase("Smc")){
			Timer t = new Timer();
			t.schedule(new TimerTask(){
				public void run(){
					acceptInvite(player);
				}
			}, 5000);
			return;
		}
		
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
		Team playerTeam = null;
		
		for(String pl : captains)
			if(player.replaceAll(" ", "_").equalsIgnoreCase(pl)){
				playerTeam = findTeam(pl);
				break;
			}
		
		if(playerTeam != null){
			for(String pl : playersInRoom)
				if(findTeam(pl).getTeamName().equalsIgnoreCase(playerTeam.getTeamName()) 
					&& !pl.equalsIgnoreCase(player.replaceAll(" ", "_")))
					return false;
			
			return true;
		}
		
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
			
			for(; i < match.getPlayers() + 1; i++)
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
