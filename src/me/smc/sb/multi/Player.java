package me.smc.sb.multi;

public class Player{

	private String name;
	private int slot;
	
	public Player(String name){
		this.name = name;
		this.slot = -1;
	}
	
	public String getName(){
		return name;
	}
	
	public int getSlot(){
		return slot;
	}
	
	public void setSlot(int slot){
		this.slot = slot;
	}
	
}
