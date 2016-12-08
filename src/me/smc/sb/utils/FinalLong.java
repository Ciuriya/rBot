package me.smc.sb.utils;

public class FinalLong {

	long val;
	
	public FinalLong(long val){ 
		set(val); 
	}
	
	public long get(){ 
		return val; 
	}
	
	public void set(long val){ 
		this.val = val; 
	}
	
	public void add(long val){ 
		this.val += val; 
	}
	
	public void sub(long val){ 
		this.val -= val; 
	}
}
