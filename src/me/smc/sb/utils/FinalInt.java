package me.smc.sb.utils;

public class FinalInt {
	
	int val;
	
	public FinalInt(int val){ 
		set(val); 
	}
	
	public int get(){ 
		return val; 
	}
	
	public void set(int val){ 
		this.val = val; 
	}
	
	public void add(int val){ 
		this.val += val; 
	}
	
	public void sub(int val){ 
		this.val -= val; 
	}
}
