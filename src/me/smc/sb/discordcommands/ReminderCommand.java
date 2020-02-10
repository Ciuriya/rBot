package me.smc.sb.discordcommands;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import me.smc.sb.main.Main;
import me.smc.sb.utils.Configuration;
import me.smc.sb.utils.Utils;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

public class ReminderCommand extends GlobalCommand{
	
	public static Configuration config;
	public static Map<User, List<Reminder>> reminders;
	
	public ReminderCommand(){
		super(null, 
			  " - Allows users to setup reminders", 
			  "{prefix}reminder\nThis command lets you get a customized reminder.\n\n" +
			  "----------\nUsage\n----------\n{prefix}reminder 2y2M2w2d2h2m2s enter message here - Reminds you in x time of y\n" + 
			  "{prefix}reminder list - Lists active reminders\n{prefix}reminder show x - Shows you the reminder with index x's message\n" +
			  "{prefix}reminder cancel x - Cancels an active reminder with x index (use list to find out)\n{prefix}reminder cancel all - Cancels all active reminders\n\n" +
			  "----------\nAliases\n----------\n{prefix}remind", 
			  true, 
			  "reminder", "remind");
		
		config = new Configuration(new File("reminders.txt"));
		reminders = new HashMap<User, List<Reminder>>();
		
		loadAll();
	}

	@Override
	public void onCommand(MessageReceivedEvent e, String[] args){
		Utils.deleteMessage(e.getChannel(), e.getMessage());
		
		if(!Utils.checkArguments(e, args, 1)) return;
		
		if(args[0].equalsIgnoreCase("list")){
			List<Reminder> reminders = new ArrayList<Reminder>();
			
			if(ReminderCommand.reminders.containsKey(e.getAuthor())) 
				reminders = ReminderCommand.reminders.get(e.getAuthor());
			
			if(reminders.isEmpty()){
				Utils.info(e.getChannel(), "You have no active reminders!");
				return;
			}
			
			StringBuilder builder = new StringBuilder();
			
			builder.append("**Active reminders for " + e.getAuthor().getAsMention() + "**")
				   .append("\n```diff");
			
			for(int i = 0; i < reminders.size(); i++)
				builder.append("\n" + i + " - " + Utils.toDate(reminders.get(i).epochTimestamp) + " UTC");
			
			Utils.infoBypass(e.getChannel(), builder.toString() + "```");
		}else if(args[0].equalsIgnoreCase("cancel")){
			if(!Utils.checkArguments(e, args, 2)) return;
			
			List<Reminder> reminders = new ArrayList<Reminder>();
			
			if(ReminderCommand.reminders.containsKey(e.getAuthor())) 
				reminders = ReminderCommand.reminders.get(e.getAuthor());
			
			if(args[1].equalsIgnoreCase("all")){
				int reminderCount = reminders.size();
				
				for(Reminder reminder : new ArrayList<>(reminders)){
					reminder.cancel();
					reminder.delete();
				}
				
				Utils.infoBypass(e.getChannel(), "Cancelled " + reminderCount + " reminders!");
			}else{
				int index = Utils.stringToInt(args[1]);
				
				if(index == -1 || index >= reminders.size()){
					Utils.info(e.getChannel(), "No active reminders matching that index exist!");
					return;
				}
				
				Reminder reminder = reminders.get(index);
				
				reminder.cancel();
				reminder.delete();
				
				Utils.infoBypass(e.getChannel(), "Cancelled 1 reminder!");
			}
		}else if(args[0].equalsIgnoreCase("show")){
			if(!Utils.checkArguments(e, args, 2)) return;
			
			List<Reminder> reminders = new ArrayList<Reminder>();
			
			if(ReminderCommand.reminders.containsKey(e.getAuthor())) 
				reminders = ReminderCommand.reminders.get(e.getAuthor());
			
			int index = Utils.stringToInt(args[1]);
			
			if(index == -1 || index >= reminders.size()){
				Utils.info(e.getChannel(), "No active reminders matching that index exist!");
				return;
			}

			Utils.infoBypass(e.getChannel(), "**Reminder " + index + "**\n" + reminders.get(index).message.replaceAll("\\|\\|\\|", "\n"));
		}else{
			if(!Utils.checkArguments(e, args, 2)) return;
			
			String duration = args[0];
			String message = "";
			
			for(int i = 1; i < args.length; i++)
				message += " " + args[i].replaceAll("\n", "|||");
			
			message = message.substring(1);
			
			long time = Utils.fromDuration(duration);
			
			if(time <= 0){
				Utils.info(e.getChannel(), "Invalid duration! Please use this format: 2y2M2w2d2h2m2s " + 
										   "(y = years, M = months, w = weeks, d = days, h = hours, m = minutes, s = seconds");
				return;
			}
			
			long epochReminderTime = Utils.getCurrentTimeUTC() + time;
			
			ArrayList<String> users = config.getStringList("users");
			
			if(!users.contains(e.getAuthor().getId())){
				users.add(e.getAuthor().getId());
				config.writeStringList("users", users, true);
			}
			
			Reminder reminder = new Reminder(e.getAuthor(), epochReminderTime, message);
			
			reminder.register();
			reminder.schedule();
			
			Utils.infoBypass(e.getChannel(), e.getAuthor().getAsMention() + " You will be reminded on " + Utils.toDate(epochReminderTime) + " UTC");
		}
	}
	
	private void loadAll(){
		List<String> users = config.getStringList("users");
		
		for(String strUser : users){
			User user = Main.api.getUserById(strUser);
			
			if(user == null) continue;
			
			List<String> strReminders = config.getStringList(user.getId() + "-reminders");
			List<Reminder> reminders = new ArrayList<>();
			
			for(String strReminder : strReminders){
				Reminder reminder = Reminder.fromString(user, strReminder);
				
				if(reminder.epochTimestamp < Utils.getCurrentTimeUTC()){
					reminder.delete();
					continue;
				}
				
				reminder.schedule();
				
				reminders.add(reminder);
			}
			
			Collections.sort(reminders, new ReminderSort());
			
			ReminderCommand.reminders.put(user, reminders);
		}
	}
}

class Reminder{
	public User user;
	public long epochTimestamp;
	public String message;
	
	private Timer timer;
	
	public Reminder(User user, long epochTimestamp, String message){
		this.user = user;
		this.epochTimestamp = epochTimestamp;
		this.message = message;
		
		timer = new Timer();
	}
	
	public void schedule(){
		long delay = epochTimestamp - Utils.getCurrentTimeUTC();
		
		if(delay <= 0) delay = 0; // instant reminder
		
		timer.schedule(new TimerTask(){
			public void run(){
				Utils.infoBypass(user.openPrivateChannel().complete(), "**Reminder**\n" + message.replaceAll("\\|\\|\\|", "\n"));
				delete();
			}
		}, epochTimestamp - Utils.getCurrentTimeUTC());
	}
	
	public void register(){
		ArrayList<String> reminders = ReminderCommand.config.getStringList(user.getId() + "-reminders");
		List<Reminder> loadedReminders = new ArrayList<>();
		String strReminder = toString();
		
		if(ReminderCommand.reminders.containsKey(user))
			loadedReminders = ReminderCommand.reminders.get(user);
		
		if(!loadedReminders.contains(this)){
			loadedReminders.add(this);
			Collections.sort(loadedReminders, new ReminderSort());
			
			ReminderCommand.reminders.put(user, loadedReminders);
		}
		
		if(reminders.contains(strReminder)) return;
		else reminders.add(strReminder);
		
		ReminderCommand.config.writeStringList(user.getId() + "-reminders", reminders, false);
	}
	
	public void cancel(){
		timer.cancel();
	}
	
	public void delete(){
		ArrayList<String> reminders = ReminderCommand.config.getStringList(user.getId() + "-reminders");
		List<Reminder> loadedReminders = new ArrayList<>();
		String strReminder = toString();
		
		if(ReminderCommand.reminders.containsKey(user))
			loadedReminders = ReminderCommand.reminders.get(user);
		
		if(loadedReminders.contains(this)){
			loadedReminders.remove(this);
			Collections.sort(loadedReminders, new ReminderSort());
			
			ReminderCommand.reminders.put(user, loadedReminders);
		}
		
		if(reminders.contains(strReminder))
			reminders.remove(strReminder);
		else return;
		
		ReminderCommand.config.writeStringList(user.getId() + "-reminders", reminders, false);
	}
	
	public String toString(){
		return epochTimestamp + "-" + message;
	}
	
	public static Reminder fromString(User user, String reminder){
		long epochTimestamp = Utils.stringToLong(reminder.split("-")[0]);
		String message = reminder.split("-")[1];
		
		return new Reminder(user, epochTimestamp, message);
	}
}

class ReminderSort implements Comparator<Reminder>{
	@Override
	public int compare(Reminder o1, Reminder o2){
		return Long.compare(o1.epochTimestamp, o2.epochTimestamp);
	}
}