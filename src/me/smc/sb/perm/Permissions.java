package me.smc.sb.perm;

import java.util.logging.Level;

import me.smc.sb.utils.Log;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;

public enum Permissions{

	INSTANT_INVITE(0),
	KICK_MEMBER(1),
	BAN_MEMBER(2),
	DISCORD_ADMIN(3),
	MANAGE_CHANNELS(4),
	MANAGE_SERVER(5),
	ADD_REACTION(6),
	VIEW_AUDIT_LOGS(7),
	PRIORITY_SPEAKER(8),
	VIEW_CHANNEL(10),
	READ_MESSAGES(10),
	SEND_MESSAGES(11),
	SEND_TTS_MESSAGES(12),
	MANAGE_MESSAGES(13),
	EMBED_LINKS(14),
	ATTACH_FILES(15),
	READ_MESSAGE_HISTORY(16),
	MENTION_EVERYONE(17),
	USE_EXTERNAL_EMOJIS(18),
	VOICE_STREAM(9),
	VOICE_CONNECT(20),
	VOICE_SPEAK(21),
	VOICE_MUTE_MEMBERS(22),
	VOICE_DEAFEN_MEMBERS(23),
	VOICE_MOVE_MEMBERS(24),
	VOICE_USE_VAD(25),
	NICKNAME_CHANGE(26),
	NICKNAME_MANAGE(27),
	MANAGE_ROLES(28),
	MANAGE_PERMISSIONS(28),
	MANAGE_WEBHOOKS(29),
	MANAGE_EMOTES(30),
	TOURNEY_ADMIN(999),
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
		else if(!(perm.equals(BOT_ADMIN) || perm.equals(IRC_BOT_ADMIN))){
			Member member = channel.getGuild().retrieveMemberById(user.getId()).complete();

			if(member != null){
				boolean hasActualPerm = member.hasPermission(channel, Permission.getFromOffset(perm.getOffset()));
				boolean hasAdminPerm = member.hasPermission(channel, Permission.getFromOffset(DISCORD_ADMIN.offset));

				return hasActualPerm || hasAdminPerm;
			}
		}
		
		return false;
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
