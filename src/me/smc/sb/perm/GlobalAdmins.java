package me.smc.sb.perm;

import net.dv8tion.jda.entities.User;

public enum GlobalAdmins{

	SMC("91302128328392704", false, "Smc"),
	AUTO("91184384442384384", false, "-_Auto_-"),
	ST("77631618088435712", true, "Sexual_Tentacle");
	
	String id, osuUser;
	boolean onlyIRC;
	
	GlobalAdmins(String id, boolean onlyIRC, String osuUser){
		this.id = id;
		this.onlyIRC = onlyIRC;
		this.osuUser = osuUser;
	}
	
	public String getId(){
		return id;
	}
	
	public String getOsuUser(){
		return osuUser;
	}
	
	public boolean isOnlyIRCAdmin(){
		return onlyIRC;
	}
	
	public static boolean isAdmin(User user){
		for(GlobalAdmins admin : GlobalAdmins.values())
			if(admin.getId().equalsIgnoreCase(user.getId()) && !admin.isOnlyIRCAdmin())
				return true;
		return false;
	}
	
	public static boolean isIRCAdmin(User user){
		for(GlobalAdmins admin : GlobalAdmins.values())
			if(admin.getId().equalsIgnoreCase(user.getId()))
				return true;
		return false;
	}
	
	public static boolean isAdmin(String user){
		for(GlobalAdmins admin : GlobalAdmins.values())
			if(admin.getOsuUser().equalsIgnoreCase(user) && !admin.isOnlyIRCAdmin())
				return true;
		return false;
	}
	
	public static boolean isIRCAdmin(String user){
		for(GlobalAdmins admin : GlobalAdmins.values())
			if(admin.getOsuUser().equalsIgnoreCase(user))
				return true;
		return false;
	}
	
}
