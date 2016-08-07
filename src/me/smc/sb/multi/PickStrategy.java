package me.smc.sb.multi;

public interface PickStrategy{

	public static PickStrategy findStrategy(String name){
		switch(name.toLowerCase()){
			case "mod": return new ModPickStrategy();
			case "default": case "regular":
			default: return new RegularPickStrategy();
		}
	}
	
	public static String getStrategyName(PickStrategy strategy){
		switch(strategy.getClass().getSimpleName()){
			case "ModPickStrategy": return "mod";
			case "RegularPickStrategy":
			default: return "regular";
		}
	}
	
	public void handleMapSelect(Game game, String map, boolean select, String mod);

}
