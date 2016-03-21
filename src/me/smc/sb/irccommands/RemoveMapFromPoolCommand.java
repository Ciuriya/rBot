package me.smc.sb.irccommands;

import org.pircbotx.PircBotX;
import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.PrivateMessageEvent;

import me.smc.sb.multi.Tournament;
import me.smc.sb.perm.Permissions;
import me.smc.sb.utils.Utils;

public class RemoveMapFromPoolCommand extends IRCCommand{

	public RemoveMapFromPoolCommand(){
		super("Removes a map from the map pool.",
			  "<tournament name> <map pool number> <map url> ",
			  Permissions.IRC_BOT_ADMIN,
			  "pooldeletemap");
	}
	
	@Override
	public String onCommand(MessageEvent<PircBotX> e, PrivateMessageEvent<PircBotX> pe, String discord, String[] args){
		String argCheck = Utils.checkArguments(args, 3);
		if(argCheck.length() > 0) return argCheck;
		
		String tournamentName = "";
		
		for(int i = 0; i < args.length - 2; i++) tournamentName += args[i] + " ";
		Tournament t = Tournament.getTournament(tournamentName.substring(0, tournamentName.length() - 1));
		
		if(t == null) return "Invalid tournament!";
		
		if(Utils.stringToInt(args[args.length - 2]) == -1) return "Map pool number needs to be a number!";
		if(t.getPool(Utils.stringToInt(args[args.length - 2])) == null) return "The map pool is invalid!";
		
		String url = Utils.takeOffExtrasInBeatmapURL(args[args.length - 2]);
		
		if(!url.matches("^https?:\\/\\/osu.ppy.sh\\/b\\/[0-9]{1,8}")){
			return "Invalid URL, example format: https://osu.ppy.sh/b/123456";
		}
		
		t.getPool(Utils.stringToInt(args[args.length - 2])).removeMap(args[args.length - 1]);
		t.getPool(Utils.stringToInt(args[args.length - 2])).save(false);
		
		return "Removed " + args[args.length - 1] + " from the map pool!";
	}
	
}
