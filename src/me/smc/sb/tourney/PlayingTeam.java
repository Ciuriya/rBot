package me.smc.sb.tourney;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class PlayingTeam{
	
	private Game game;
	private Team team;
	private int points;
	private int rematchesLeft;
	private List<Player> currentPlayers;
	private List<Map> bans;
	private LinkedList<PickedMap> mapsPicked;

	public PlayingTeam(Team team, Game game){
		this.game = game;
		this.team = team;
		this.rematchesLeft = game.match.getTournament().getInt("rematchesAllowed");
		this.currentPlayers = new ArrayList<>();
		this.bans = new ArrayList<>();
		this.mapsPicked = new LinkedList<>();
	}
	
	public Game getGame(){
		return game;
	}
	
	public Match getMatch(){
		return game.match;
	}
	
	public Team getTeam(){
		return team;
	}
	
	public int getPoints(){
		return points;
	}
	
	public boolean canRematch(){
		return rematchesLeft != 0;
	}
	
	public List<Player> getCurrentPlayers(){
		return currentPlayers;
	}
	
	public List<Map> getBans(){
		return bans;
	}
	
	public LinkedList<PickedMap> getPicks(){
		return mapsPicked;
	}
	
	public boolean useRematch(){
		boolean rematch = canRematch();
		
		if(rematch) rematchesLeft--;
		
		return rematch;
	}
	
	public boolean addPlayer(Player player){
		if(currentPlayers.size() < getMatch().getMatchSize() / 2 && team.has(player) &&
			!currentPlayers.contains(player)){
			return currentPlayers.add(player);
		}
		
		return false;
	}
	
	public boolean removePlayer(Player player){
		if(currentPlayers.contains(player))
			return currentPlayers.remove(player);
		
		return false;
	}
	
	// index starts at 0
	public void inviteTeam(int index, long delay){
		if(index > team.getPlayers().size() - 1) inviteTeam(0, delay);
		else{
			game.banchoHandle.sendMessage("!mp invite " + team.getPlayers().get(index).getIRCTag(), false);
			
			new Timer().schedule(new TimerTask(){
				public void run(){
					if(currentPlayers.size() == 0)
						inviteTeam(index + 1, delay);
				}
			}, delay);
		}
	}
}
