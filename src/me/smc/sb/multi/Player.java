package me.smc.sb.multi;

public class Player{

	private String name;
	private int slot;
	private boolean hasMod;
	
	public Player(String name){
		this.name = name;
		this.slot = -1;
		this.hasMod = false;
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
	
	public void setSlot(int slot){
		this.slot = slot;
	}
	
	public void setHasMod(boolean hasMod){
		this.hasMod = hasMod;
	}
	
}
