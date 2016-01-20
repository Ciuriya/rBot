package me.smc.sb.perm;

import me.itsghost.jdiscord.talkable.GroupUser;

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
	BOT_ADMIN(30);
	
	int offset;
	
	Permissions(int offset){
		this.offset = offset;
	}
	
	public int getOffset(){
		return offset;
	}
	
	public static boolean hasPerm(GroupUser user, Permissions perm){
		if(perm == null) return true;
		if(GlobalAdmins.isAdmin(user)) return true;
		if(perm.equals(BOT_ADMIN)) return false;
		return user.hasPerm(me.itsghost.jdiscord.talkable.GroupUser.Permissions.valueOf(perm.name()));
	}
	
}
