package me.smc.sb.irccommands;

import org.pircbotx.PircBotX;
import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.PrivateMessageEvent;

import me.smc.sb.multi.Map;
import me.smc.sb.multi.MapPool;
import me.smc.sb.multi.Tournament;
import me.smc.sb.perm.Permissions;
import me.smc.sb.utils.Utils;

public class AddMapToPoolCommand extends IRCCommand{

	public AddMapToPoolCommand(){
		super("Adds a map to the map pool.",
			  "<tournament name> <map pool number> <map url> <map category (0 = NM -> 5 = TB)> ",
			  Permissions.TOURNEY_ADMIN,
			  "pooladdmap");
	}
	
	@Override
	public String onCommand(MessageEvent<PircBotX> e, PrivateMessageEvent<PircBotX> pe, String discord, String[] args){
		String argCheck = Utils.checkArguments(args, 4);
		if(argCheck.length() > 0) return argCheck;
		
		String tournamentName = "";
		
		for(int i = 0; i < args.length - 3; i++) tournamentName += args[i] + " ";
		Tournament t = Tournament.getTournament(tournamentName.substring(0, tournamentName.length() - 1));
		
		if(t == null) return "Invalid tournament!";
		
		if(Utils.stringToInt(args[args.length - 3]) == -1) return "Map pool number needs to be a number!";
		if(t.getPool(Utils.stringToInt(args[args.length - 3])) == null) return "The map pool is invalid!";
		
		String user = Utils.toUser(e, pe);
		
		if(t.isAdmin(user)){
			String url = Utils.takeOffExtrasInBeatmapURL(args[args.length - 2]);
			
			if(!url.matches("^https?:\\/\\/osu.ppy.sh\\/b\\/[0-9]{1,8}"))
				return "Invalid URL, example format: https://osu.ppy.sh/b/123456";
			
			if(Utils.stringToInt(args[args.length - 1]) < 0 || Utils.stringToInt(args[args.length - 1]) > 5)
				return "The map category needs to be within 0 to 5! (0 = NM, 1 = FM, 2 = HD, 3 = HR, 4 = DT, 5 = TB)";
			
			MapPool pool = t.getPool(Utils.stringToInt(args[args.length - 3]));
			
			Map map = new Map(url, Utils.stringToInt(args[args.length - 1]), pool);
			pool.addMap(map);
			pool.save(false);
			
			return "Added beatmap #" + map.getBeatmapID() + " to the pool!";
		}
		
		return "";
	}
	
}
