package me.smc.sb.multi;

import java.util.Timer;
import java.util.TimerTask;

import me.smc.sb.utils.FinalInt;

public class SoloGame extends Game{

	public SoloGame(Match match){
		super(match);
	}

	@Override
	public void initialInvite(){
		Player fPlayer = match.getFirstTeam().getPlayers().get(0);
		Player sPlayer = match.getSecondTeam().getPlayers().get(0);
		captains.add(fPlayer.getName().replaceAll(" ", "_"));
		captains.add(sPlayer.getName().replaceAll(" ", "_"));
		
		fPlayer.setSlot(-1);
		sPlayer.setSlot(-1);
		
		for(String player : captains)
			invitePlayer(player);
	}

	@Override
	public void allowTeamInvites(){
		messageUpdater("Both players, use !random to settle who goes first. If you need help, use !alert <message>");
	}

	@Override
	protected void playerLeft(String message){ //If player leaves for more than a certain time, game lost for him
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
			
			sendMessage("Someone has disconnected, there will be a rematch once the player returns!");
			
			updateTwitch("There was a disconnection, the match will be replayed!");
			
			banchoFeedback.clear();
			switchPlaying(false, true);
		}
		
		if(rollsLeft == 0){
			final FinalInt timeLeft = new FinalInt(150000);
			
			Timer t = new Timer();
			t.scheduleAtFixedRate(new TimerTask(){
				public void run(){
					if(timeLeft.get() <= 0){
						if(playersInRoom.size() == match.getPlayers()){
							if(mapSelected) prepareReadyCheck();
							t.cancel();
							return;
						}
						
						timeLeft.sub(10000);
						
						if(timeLeft.get() <= 0){
							String winningTeam = (fTeamPoints > sTeamPoints ? match.getFirstTeam().getTeamName() : match.getSecondTeam().getTeamName());
							
							sendMessage(player + " has been disqualified!");
							sendMessage("The lobby is ending in 30 seconds, thanks for playing!");
							sendMessage("!mp timer");
							
							if(mapUpdater != null) mapUpdater.cancel();
							
							updateTwitch(winningTeam + " has won this game! " + mpLink, 20);
							
							Timer twitchCloseDelay = new Timer();
							twitchCloseDelay.schedule(new TimerTask(){
								public void run(){
									match.getTournament().stopStreaming(SoloGame.this);
								}
							}, 25500);
							
							Timer time = new Timer();
							time.schedule(new TimerTask(){
								public void run(){
									stop();
									t.cancel();
								}
							}, 30000);
						}
					}
				}
			}, 0, 10000);
		}
	}
	
}
