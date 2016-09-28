package me.smc.sb.multi;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;

import me.smc.sb.irccommands.InvitePlayerCommand;
import me.smc.sb.utils.Utils;

public class TeamGame extends Game{

	private ArrayList<Player> fTeamPlayers;
	private ArrayList<Player> sTeamPlayers;
	
	public TeamGame(Match match){
		super(match);
	}

	@Override
	public void initialInvite(){
		LinkedList<Player> fullList = new LinkedList<Player>(match.getFirstTeam().getPlayers());
		
		fullList.addAll(match.getSecondTeam().getPlayers());
		
		for(Player pl : fullList)
			pl.setSlot(-1);
		
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
						if(teamToBoolean(findTeam(playersInRoom.get(0)))){
							fTeamPoints++;
						}else sTeamPoints++;
					
					stop();
				}
			}
		}, 600000);
	}
	
	@Override
	public void allowTeamInvites(){
		if(InvitePlayerCommand.allowedInviters.containsKey(match.getFirstTeam())) return;
		
		messageUpdater("Use !invite <player name> to invite your teammates or invite them through osu!.",
				       "Both captains, use !random to settle which team goes first. If you need help, use !alert <message>");
	
		InvitePlayerCommand.allowedInviters.put(match.getFirstTeam(), this);
		InvitePlayerCommand.allowedInviters.put(match.getSecondTeam(), this);
	}

	@Override
	protected void playerLeft(String message){
		String player = message.replace(" left the game.", "").replaceAll(" ", "_");
		joinQueue.remove(player);
		playersInRoom.remove(player);
		
		for(Player pl : findTeam(player).getPlayers())
			if(pl.eq(player)) pl.setSlot(-1);
		
		if(!hijackedSlots.isEmpty()){
			int rSlot = -1;
			for(int slot : hijackedSlots.keySet())
				if(hijackedSlots.get(slot).equalsIgnoreCase(player)){
					rSlot = slot;
					break;
				}
			
			if(rSlot != -1) hijackedSlots.remove(rSlot);
		}
		
		if(state.eq(GameState.PLAYING) && rematchesLeft > 0){
			rematchesLeft--;
			
			state = GameState.WAITING;
			mapSelected = true;
			
			sendMessage("!mp abort");
			sendMessage("!mp aborttimer");
			
			sendMessage("Someone has disconnected, there will be a rematch!");
			
			updateTwitch("There was a disconnection, the match will be replayed!");
			
			banchoFeedback.clear();
			switchPlaying(false, true);
			
			prepareReadyCheck();
			return;
		}
	}

	private void scheduleNextCaptainInvite(){
		Timer captainFallback = new Timer();
		captainFallback.schedule(new TimerTask(){
			public void run(){
				if(match == null) return;
				int fTeamCaptains = 0, sTeamCaptains = 0;
				
				for(String player : playersInRoom){
					Team team = findTeam(player);	
					if(teamToBoolean(team)) fTeamCaptains++;
					else sTeamCaptains++;
				}
				
				Team missingTeam = null;
				
				if(fTeamCaptains == 0 && sTeamCaptains == 0){
					addNextCaptain(match.getFirstTeam());
					addNextCaptain(match.getSecondTeam());
					scheduleNextCaptainInvite();
					return;
				}else if(fTeamCaptains == 0) missingTeam = match.getFirstTeam();
				else if(sTeamCaptains == 0) missingTeam = match.getSecondTeam();
				else return;
				
				addNextCaptain(missingTeam);
				scheduleNextCaptainInvite();
			}
		}, 60000);
	}
	
	private void addNextCaptain(Team team){
		for(Player pl : team.getPlayers())
			if(!captains.contains(pl.getName().replaceAll(" ", "_")) && !playersInRoom.contains(pl.getName().replaceAll(" ", "_"))){
				captains.add(pl.getName().replaceAll(" ", "_"));
				invitePlayer(pl.getName().replaceAll(" ", "_"));
				return;
			}

		for(Player pl : team.getPlayers())
			captains.remove(pl.getName().replaceAll(" ", "_"));
	}
	
	public void resize(String message){
		if(message == null && !state.eq(GameState.RESIZING)){
			state = GameState.RESIZING;
			
			fTeamPlayers = new ArrayList<>();
			sTeamPlayers = new ArrayList<>();
			
			Timer t = new Timer();
			t.schedule(new TimerTask(){
				public void run(){
					if(match.getPlayers() > roomSize){
						sendMessage("!mp size " + match.getPlayers());
						roomSize = match.getPlayers();
						
						ArrayList<Integer> freeSlots = new ArrayList<>();
						ArrayList<Player> toMove = new ArrayList<>();
						
						for(int i = match.getPlayers() / 2 + 1; i <= match.getPlayers(); i++)
							freeSlots.add(i);

						for(Player p : sTeamPlayers)
							if(p.getSlot() > match.getPlayers() / 2)
								freeSlots.remove((Integer) p.getSlot());
							else toMove.add(p);
						
						for(Player p : new ArrayList<>(toMove)){
							int slot = freeSlots.stream().findFirst().orElse(-1);
							
							if(slot != -1){
								freeSlots.remove((Integer) slot);
								sendMessage("!mp move " + p.getName().replaceAll(" ", "_") + " " + slot);
								toMove.remove(p);
							}else sendMessage("!mp kick " + p.getName().replaceAll(" ", "_"));
						}				
					}else{
						ArrayList<Integer> freeSlots = new ArrayList<>();
						ArrayList<Player> toMove = new ArrayList<>();
						
						for(int i = 1; i <= match.getPlayers() / 2; i++)
							freeSlots.add(i);

						for(Player p : fTeamPlayers)
							if(p.getSlot() <= match.getPlayers() / 2)
								freeSlots.remove((Integer) p.getSlot());
							else toMove.add(p);
						
						for(Player p : new ArrayList<>(toMove)){
							int slot = freeSlots.stream().findFirst().orElse(-1);
							
							if(slot != -1){
								freeSlots.remove((Integer) slot);
								sendMessage("!mp move " + p.getName().replaceAll(" ", "_") + " " + slot);
								toMove.remove(p);
							}else sendMessage("!mp kick " + p.getName().replaceAll(" ", "_"));
						}				
						
						freeSlots.clear();
						toMove.clear();
						
						for(int i = match.getPlayers() / 2 + 1; i <= match.getPlayers(); i++)
							freeSlots.add(i);

						for(Player p : sTeamPlayers)
							if(p.getSlot() > match.getPlayers() / 2 && p.getSlot() <= match.getPlayers())
								freeSlots.remove((Integer) p.getSlot());
							else toMove.add(p);
						
						for(Player p : new ArrayList<>(toMove)){
							int slot = freeSlots.stream().findFirst().orElse(-1);
							
							if(slot != -1){
								freeSlots.remove((Integer) slot);
								sendMessage("!mp move " + p.getName().replaceAll(" ", "_") + " " + slot);
								toMove.remove(p);
							}else sendMessage("!mp kick " + p.getName().replaceAll(" ", "_"));
						}
						
						sendMessage("!mp size " + match.getPlayers());
						roomSize = match.getPlayers();
					}
					
					state = GameState.WAITING;
				}
			}, 2500);
		}else{
			int slot = Utils.stringToInt(message.split(" ")[1]);
			message = Utils.removeExcessiveSpaces(message);
			String[] spaceSplit = message.split(" ");
			
			int count = 0;
			for(String arg : spaceSplit){
				if(arg.contains("osu.ppy.sh")) break;
				count++;
			}
			
			String player = "";

			for(int i = count + 1; i < spaceSplit.length; i++){
				if(spaceSplit[i].equalsIgnoreCase("[Team")){
					count = i;
					break;
				}
				if(spaceSplit[i].equalsIgnoreCase("[Host")){
					count = i + 2;
					break;
				}
				
				player += spaceSplit[i] + "_";
			}
			
			player = player.substring(0, player.length() - 1);
			
			Team team = findTeam(player);
			
			if(team == null){
				sendMessage("!mp kick " + player);
				sendMessage(player + " is not on a team!");
				return;
			}
			
			Player p = findPlayer(player);

			if(p != null) p.setSlot(slot);
			
			if(teamToBoolean(team)){
				if(fTeamPlayers.size() >= match.getPlayers() / 2){
					sendMessage("!mp kick " + player);
					return;
				}
				
				fTeamPlayers.add(p);
			}else{
				if(sTeamPlayers.size() >= match.getPlayers() / 2)
					sendMessage("!mp kick " + player);
				
				sTeamPlayers.add(p);
			}
		}
	}
}
