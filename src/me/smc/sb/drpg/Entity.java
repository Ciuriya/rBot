package me.smc.sb.drpg;

import java.util.ArrayList;
import java.util.List;

public class Entity{
	
	//dynamic mob adding and shit?
	//if id is player, don't load
	
	private int id;
	private String name;
	private float exp;
	//class here
	//spec here
	//race here
	private boolean gender; //true is male
	private float str;
	private float end;
	private float dex;
	private float intel;
	private float wis;
	private float cha;
	private float x; //more precise than just the tile
	private float y; //these store the exact location within the tile (how long until they exit it for example)
	private String desc;
	private Guild guild;
	private Tile tile;
	private Party party;
	private List<Integer> friends;
	private List<String> dialogs;
	private List<Integer> skills;
	public static List<Entity> entities = new ArrayList<>();
	
	//For loading only
	public Entity(int id){
		this.id = id;
		
		load();
	}
	
	public Entity(int id, String name, float exp, String _class, String spec, String race,
				  boolean gender, float str, float end, float dex, float intel, float wis, float cha,
				  float x, float y, String desc, int guildId, int tileId, int currentPartyId){
		this.id = id;
		this.name = name;
		this.exp = exp;
		//class
		//spec
		//race
		this.gender = gender;
		this.str = str;
		this.end = end;
		this.dex = dex;
		this.intel = intel;
		this.wis = wis;
		this.cha = cha;
		this.x = x;
		this.y = y;
		this.desc = desc; //can be null
		if(guildId != -1) this.guild = Guild.getGuild(guildId);
		this.tile = Tile.getTile(tileId);
		if(currentPartyId != -1) party = Party.getParty(currentPartyId);
		
		skills = new ArrayList<>();
		ESkill.skills.stream().filter(s -> s.getEntity().id == id).forEach(s -> skills.add(s.getId()));
		
		if(id == -1) //-1 for adding
			insert();
		
		entities.add(this);
	}
	
	public int getId(){
		return id;
	}
	
	public String getName(){
		return name;
	}
	
	//without levels
	public float getRawExp(){
		return exp;
	}
	
	//current exp to next level
	public float getExp(){
		return 0;
	}
	
	public int getLevel(){
		return 1;
	}
	
	public boolean isMale(){
		return gender;
	}
	
	public float getStr(){
		return str;
	}
	
	public float getEnd(){
		return end;
	}
	
	public float getDex(){
		return dex;
	}
	
	public float getInt(){
		return intel;
	}
	
	public float getWis(){
		return wis;
	}
	
	public float getCha(){
		return cha;
	}
	
	public float getX(){
		return x;
	}
	
	public float getY(){
		return y;
	}
	
	public String getDescription(){
		return desc;
	}
	
	public Guild getGuild(){
		return guild;
	}
	
	public Tile getTile(){
		return tile;
	}
	
	public Party getParty(){
		return party;
	}
	
	public Player getPlayer(){
		return Player.players.stream().filter(p -> p.getEntity().id == id).findFirst().orElse(null);
	}
	
	public Battle getBattle(){
		return Battle.battles.stream().filter(b -> b.getFightingEntities().contains(id)).findFirst().orElse(null);
	}
	
	public List<Integer> getSkills(){
		return skills;
	}
	
	public float getSkillExp(int skillId){
		if(skills.contains(skillId))
			return ESkill.getSkill(skillId).getExp();
		
		return 0f;
	}
	
	public static Entity getEntity(int id){
		return entities.stream().filter(e -> e.id == id).findFirst().orElse(null);
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
