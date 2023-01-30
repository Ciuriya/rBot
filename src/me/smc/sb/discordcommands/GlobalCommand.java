package me.smc.sb.discordcommands;

import java.util.ArrayList;
import java.util.List;

import me.smc.sb.main.Main;
import me.smc.sb.perm.Permissions;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public abstract class GlobalCommand{

    private final Permissions perm;
    private final String[] names;
    private final String description, extendedDescription;
    private final boolean allowsDm;
    public static List<GlobalCommand> commands = new ArrayList<GlobalCommand>();
	
	public GlobalCommand(Permissions perm, String description, String extendedDescription, boolean allowsDm, String...names){
		this.perm = perm;
		this.description = description;
		this.extendedDescription = extendedDescription;
		this.allowsDm = allowsDm;
		this.names = names;
	}
	
	public boolean canUse(User user, MessageChannelUnion channel){
		if(channel.getType().isGuild())
			return Permissions.hasPerm(user, channel, perm);
		else return Permissions.check(user, perm);
	}
	
	public String getDescription(){
		return description;
	}
	
	public String getExtendedDescription(){
		return extendedDescription;
	}
	
	public boolean allowsDm(){
		return allowsDm;
	}
	
	public String[] getNames(){
		return names;
	}
	
	public String getNamesDisplay(){
		String build = "";
		
		for(String name : names)
			build += "/" + name;
		
		return build.substring(1);
	}
	
	public boolean isName(String name){
		for(String n : names)
			if(n.equalsIgnoreCase(name))
				return true;
		return false;
	}
	
	public static boolean handleCommand(MessageReceivedEvent e, String msg){
		String[] split = msg.split(" ");
		
		for(GlobalCommand gc : commands)
			if(gc.isName(split[0]) && gc.canUse(e.getAuthor(), e.getChannel())){
				
				if(!gc.allowsDm() && e.isFromType(ChannelType.PRIVATE)) return true;
				
				Main.commandsUsedThisSession++;
				
				final String[] args = msg.replace(split[0] + " ", "").split(" ");
				
				Thread t = new Thread(new Runnable(){
					public void run(){
						if(msg.contains(" ")) gc.onCommand(e, args);
						else gc.onCommand(e, new String[]{});
					}
				});
				
				t.start();
				return true;
			}
		return false;
	}
	
	public static void registerCommands(){
		commands.add(new AboutCommand());
		commands.add(new BrainPowerCommand());
		commands.add(new CleanCommand());
		commands.add(new DeadpoolCommand());
		commands.add(new DownloadOsuMapCommand());
		commands.add(new EditCmdCommand());
		commands.add(new ExecIRCCommand());
		commands.add(new ExecProcessCommand());
		commands.add(new HaltCommand());
		commands.add(new HelpCommand());
		commands.add(new IdToUserCommand());
		commands.add(new JoinServerCommand());
		commands.add(new ListPermsCommand());
		commands.add(new ListServersCommand());
		commands.add(new MessageIRCCommand());
		commands.add(new MsgUserCommand());
		commands.add(new OsuLastTopPlays());
		commands.add(new OsuLiveLeaderboardCommand());
		commands.add(new OsuMapRankTrackCommand());
		commands.add(new OsuRecentPlayCommand());
		commands.add(new OsuScoresCommand());
		commands.add(new OsuSetProfileCommand());
		commands.add(new OsuStatsCommand());
		commands.add(new OsuTrackCommand());
		commands.add(new PingCommand());
		commands.add(new PlayFormatCommand());
		commands.add(new PollIRCCommand());
		commands.add(new PollCommand());
		commands.add(new ReceivePMCommand());
		commands.add(new ReminderCommand());
		commands.add(new ReportCommand());
		commands.add(new RPGCommand());
		commands.add(new SearchCommand());
		commands.add(new SetPrefixCommand());
		commands.add(new SilentCommand());
		commands.add(new SMTScoreRecentCalculationCommand());
		commands.add(new StatsCommand());
		commands.add(new StopCommand());
		commands.add(new UserInfoCommand());
		commands.add(new VoiceCommand());
		commands.add(new YoutubeDownloadCommand());
	}
	
	public abstract void onCommand(MessageReceivedEvent e, String[] args);
	
}
