package me.smc.sb.multi;

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
		
		messageUpdater("Use !invite <player name> to invite your teammates or simply invite them through osu!.",
				       "Both captains, please use !random to settle which team goes first.");
	
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
	}

	private void scheduleNextCaptainInvite(){
		Timer captainFallback = new Timer();
		captainFallback.schedule(new TimerTask(){
			public void run(){
				if(match == null) return;
				int fTeamCaptains = 0, sTeamCaptains = 0;
				
				for(String player : playersInRoom)
					if(captains.contains(player.replaceAll(" ", "_"))){
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
	
	public void resize(){
		sendMessage("!mp size " + match.getPlayers());
		//do some shit here I guess?
	}
}
