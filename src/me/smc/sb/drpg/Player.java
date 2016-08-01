package me.smc.sb.drpg;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Player{

	private Entity entity;
	private String discordId;
	private Map<Integer, Integer> quests; //0 = ongoing, 1 = completed
	public static List<Player> players = new ArrayList<>();
	
	public Player(String name, String discordId, boolean load){
		this.discordId = discordId;
		
		if(load) load();
		else insert(name);
		
		players.add(this);
	}
	
	public Entity getEntity(){
		return entity;
	}
	
	public String getDiscordId(){
		return discordId;
	}
	
	public List<Integer> getCompletedQuests(){
		return quests.keySet().stream().filter(i -> quests.get(i) == 1).collect(Collectors.toList());
	}
	
	public List<Integer> getOngoingQuests(){
		return quests.keySet().stream().filter(i -> quests.get(i) == 0).collect(Collectors.toList());
	}
	
	public void addCompletedQuest(int questId){
		quests.put(questId, 1);
		save();
	}
	
	public void addOngoingQuest(int questId){
		quests.put(questId, 0);
		save();
	}
	
	public void abandonQuest(int questId){
		if(quests.containsKey(questId) && quests.get(questId) != 1){
			quests.remove(questId);
			save();
		}
	}
	
	public boolean hasQuest(int questId){
		return quests.containsKey(questId);
	}
	
	public boolean isQuestOngoing(int questId){
		return quests.containsKey(questId) && quests.get(questId) == 0;
	}
	
	public boolean isQuestCompleted(int questId){
		return quests.containsKey(questId) && quests.get(questId) == 1;
	}
	
	public static Player getPlayer(String discordId){
		return players.stream().filter(p -> p.discordId.equals(discordId)).findFirst().orElse(null);
	}
	
	private void insert(String name){
		//insert in player
		//change that shit for better values
		entity = new Entity(-1, name, 0f, "Peasant", "None", "Caucasian", true, 
							0f, 0f, 0f, 0f, 0f, 0f, -1, -1, "", 0, -1, -1, -1);
	}
	
	public void save(){
		
	}
	
	public void delete(){
		
	}
	
	private void load(){
		//find id using discord id and load entity with it (only when needed, don't load all!)
	}
	
}
