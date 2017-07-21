package me.smc.sb.tourney;

import me.smc.sb.utils.Utils;

public class Player{

	private String name;
	private String userID;
	private int slot;
	private boolean hasMod;
	private double modMultiplier;
	private boolean isPlaying;
	private boolean verified;
	private boolean submitted;
	private int rank;
	
	public Player(String name){
		this.name = name;
		this.userID = "";
		this.slot = -1;
		this.hasMod = false;
		this.modMultiplier = 1;
		this.isPlaying = false;
		this.verified = false;
		this.submitted = false;
		this.rank = -1;
	}
	
	public String getName(){
		return name;
	}
	
	public String getUserID(){
		return userID;
	}
	
	public String getIRCTag(){
		if(userID.length() == 0 || userID.equalsIgnoreCase("-1")) userID = Utils.getOsuPlayerId(name, true);
		
		return userID.length() > 0 && !userID.equalsIgnoreCase("-1") ? "#" + userID : name.replaceAll(" ", "_");
	}
	
	public int getSlot(){
		return slot;
	}
	
	public boolean hasMod(){
		return hasMod;
	}
	
	public double getModMultiplier(){
		return modMultiplier;
	}
	
	public boolean isPlaying(){
		return isPlaying;
	}
	
	public boolean isVerified(){
		return verified;
	}
	
	public boolean submittedScore(){
		return submitted;
	}
	
	public int getRank(){
		return rank;
	}
	
	public void setName(String name){
		this.name = name;
	}
	
	public void setUserID(String userID){
		this.userID = userID;
	}
	
	public void setSlot(int slot){
		this.slot = slot;
	}
	
	public void setHasMod(boolean hasMod){
		this.hasMod = hasMod;
	}
	
	public void setModMultiplier(double modMultiplier){
		this.modMultiplier = modMultiplier;
	}
	
	public void setPlaying(boolean playing){
		this.isPlaying = playing;
	}
	
	public void setVerified(boolean verified){
		this.verified = verified;
	}
	
	public void setSubmitted(boolean submitted){
		this.submitted = submitted;
	}
	
	public void setRank(int rank){
		this.rank = rank;
	}
	
	public boolean eq(Player player){
		return eq(player.getName());
	}
	
	public boolean eq(String player){
		if(player.replaceAll(" ", "_").equalsIgnoreCase(name.replaceAll(" ", "_")))
			return true;
		return false;
	}
	
}
