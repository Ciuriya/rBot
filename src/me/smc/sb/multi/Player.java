package me.smc.sb.multi;

public class Player{

	private String name;
	private int slot;
	private boolean hasMod;
	private double modMultiplier;
	private boolean isPlaying;
	private boolean verified;
	
	public Player(String name){
		this.name = name;
		this.slot = -1;
		this.hasMod = false;
		this.modMultiplier = 1;
		this.isPlaying = false;
		this.verified = false;
	}
	
	public String getName(){
		return name;
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
	
	public boolean eq(Player player){
		return eq(player.getName());
	}
	
	public boolean eq(String player){
		if(player.replaceAll(" ", "_").equalsIgnoreCase(name.replaceAll(" ", "_")))
			return true;
		return false;
	}
	
}
