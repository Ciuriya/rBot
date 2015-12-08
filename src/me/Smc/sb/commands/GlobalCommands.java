package me.Smc.sb.commands;

import java.io.File;
import java.net.URL;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import me.Smc.sb.listeners.Listener;
import me.Smc.sb.main.Main;
import me.Smc.sb.missingapi.MessageHistory;
import me.Smc.sb.perm.Permissions;
import me.Smc.sb.utils.Configuration;
import me.Smc.sb.utils.Utils;
import me.itsghost.jdiscord.OnlineStatus;
import me.itsghost.jdiscord.Role;
import me.itsghost.jdiscord.Server;
import me.itsghost.jdiscord.events.UserChatEvent;
import me.itsghost.jdiscord.message.MessageBuilder;
import me.itsghost.jdiscord.talkable.Group;
import me.itsghost.jdiscord.talkable.GroupUser;

public class GlobalCommands{

	public static void handleCommand(UserChatEvent e, String cmdName, boolean dm){
		String msg = e.getMsg().getMessage();
		switch(cmdName.toLowerCase()){
			case "commands": CommandList.execute(e, dm); return;
			case "help": CommandList.execute(e, dm); return;
			case "halt": if(!dm) Halt.execute(e); return;
			case "restart": e.getMsg().setMessage("stop 2"); Stop.execute(e, dm); return;
			case "update": e.getMsg().setMessage("stop 3"); Stop.execute(e, dm); return;
			case "stop": Stop.execute(e, dm); return;
			case "about": About.execute(e, dm); return;
			case "brainpower": brainpower(e, msg, dm); return;
			case "silent": silent(e, msg, dm); return;
			case "joinserver": joinServer(e, msg); return;
			case "addcom": addCommand(e, msg, dm); return;
			case "delcom": deleteCommand(e, msg, dm); return;
			case "setdel": setDelimiters(e, msg, dm); return;
			case "setprefix": setPrefix(e, msg, dm); return;
			case "suggest": suggest(e, msg, dm); return;
			case "readsuggest": Suggestions.execute(e, msg, dm); return;
			case "osustats": OsuStats.execute(e, msg, dm); return;
			case "clean": clean(e, msg, dm); return;
			case "listperms": listPerms(e, msg, dm); return;
			case "stats": stats(e, msg, dm); return;
			case "userinfo": userInfo(e, msg, dm); return;
			case "google": google(e, msg, dm); return;
			default: return;
		}
	}
	
	private static void brainpower(UserChatEvent e, String msg, boolean dm){
		if(dm) return;
		if(!Permissions.hasPerm(e.getUser(), Permissions.MANAGE_SERVER)) return;
		String[] split = msg.split(" ");
		if(split.length == 1) BrainPower.execute(e, false);
		else if(split.length == 2 && split[1].equalsIgnoreCase("true")) BrainPower.execute(e, true);
		else if(split.length == 2 && split[1].equalsIgnoreCase("false")) BrainPower.execute(e, false);
		else{
			Utils.error(e.getGroup(), e.getUser().getUser(), " Invalid arguments!");
			return;
		}
		return;
	}
	
	private static void silent(UserChatEvent e, String msg, boolean dm){
		if(dm) return;
		if(!Permissions.hasPerm(e.getUser(), Permissions.MANAGE_MESSAGES)) return;
		if(!Listener.checkArguments(msg, 2, e)) return;
		String[] split = msg.split(" ");
		if(split.length != 2){
			Utils.error(e.getGroup(), e.getUser().getUser(), " Invalid arguments!");
			return;
		}
		if(split[1].equalsIgnoreCase("true")) Silent.execute(e, true);
		else if(split[1].equalsIgnoreCase("false")) Silent.execute(e, false);
		else{
			Utils.error(e.getGroup(), e.getUser().getUser(), " Invalid arguments!");
			return;
		}
	}
	
	private static void joinServer(UserChatEvent e, String msg){
		if(!Listener.checkArguments(msg, 2, e)) return;
		String inviteID = msg.split(" ")[1];
		if(inviteID.startsWith("https://discord.gg/"))
			inviteID = inviteID.replace("https://discord.gg/", "");
		Main.api.joinInviteId(inviteID);
	}
	
	private static void addCommand(UserChatEvent e, String msg, boolean dm){
		if(dm) return;
		e.getMsg().deleteMessage();
		if(!Permissions.hasPerm(e.getUser(), Permissions.MANAGE_MESSAGES)) return;
		if(!Listener.checkArguments(msg, 3, e)) return;
		String command = msg.split(" ")[1];
		String cmdPrefix = Main.getCommandPrefix(e.getServer().getId());
		if(command.startsWith(cmdPrefix)) command = command.substring(cmdPrefix.length());
		if(Command.findCommand(e.getServer().getId(), command) != null){
			Utils.error(e.getGroup(), e.getUser().getUser(), " This command already exists!");
			return;
		}
		String instructions = Utils.removeStartSpaces(msg.replaceFirst(msg.split(" ")[0], "").replaceFirst(msg.split(" ")[1], ""));
		int delimiters = 0;
		if(instructions.startsWith("{delimiters=")){
			delimiters = Utils.stringToInt(instructions.split("\\{delimiters=")[1].split("}")[0]);
			instructions = "";
		}
		String desc = "";
		if(instructions.contains("{desc=")){
			desc = instructions.split("\\{desc=")[1].split("}")[0];
			instructions = Utils.removeStartSpaces(instructions.replace("{desc=" + desc + "}", ""));
		}
		Command cmd = new Command(e.getServer().getId(), command, instructions, delimiters, desc, false, false);
		cmd.save();
		Utils.info(e.getGroup(), e.getUser().getUser(), " " + cmdPrefix + command + " was added!");
	}
	
	private static void deleteCommand(UserChatEvent e, String msg, boolean dm){
		e.getMsg().deleteMessage();
		if(dm) return;
		if(!Permissions.hasPerm(e.getUser(), Permissions.MANAGE_MESSAGES)) return;
		if(!Listener.checkArguments(msg, 2, e)) return;
		String cmdPrefix = Main.getCommandPrefix(e.getServer().getId());
		String command = msg.split(" ")[1];
		if(command.startsWith(cmdPrefix)) command = command.replaceFirst(cmdPrefix, "");
		Command cmd = Command.findCommand(e.getServer().getId(), command);
		if(cmd == null){
			Utils.error(e.getGroup(), e.getUser().getUser(), " That command does not exist!");
			return;
		}
		cmd.delete();
		Utils.info(e.getGroup(), e.getUser().getUser(), " " + cmdPrefix + command + " was deleted!");
	}
	
	private static void setDelimiters(UserChatEvent e, String msg, boolean dm){
		e.getMsg().deleteMessage();
		if(dm) return;
		if(!Permissions.hasPerm(e.getUser(), Permissions.MANAGE_MESSAGES)) return;
		if(!Listener.checkArguments(msg, 4, e)) return;
		String cmdPrefix = Main.getCommandPrefix(e.getServer().getId());
		String command = msg.split(" ")[1];
		if(command.startsWith(cmdPrefix)) command = command.replaceFirst(cmdPrefix, "");
		Command cmd = Command.findCommand(e.getServer().getId(), command);
		if(cmd == null){
			Utils.error(e.getGroup(), e.getUser().getUser(), " This command does not exist!");
			return;
		}
		String instructions = Utils.removeStartSpaces(msg.replaceFirst(msg.split(" ")[0], "").replaceFirst(msg.split(" ")[1], ""));
		String dels = instructions;
		String[] instrSplit = instructions.split(" ");
		for(int i = 0; i < cmd.getDelimiterCount(); i++){
			dels += " " + instrSplit[i];
			instructions = instructions.replaceFirst(instrSplit[i], "");
		}
		dels = Utils.removeStartSpaces(dels);
		instructions = Utils.removeStartSpaces(instructions);
		cmd.setDelimiter(dels.split(" "), instructions);
		cmd.save();
		Utils.info(e.getGroup(), e.getUser().getUser(), " The delimiter combination for " + cmdPrefix + command + " was set!");
	}
	
	private static void setPrefix(UserChatEvent e, String msg, boolean dm){
		e.getMsg().deleteMessage();
		if(dm) return;
		if(!Listener.checkArguments(msg, 2, e)) return;
		if(!Permissions.hasPerm(e.getUser(), Permissions.MANAGE_SERVER)) return;
		String server = e.getServer().getId();
		Main.serverConfigs.get(server).writeValue("command-prefix", msg.split(" ")[1]);
		Utils.info(e.getGroup(), e.getUser().getUser(), " The server's prefix has been set to " + msg.split(" ")[1]);
	}
	
	private static void suggest(UserChatEvent e, String msg, boolean dm){
		e.getMsg().deleteMessage();
		if(!Listener.checkArguments(msg, 2, e)) return;
		Configuration cfg = new Configuration(new File("suggestions.txt"));
		cfg.appendToStringList("suggestions", Utils.getDate() + " Suggestion by " + e.getUser().getUser().getUsername() + " - " + msg.replaceFirst(msg.split(" ")[0] + " ", ""));
		Group userGroup = e.getGroup();
		if(!dm){
			Utils.info(userGroup, e.getUser().getUser(), " You have sent a suggestion!");
		}else Utils.info(userGroup, "Your suggestion has been sent!");
	}
	
	private static void clean(UserChatEvent e, String msg, boolean dm){
		e.getMsg().deleteMessage();
		if(dm) return;
		if(!Permissions.hasPerm(e.getUser(), Permissions.MANAGE_MESSAGES)) return;
		if(!Listener.checkArguments(msg, 2, e)) return;
		int amount = Integer.valueOf(msg.split(" ")[1]);
		boolean force = false;
		if(msg.split(" ").length >= 3 && msg.split(" ")[2].equalsIgnoreCase("all")) force = true;
		int cleared = MessageHistory.getHistory(e.getGroup().getId()).deleteLastMessages(amount, force);
		Utils.info(e.getGroup(), e.getUser().getUser(), " Cleared " + cleared + " messages!");
	}
	
	private static void listPerms(UserChatEvent e, String msg, boolean dm){
		e.getMsg().deleteMessage();
		if(dm) return;
		if(!Listener.checkArguments(msg, 2, e)) return;
		if(!Permissions.hasPerm(e.getUser(), Permissions.BOT_ADMIN)) return;
		String user = "";
		String[] split = msg.split(" ");
		for(int i = 1; i < split.length; i++)
			user += " " + split[i];
		user = user.substring(1);
		MessageBuilder builder = new MessageBuilder();
		builder.addString("```Permissions for " + user + "\n");
		for(Permissions perm : Permissions.values())
			builder.addString(perm.name() + " (" + Permissions.hasPerm(e.getServer().getGroupUserByUsername(user), perm) + ")\n");
		builder.addString("```");
		e.getGroup().sendMessage(builder.build());
	}
	
	private static void stats(UserChatEvent e, String msg, boolean dm){
		e.getMsg().deleteMessage();
		if(!Permissions.hasPerm(e.getUser(), Permissions.BOT_ADMIN)) return;
		int servers = Main.api.getAvailableServers().size();
		int users = 0, connected = 0;
		for(Server s : Main.api.getAvailableServers()){
			users += s.getConnectedClients().size();
			for(GroupUser user : s.getConnectedClients()){
				OnlineStatus status = user.getUser().getOnlineStatus();
				if(status == OnlineStatus.ONLINE || status == OnlineStatus.AWAY)
					connected += 1;
			}
		}
		MessageBuilder builder = new MessageBuilder();
		long uptime = System.currentTimeMillis() - Main.bootTime;
		builder.addString("```Connected to " + servers + " servers!\n") 
			   .addString(users + " total users (" + connected + " connected)\n")
			   .addString("Uptime: " + Utils.toDuration(uptime) + "\n")
			   .addString("Messages received since startup: " + Main.messagesThisSession + "```");
		e.getGroup().sendMessage(builder.build());
	}
	
	private static void userInfo(UserChatEvent e, String msg, boolean dm){
		e.getMsg().deleteMessage();
		if(dm) return;
		String user = "";
		String[] split = msg.split(" ");
		for(int i = 1; i < split.length; i++)
			user += " " + split[i];
		user = user.substring(1);
		GroupUser gUser = e.getServer().getGroupUserByUsername(user);
		if(gUser == null) return;
		String roles = "";
		for(Role r : gUser.getRoles()) roles += ", " + r.getName();
		roles = roles.substring(2);
		MessageBuilder builder = new MessageBuilder();
		builder.addString("```User info for " + user + "\n")
			   .addString("User status: " + gUser.getUser().getOnlineStatus().name().toLowerCase() + "\n")
			   .addString("Playing (" + gUser.getUser().getGame() + ")\n")
		       .addString("User id: " + gUser.getUser().getId() + "\n")
		       .addString("Discriminator: " + gUser.getDiscriminator() + "\n")
		       .addString("Roles: " + roles + "\n")
		       .addString("Avatar: " + gUser.getUser().getAvatar() + "```");
		e.getGroup().sendMessage(builder.build());
	}
	
	private static void google(UserChatEvent e, String msg, boolean dm){
		e.getMsg().deleteMessage();
		if(!Listener.checkArguments(msg, 2, e)) return;
		boolean images = false;
		int resultNum = 1;
		if(msg.contains("{images}")){
			images = true;
			msg = msg.replaceFirst("{images}", "");
		}
		if(msg.contains("{result=")){
			resultNum = Utils.stringToInt(msg.split("\\{result=")[1].split("}")[0]);
			msg = msg.replaceFirst("{result=" + resultNum + "}", "");
		}
		String searchQuery = "";
		String[] split = msg.split(" ");
		for(int i = 1; i < split.length; i++)
			searchQuery += " " + split[i];
		searchQuery = searchQuery.substring(1).replace(" ", "+");
		/*e.getGroup().sendMessage("1");
		for(String line : Utils.getHTMLCode("https://www.google.com/#q=" + searchQuery)){
			e.getGroup().sendMessage("2=" + line);
			if(line.contains("<a href=\"/url")){
				e.getGroup().sendMessage("3");
				String[] aSplit = line.split("<a ");
				for(String str : aSplit){
					e.getGroup().sendMessage("4");
					if(!str.contains("href=\"/url")) continue;
					String link = str.split("href=")[1].split("\">")[0];
					e.getGroup().sendMessage(link);
					return;
				}
			}
		}*/
		/*try{
			Document doc = Jsoup.parse(new URL("https://www.google.com/#q=" + searchQuery), 2000);
			System.out.println("doc: " + doc.html());
			Elements resultLinks = doc.select(".r > a");
			for (Element link : resultLinks) {
			    String href = link.attr("href");
			    System.out.println("title: " + link.text());
			    System.out.println("href: " + href);
			}    
		}catch(Exception ex){
			e.getGroup().sendMessage(ex.getMessage());
		}*/
		/*try{
			Document doc = Jsoup.connect("https://www.google.com/#q=" + searchQuery).maxBodySize(0).timeout(0).userAgent("Mozilla/5.0").get();
			e.getGroup().sendMessage("HTML: " + doc.html());
	        Elements results = doc.select(".r > a");
	        for(Element result : results){
	            String linkHref = result.attr("href");
	            String linkText = result.text();
	            e.getGroup().sendMessage("Text::" + linkText + ", URL::" + linkHref.substring(6, linkHref.indexOf("&")));
	        }
	        e.getGroup().sendMessage("shit");
		}catch(Exception ex){
			e.getGroup().sendMessage(ex.getMessage());
		}*/
	}
	
}
