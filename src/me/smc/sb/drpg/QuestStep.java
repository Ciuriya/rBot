package me.smc.sb.drpg;

import java.util.ArrayList;
import java.util.List;

public abstract class QuestStep{

	protected String questName;
	protected String name;
	protected ObjectiveType objective;
	protected int stepNumber;
	protected String startMessage;
	protected String endMessage;
	protected int questEntity;
	protected int requiredItem;
	protected float x;
	protected float y;
	public static List<QuestStep> registered = new ArrayList<>();
	
	public QuestStep(String questName, String name, String objective, int stepNumber, String startMessage,
			         String endMessage, int questEntityId, int requiredItemId, float x, float y){
		this.questName = questName;
		this.name = name;
		this.objective = ObjectiveType.valueOf(objective.toUpperCase());
		this.stepNumber = stepNumber;
		this.startMessage = startMessage;
		this.endMessage = endMessage;
		this.questEntity = questEntityId;
		this.requiredItem = requiredItemId;
		this.x = x;
		this.y = y;
		
		registered.add(this);
	}
	
	public String getQuestName(){
		return questName;
	}
	
	public String getName(){
		return name;
	}
	
	public ObjectiveType getObjective(){
		return objective;
	}
	
	public int getStepNumber(){
		return stepNumber;
	}
	
	public String getStartMessage(){
		return startMessage;
	}
	
	public String getEndMessage(){
		return endMessage;
	}
	
	public int getQuestEntity(){
		return questEntity;
	}
	
	public int getRequiredItem(){
		return requiredItem;
	}
	
	public float getX(){
		return x;
	}
	
	public float getY(){
		return y;
	}
	
	public static QuestStep getQuestStep(String name){
		return registered.stream().filter(qs -> qs.name.equalsIgnoreCase(name)).findFirst().orElse(null);
	}
	
	public abstract void startStep();
	
	public abstract void isStepComplete();
	
}
