package me.smc.sb.perm;

import me.itsghost.jdiscord.talkable.GroupUser;

public enum GlobalAdmins{

	SMC("91302128328392704", false),
	AUTO("91184384442384384", false),
	ST("77631618088435712", true);
	
	String id;
	boolean onlyIRC;
	
	GlobalAdmins(String id, boolean onlyIRC){
		this.id = id;
	}
	
	public String getId(){
		return id;
	}
	
	public boolean isOnlyIRCAdmin(){
		return onlyIRC;
	}
	
	public static boolean isAdmin(GroupUser user){
		for(GlobalAdmins admin : GlobalAdmins.values())
			if(admin.getId().equalsIgnoreCase(user.getUser().getId()) && !admin.isOnlyIRCAdmin())
				return true;
		return false;
	}
	
	public static boolean isIRCAdmin(GroupUser user){
		for(GlobalAdmins admin : GlobalAdmins.values())
			if(admin.getId().equalsIgnoreCase(user.getUser().getId()))
				return true;
		return false;
	}
	
}
