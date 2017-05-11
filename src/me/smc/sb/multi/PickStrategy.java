package me.smc.sb.multi;

public interface PickStrategy{

	public static PickStrategy findStrategy(String name){
		switch(name.toLowerCase()){
			case "nbtb": case "nobacktoback": 
				return new NoBackToBackPickStrategy();
			case "mod": return new ModPickStrategy();
			case "uniquemod": return new UniqueModPickStrategy();
			case "default": case "regular":
			default: return new RegularPickStrategy();
		}
	}
	
	public static String getStrategyName(PickStrategy strategy){
		switch(strategy.getClass().getSimpleName()){
			case "NoBackToBackPickStrategy": return "nbtb";
			case "ModPickStrategy": return "mod";
			case "UniqueModPickStrategy": return "uniquemod";
			case "RegularPickStrategy":
			default: return "regular";
		}
	}
	
	public void handleMapSelect(Game game, String map, boolean select, String mod);

}
