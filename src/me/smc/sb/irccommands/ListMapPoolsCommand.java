package me.smc.sb.irccommands;

import org.pircbotx.PircBotX;
import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.PrivateMessageEvent;

import me.smc.sb.multi.MapPool;
import me.smc.sb.multi.Tournament;
import me.smc.sb.perm.Permissions;
import me.smc.sb.utils.Utils;

public class ListMapPoolsCommand extends IRCCommand{

	public ListMapPoolsCommand(){
		super("Lists all map pools.",
			  "<tournament name> ",
			  Permissions.TOURNEY_ADMIN,
			  "poollist");
	}
	
	@Override
	public String onCommand(MessageEvent<PircBotX> e, PrivateMessageEvent<PircBotX> pe, String discord, String[] args){
		String argCheck = Utils.checkArguments(args, 1);
		if(argCheck.length() > 0) return argCheck;
		
		String tournamentName = "";
		
		for(int i = 0; i < args.length; i++) tournamentName += args[i] + " ";
		Tournament t = Tournament.getTournament(tournamentName.substring(0, tournamentName.length() - 1));
		
		if(t == null) return "Invalid tournament!";
		
		String user = Utils.toUser(e, pe);
		
		if(t.isAdmin(user)){
			String msg = "Map Pools in " + t.getName();
			if(discord != null) msg = "```" + msg + "\n";
			else msg += "=";
			
			for(MapPool pool : t.getMapPools()){
				msg += "#" + pool.getPoolNum() + " - " + pool.getMaps().size() + " maps";
				if(discord != null) msg += "\n";
				else msg += "=";
			}
			
			if(discord == null){
				String built = "";
				for(String part : msg.split("=")){
					if(part.isEmpty()) continue;
					if(e == null && pe == null) built += part + "\n";
					else Utils.info(e, pe, discord, part);
				}
				
				if(built.length() > 0) return built.substring(0, built.length() - 1);
			}else return msg + "```";
		}
		
		return "";
	}
	
}
