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
			  Permissions.IRC_BOT_ADMIN,
			  "poollist");
	}
	
	@Override
	public void onCommand(MessageEvent<PircBotX> e, PrivateMessageEvent<PircBotX> pe, String discord, String[] args){
		if(!Utils.checkArguments(e, pe, discord, args, 1)) return;
		
		String tournamentName = "";
		
		for(int i = 0; i < args.length; i++) tournamentName += args[i] + " ";
		Tournament t = Tournament.getTournament(tournamentName.substring(0, tournamentName.length() - 1));
		
		if(t == null){Utils.info(e, pe, discord, "Invalid tournament!"); return;}
		
		String msg = "Map Pools in " + t.getName();
		if(discord != null) msg = "```" + msg + "\n";
		else msg += "=";
		
		for(MapPool pool : t.getMapPools()){
			msg += "#" + pool.getPoolNum() + " - " + pool.getMaps().size() + " maps";
			if(discord != null) msg += "\n";
			else msg += "=";
		}
		
		if(discord == null)
			for(String part : msg.split("=")){
				if(part.isEmpty()) continue;
				Utils.info(e, pe, discord, part);
			}
		else Utils.info(e, pe, discord, msg + "```");
	}
	
}
