package me.Smc.sb.missingapi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

import me.Smc.sb.main.Main;
import me.itsghost.jdiscord.message.Message;
import me.itsghost.jdiscord.talkable.Group;

public class MessageHistory{

	//clean {amount} (all)
	
	public static HashMap<String, MessageHistory> groupHistory = new HashMap<String, MessageHistory>();
	public static final int maxAmount = 100;

	private LinkedList<Message> messages;
	
	public MessageHistory(String groupId){
		messages = new LinkedList<Message>();
		groupHistory.put(groupId, this);
	}
	
	public void addMessage(Message msg){
		messages.addFirst(msg);
		if(messages.size() > maxAmount) messages.removeLast();
	}
	
	public Message getLastMessageByUser(String id){
		for(Message message : messages)
			if(message.getSender().getId().equalsIgnoreCase(id))
				return message;
		return null;
	}
	
	public int deleteLastMessages(int amount, boolean force){
		int i = 0;
		ArrayList<Message> canDelete = new ArrayList<Message>();
		while(canDelete.size() < amount){
			if(messages.size() > i){
				Message msg = messages.get(i);
				if(!force && msg.getSender().getId().equalsIgnoreCase(Main.api.getSelfInfo().getId()))
					canDelete.add(msg);
				else if(force) canDelete.add(msg);
			}else break;
			i++;
		}
		for(Message msg : canDelete){
			messages.remove(msg);
			msg.deleteMessage();
		}
		int cleared = canDelete.size();
		canDelete.clear();
		return cleared;
	}
	
	public static void addMessage(Group group, Message msg){
		groupHistory.get(group.getId()).addMessage(msg);
	}
	
	public static MessageHistory getHistory(String groupId){
		if(!groupHistory.containsKey(groupId)) return null;
		return groupHistory.get(groupId);
	}
	
}
