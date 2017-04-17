package me.smc.sb.tracking;

public class PPInfo{

	private double pp;
	private double ppForFC;
	
	public PPInfo(double pp, double ppForFC){
		this.pp = pp;
		this.ppForFC = ppForFC;
	}
	
	public double getPP(){
		return pp;
	}
	
	public double getPPForFC(){
		return ppForFC;
	}
	
}
