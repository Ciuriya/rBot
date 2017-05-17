package me.smc.sb.charts;

public class ChartGenerator{

	private String apiKey;
	
	public ChartGenerator(String apiKey){
		this.apiKey = apiKey;
	}
	
	public String getAPIKey(){
		return apiKey;
	}
	
	public String generateChart(String type, String title, int total, String[][] values){
		if(total == 0) return "";
		
		ChartType chartType = ChartType.findType(type);
		
		if(chartType != null){
			return chartType.generateChart(values, total, title).toURLString();
		}
		
		return "";
	}
	
}
