package me.Smc.sb.perm;

import me.itsghost.jdiscord.talkable.GroupUser;

public enum GlobalAdmins{

	SMC("91302128328392704"),
	AUTO("91184384442384384");
	
	String id;
	
	GlobalAdmins(String id){
		this.id = id;
	}
	
	public String getId(){
		return id;
	}
	
	public static boolean isAdmin(GroupUser user){
		for(GlobalAdmins admin : GlobalAdmins.values())
			if(admin.getId().equalsIgnoreCase(user.getUser().getId()))
				return true;
		return false;
	}
	
}
