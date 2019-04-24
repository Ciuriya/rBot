package me.smc.sb.tracking;

public class PPInfo{

	private double pp;
	private double ppForFC;
	private double aimPP;
	private double speedPP;
	private double accPP;
	
	public PPInfo(double pp, double ppForFC, double aimPP, double speedPP, double accPP){
		this.pp = pp;
		this.ppForFC = ppForFC;
		this.aimPP = aimPP;
		this.speedPP = speedPP;
		this.accPP = accPP;
	}
	
	public double getPP(){
		return pp;
	}
	
	public double getPPForFC(){
		return ppForFC;
	}
	
	public double getAimPP(){
		return aimPP;
	}

	public double getSpeedPP(){
		return speedPP;
	}
	
	public double getAccPP(){
		return accPP;
	}
	
	public void setPP(double pp) {
		this.pp = pp;
	}
}
