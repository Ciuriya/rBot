package me.smc.sb.multi;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;

import me.smc.sb.irccommands.InvitePlayerCommand;

public class TeamGame extends Game{

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
		
		boolean team = teamToBoolean(findTeam(player));
		
		if(state.eq(GameState.PLAYING) && (team ? rematchesLeftFTeam : rematchesLeftSTeam) > 0 && warmupsLeft == 0){
			if(team) rematchesLeftFTeam--;
			else rematchesLeftSTeam--;
			
			lastRematchFTeam = team;
			
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
			
			ArrayList<Player> fTeamPlayers = new ArrayList<>();
			ArrayList<Player> sTeamPlayers = new ArrayList<>();
			
			for(String playerName : playersInRoom){
				Player player = findPlayer(playerName);
				
				if(teamToBoolean(findTeam(playerName)))
					fTeamPlayers.add(player);
				else sTeamPlayers.add(player);
			}
			
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

						for(Player p : sTeamPlayers){
							if(p.getSlot() > match.getPlayers() / 2)
								freeSlots.remove((Integer) p.getSlot());
							else toMove.add(p);
						}
						
						for(Player p : new ArrayList<>(toMove)){
							int slot = freeSlots.stream().findFirst().orElse(-1);
							
							if(slot != -1){
								freeSlots.remove((Integer) slot);
								sendMessage("!mp move " + p.getIRCTag() + " " + slot);
								p.setSlot(slot);
								toMove.remove(p);
							}else kickPlayer(p.getIRCTag());
						}				
					}else{
						ArrayList<Integer> freeSlots = new ArrayList<>();
						ArrayList<Player> toMove = new ArrayList<>();
						
						for(int i = 1; i <= match.getPlayers() / 2; i++)
							freeSlots.add(i);

						for(Player p : fTeamPlayers){
							if(p.getSlot() <= match.getPlayers() / 2)
								freeSlots.remove((Integer) p.getSlot());
							else toMove.add(p);
						}
						
						for(Player p : new ArrayList<>(toMove)){
							int slot = freeSlots.stream().findFirst().orElse(-1);
							
							if(slot != -1){
								freeSlots.remove((Integer) slot);
								sendMessage("!mp move " + p.getIRCTag() + " " + slot);
								p.setSlot(slot);
								toMove.remove(p);
							}else kickPlayer(p.getIRCTag());
						}				
						
						freeSlots.clear();
						toMove.clear();
						
						for(int i = match.getPlayers() / 2 + 1; i <= match.getPlayers(); i++)
							freeSlots.add(i);

						for(Player p : sTeamPlayers){
							if(p.getSlot() > match.getPlayers() / 2 && p.getSlot() <= match.getPlayers())
								freeSlots.remove((Integer) p.getSlot());
							else toMove.add(p);
						}
						
						for(Player p : new ArrayList<>(toMove)){
							int slot = freeSlots.stream().findFirst().orElse(-1);
							
							if(slot != -1){
								freeSlots.remove((Integer) slot);
								sendMessage("!mp move " + p.getIRCTag() + " " + slot);
								p.setSlot(slot);
								toMove.remove(p);
							}else kickPlayer(p.getIRCTag());
						}
						
						sendMessage("!mp size " + match.getPlayers());
						roomSize = match.getPlayers();
					}
					
					state = GameState.WAITING;
				}
			}, 2500);
		}
	}
	
	private void kickPlayer(String player){
		sendMessage("!mp kick " + player);
		joinQueue.remove(player);
		playersInRoom.remove(player);
	}
}
