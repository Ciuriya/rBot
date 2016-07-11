package me.smc.sb.drpg;

import java.util.ArrayList;
import java.util.List;

public class Party{

	private int id;
	private String channelId;
	private Entity leader;
	private List<Integer> members;
	public static List<Party> parties = new ArrayList<>();
	
	public Party(int id){
		this.id = id;
		
		load();
	}
	
	public Party(int id, String channelId, int entityId){
		this.id = id;
		this.channelId = channelId;
		this.leader = Entity.getEntity(entityId);
		
		members = new ArrayList<>();
		Entity.entities.stream().filter(e -> e.getParty() != null && e.getParty().id == id).forEach(e -> members.add(e.getId()));
		
		if(id == -1) //-1 for adding
			insert();
		
		parties.add(this);
	}
	
	public int getId(){
		return id;
	}
	
	public String getChannelId(){
		return channelId;
	}
	
	public Entity getLeader(){
		return leader;
	}
	
	public void changeLeader(int entityId){
		if(entityId != leader.getId()){
			if(!members.contains(entityId))
				members.add(entityId);
			
			leader = Entity.getEntity(entityId);
			save();	
		}
	}
	
	public void addMember(int entityId){
		if(!members.contains(entityId)){
			members.add(entityId);
			save();
		}
	}
	
	public void removeMember(int entityId){
		if(members.contains(entityId)){
			members.remove((Integer) entityId);
			save();
		}
	}
	
	public boolean isMember(int entityId){
		return members.contains(entityId);
	}
	
	public List<Integer> getMembers(){
		return members;
	}
	
	public static Party getParty(int id){
		return parties.stream().filter(p -> p.id == id).findFirst().orElse(null);
	}
	
	private void insert(){
		
	}
	
	public void save(){
		
	}
	
	public void delete(){
		
	}
	
	private void load(){
		
	}
	
}
