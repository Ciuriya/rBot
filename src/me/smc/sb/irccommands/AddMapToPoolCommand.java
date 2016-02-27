package me.smc.sb.irccommands;

import org.pircbotx.PircBotX;
import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.PrivateMessageEvent;

import me.smc.sb.multi.Map;
import me.smc.sb.multi.Tournament;
import me.smc.sb.perm.Permissions;
import me.smc.sb.utils.Utils;

public class AddMapToPoolCommand extends IRCCommand{

	public AddMapToPoolCommand(){
		super("Adds a map to the map pool.",
			  "<tournament name> <map pool number> <map url> <map category (0 = NM -> 5 = TB)> ",
			  Permissions.IRC_BOT_ADMIN,
			  "pooladdmap");
	}
	
	@Override
	public void onCommand(MessageEvent<PircBotX> e, PrivateMessageEvent<PircBotX> pe, String discord, String[] args){
		if(!Utils.checkArguments(e, pe, discord, args, 4)) return;
		
		String tournamentName = "";
		
		for(int i = 0; i < args.length - 3; i++) tournamentName += args[i] + " ";
		Tournament t = Tournament.getTournament(tournamentName.substring(0, tournamentName.length() - 1));
		
		if(t == null){Utils.info(e, pe, discord, "Invalid tournament!"); return;}
		
		if(Utils.stringToInt(args[args.length - 3]) == -1){Utils.info(e, pe, discord, "Map pool number needs to be a number!"); return;}
		if(t.getPool(Utils.stringToInt(args[args.length - 3])) == null){Utils.info(e, pe, discord, "The map pool is invalid!"); return;}
		
		String url = Utils.takeOffExtrasInBeatmapURL(args[args.length - 2]);
		
		if(!url.matches("^https?:\\/\\/osu.ppy.sh\\/b\\/[0-9]{1,8}")){
			Utils.info(e, pe, discord, "Invalid URL, example format: https://osu.ppy.sh/b/123456");
			return;
		}
		
		if(Utils.stringToInt(args[args.length - 1]) < 0 || Utils.stringToInt(args[args.length - 1]) > 5){
			Utils.info(e, pe, discord, "The map category needs to be within 0 to 5! (0 = NM, 1 = FM, 2 = HD, 3 = HR, 4 = DT, 5 = TB)");
			return;
		}
		
		Map map = new Map(url, Utils.stringToInt(args[args.length - 1]));
		t.getPool(Utils.stringToInt(args[args.length - 3])).addMap(map);
		t.getPool(Utils.stringToInt(args[args.length - 3])).save(false);
		
		Utils.info(e, pe, discord, "Added beatmap #" + map.getBeatmapID() + " to the pool!");
	}
	
}
