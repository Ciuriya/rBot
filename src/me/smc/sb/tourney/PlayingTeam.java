package me.smc.sb.tourney;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import me.smc.sb.irccommands.RandomCommand;

public class PlayingTeam{
	
	private Game game;
	private Team team;
	private int points;
	private int rematchesLeft;
	private int roll;
	private List<Player> currentPlayers;
	private List<Map> bans;
	private LinkedList<PickedMap> mapsPicked;

	public PlayingTeam(Team team, Game game){
		this.game = game;
		this.team = team;
		this.points = 0;
		this.roll = -1;
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
	
	public int getRoll(){
		return roll;
	}
	
	public boolean canRematch(){
		return rematchesLeft != 0;
	}
	
	public int getRematchesLeft(){
		return rematchesLeft;
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
	
	public void addPick(PickedMap map){
		mapsPicked.add(map);
	}
	
	public void addBan(Map map){
		bans.add(map);
	}
	
	public void addRematch(){
		rematchesLeft++;
	}
	
	public void addPoint(){
		points++;
	}
	
	public void removePoint(){
		points--;
	}
	
	public void setPoints(int points){
		this.points = points;
	}
	
	public boolean useRematch(){
		boolean rematch = canRematch();
		
		if(rematch) rematchesLeft--;
		
		return rematch;
	}
	
	public void setRoll(int roll){
		this.roll = roll;
		RandomCommand.waitingForRolls.remove(this);
		game.checkRolls();
	}
	
	public boolean addPlayer(Player player){
		if(currentPlayers.size() < getMatch().getMatchSize() / 2 && team.has(player) &&
			!currentPlayers.contains(player)){
			return currentPlayers.add(player);
		}
		
		if(currentPlayers.contains(player)) return true;
		
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
