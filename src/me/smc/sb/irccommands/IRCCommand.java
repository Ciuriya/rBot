package me.smc.sb.irccommands;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import org.pircbotx.PircBotX;
import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.PrivateMessageEvent;

import me.smc.sb.main.Main;
import me.smc.sb.perm.Permissions;
import me.smc.sb.utils.BanchoCommands;
import me.smc.sb.utils.Log;
import me.smc.sb.utils.Utils;

public abstract class IRCCommand{

	private final String[] names;
	private final String description, usage;
	private final Permissions perm;
	private final boolean allowsTwitch;
	public static List<IRCCommand> commands;
	
	public IRCCommand(String description, String usage, Permissions perm, String...names){
		this(description, usage, perm, false, names);
	}
	
	public IRCCommand(String description, String usage, Permissions perm, boolean twitch, String...names){
		this.names = names;
		this.perm = perm;
		this.description = description;
		this.usage = usage;
		this.allowsTwitch = twitch;
	}
	
	public String getDescription(){
		return description;
	}
	
	public String getUsage(){
		return usage;
	}
	
	public String[] getNames(){
		return names;
	}
	
	public Permissions getPerm(){
		return perm;
	}
	
	public boolean allowsTwitch(){
		return allowsTwitch;
	}
	
	public boolean isName(String name){
		for(String n : names)
			if(n.equalsIgnoreCase(name))
				return true;
		return false;
	}
	
	public static String handleCommand(MessageEvent<PircBotX> e, PrivateMessageEvent<PircBotX> pe, String discord, String msg){
		if(pe != null)
			try{
				Main.ircBot.sendIRC().joinChannel(pe.getUser().getNick());
			}catch(Exception ex){
				Log.logger.log(Level.INFO, "Could not join channel " + pe.getUser().getNick());
			}
		
		String user = Utils.toUser(e, pe);
		
		String[] split = msg.split(" ");
		
		for(IRCCommand ic : commands)
			if(ic.isName(split[0]) && (me.smc.sb.perm.Permissions.hasPerm(user, ic.perm) || Utils.isTwitch(e))){
				if(!ic.allowsTwitch() && Utils.isTwitch(e)) continue;
					
				String[] args = msg.replace(split[0] + " ", "").split(" ");
				
				if(!msg.contains(" ")) args = new String[]{};
				return ic.onCommand(e, pe, discord, args);
			}
		
		if(!BanchoCommands.isBanchoCommand(split[0]))
			return "This is not a command! Use !help if you are lost!";
		
		return "";
	}
	
	public static void registerCommands(){
		commands = new ArrayList<IRCCommand>();
		commands.add(new HelpCommand());
		commands.add(new CreateTournamentCommand());
		commands.add(new CreateMapPoolCommand());
		commands.add(new CreateTeamCommand());
		commands.add(new CreateMatchCommand());
		commands.add(new DeleteTournamentCommand());
		commands.add(new DeleteMapPoolCommand());
		commands.add(new DeleteTeamCommand());
		commands.add(new DeleteMatchCommand());
		commands.add(new ListMapPoolsCommand());
		commands.add(new ListMapsInPoolCommand());
		commands.add(new GeneratePoolDownloadCommand());
		commands.add(new ListTournamentsCommand());
		commands.add(new ListTeamsCommand());
		commands.add(new ListTeamPlayersCommand());
		commands.add(new ListMatchesCommand());
		commands.add(new ListGamesCommand());
		commands.add(new ListMatchAdminsCommand());
		commands.add(new AddMatchAdminCommand());
		commands.add(new RemoveMatchAdminCommand());
		commands.add(new SetScoreV2Command());
		commands.add(new AddMapToPoolCommand());
		commands.add(new RemoveMapFromPoolCommand());
		commands.add(new SetMapPoolSheetCommand());
		commands.add(new SetTeamPlayersCommand());
		commands.add(new SetMatchPoolCommand());
		commands.add(new SetMatchTeamsCommand());
		commands.add(new SetBestOfCommand());
		commands.add(new SetMatchScheduleCommand());
		commands.add(new ScanPlayerRanksCommand());
		commands.add(new ForceStopGameCommand());
		commands.add(new ForceStartGameCommand());
		commands.add(new SetGameScoreCommand());
		commands.add(new SetResultDiscordCommand());
		commands.add(new SetRankBoundsCommand());
		commands.add(new SetDisplayNameCommand());
		commands.add(new SetPickWaitTimeCommand());
		commands.add(new SetBanWaitTimeCommand());
		commands.add(new SetReadyWaitTimeCommand());
		commands.add(new SetTwitchChannelCommand());
		commands.add(new SetMatchPriorityCommand());
		commands.add(new JoinMatchCommand());
		commands.add(new RandomCommand());
		commands.add(new SelectMapCommand());
		commands.add(new InvitePlayerCommand());
		commands.add(new BanMapCommand());
		commands.add(new PassTurnCommand());
		commands.add(new SkipRematchCommand());
		commands.add(new ContestCommand());
		commands.add(new ChangeWarmupModCommand());
		commands.add(new MPLinkCommand());
		commands.add(new CurrentScoreCommand());
		commands.add(new CurrentMapCommand());
	}
	
	public abstract String onCommand(MessageEvent<PircBotX> e, PrivateMessageEvent<PircBotX> pe, String discord, String[] args);
	
}
