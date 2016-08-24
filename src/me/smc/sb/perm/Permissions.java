package me.smc.sb.perm;

import net.dv8tion.jda.Permission;
import net.dv8tion.jda.entities.TextChannel;
import net.dv8tion.jda.entities.User;

public enum Permissions{

	INSTANT_INVITE(0),
	KICK_MEMBER(1),
	BAN_MEMBER(2),
	MANAGE_ROLES(3),
	MANAGE_CHANNELS(4),
	MANAGE_SERVER(5),
	READ_MESSAGES(10),
	SEND_MESSAGES(11),
	SEND_TTS_MESSAGES(12),
	MANAGE_MESSAGES(13),
	EMBED_LINKS(14),
	ATTACH_FILES(15),
	READ_MESSAGE_HISTORY(16),
	MENTION_EVERYONE(17),
	VOICE_CONNECT(20),
	VOICE_SPEAK(21),
	VOICE_MUTE_MEMBERS(22),
	VOICE_DEAFEN_MEMBERS(23),
	VOICE_MOVE_MEMBERS(24),
	VOICE_USE_VAD(25),
	BOT_ADMIN(1000),
	IRC_BOT_ADMIN(1001);
	
	int offset;
	
	Permissions(int offset){
		this.offset = offset;
	}
	
	public int getOffset(){
		return offset;
	}
	
	public static boolean check(User user, Permissions perm){
		if(perm == null) return true;
		if(GlobalAdmins.isAdmin(user)) return true;
		if(perm.equals(IRC_BOT_ADMIN) && GlobalAdmins.isIRCAdmin(user)) return true;
		if(perm.equals(BOT_ADMIN) && GlobalAdmins.isAdmin(user)) return true;
		return false;
	}
	
	public static boolean hasPerm(User user, TextChannel channel, Permissions perm){
		if(check(user, perm)) return true;
		else if(!(perm.equals(BOT_ADMIN) || perm.equals(IRC_BOT_ADMIN)))
			return channel.checkPermission(user, Permission.getFromOffset(perm.getOffset()));
		else return false;
	}
	
	public static boolean hasPerm(String user, Permissions perm){
		if(user == null) return true;
		if(perm == null) return true;
		if(GlobalAdmins.isAdmin(user)) return true;
		if(perm.equals(IRC_BOT_ADMIN) && GlobalAdmins.isIRCAdmin(user)) return true;
		if(perm.equals(BOT_ADMIN)) return false;
		return true;
	}
	
}
