package me.smc.sb.drpg;

import java.util.ArrayList;
import java.util.List;

public abstract class Quest{

	protected String name;
	protected boolean factionRestricted;
	protected String startText;
	protected String ongoingText;
	protected String endText;
	protected List<QuestStep> steps;
	public static List<Quest> registered = new ArrayList<>();
	
	public Quest(String name, boolean factionRestricted, String startText,
				 String ongoingText, String endText, String objective){
		this.name = name;
		this.factionRestricted = factionRestricted;
		this.startText = startText;
		this.ongoingText = ongoingText;
		this.endText = endText;
		
		steps = new ArrayList<>();
		QuestStep.registered.stream().filter(qs -> qs.questName.equalsIgnoreCase(name)).forEach(qs -> steps.add(qs));
		
		registered.add(this);
	}
	
	public String getName(){
		return name;
	}
	
	public boolean isFactionRestricted(){
		return factionRestricted;
	}
	
	public String getStartText(){
		return startText;
	}
	
	public String getOngoingText(){
		return ongoingText;
	}
	
	public String getEndText(){
		return endText;
	}
	
	public QuestStep getNextStep(int stepNumber){
		return steps.stream().filter(qs -> qs.stepNumber == stepNumber + 1).findFirst().orElse(null);
	}
	
	public static Quest getQuest(String name){
		return registered.stream().filter(q -> q.name.equalsIgnoreCase(name)).findFirst().orElse(null);
	}
	
	//no clue what to do here but I know I'll need some abstracts
	
}
