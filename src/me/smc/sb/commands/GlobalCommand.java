package me.smc.sb.commands;

import java.util.ArrayList;
import java.util.List;

import me.itsghost.jdiscord.events.UserChatEvent;
import me.itsghost.jdiscord.talkable.GroupUser;
import me.smc.sb.main.Main;
import me.smc.sb.perm.Permissions;

public abstract class GlobalCommand{

    private final Permissions perm;
    private final String[] names;
    private final String description, extendedDescription;
    private final boolean allowsDm;
    public static List<GlobalCommand> commands;
	
	public GlobalCommand(Permissions perm, String description, String extendedDescription, boolean allowsDm, String...names){
		this.perm = perm;
		this.description = description;
		this.extendedDescription = extendedDescription;
		this.allowsDm = allowsDm;
		this.names = names;
	}
	
	public boolean canUse(GroupUser user){
		return Permissions.hasPerm(user, perm);
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
	
	public static boolean handleCommand(UserChatEvent e, String msg){
		String[] split = msg.split(" ");
		for(GlobalCommand gc : commands)
			if(gc.isName(split[0])){
				if(!gc.allowsDm() && e.isDm()) return true;
				Main.commandsUsedThisSession++;
				String[] args = msg.replace(split[0] + " ", "").split(" ");
				if(!msg.contains(" ")) args = new String[]{};
				gc.onCommand(e, args);
				return true;
			}
		return false;
	}
	
	public static void registerCommands(){
		commands = new ArrayList<GlobalCommand>();
		commands.add(new AboutCommand());
		commands.add(new BrainPowerCommand());
		commands.add(new CleanCommand());
		commands.add(new EditCmdCommand());
		commands.add(new HaltCommand());
		commands.add(new HelpCommand());
		commands.add(new JoinServerCommand());
		commands.add(new ListPermsCommand());
		commands.add(new OsuStatsCommand());
		commands.add(new SearchCommand());
		commands.add(new SetPrefixCommand());
		commands.add(new SilentCommand());
		commands.add(new StatsCommand());
		commands.add(new StopCommand());
		commands.add(new SuggestCommand());
		commands.add(new UserInfoCommand());
	}
	
	public abstract void onCommand(UserChatEvent e, String[] args);
	
}
