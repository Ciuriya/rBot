package me.smc.sb.charts;

import java.util.ArrayList;
import java.util.List;

import com.googlecode.charts4j.GChart;

public abstract class ChartType{

	public static List<ChartType> types = new ArrayList<>();
	
	private String name;
	
	public ChartType(String name){
		this.name = name;
		
		types.add(this);
	}
	
	public String getName(){
		return name;
	}
	
	public abstract GChart generateChart(String[][] values, int totalVal, String title);
	
	public static ChartType findType(String name){
		for(ChartType type : types)
			if(type.getName().equalsIgnoreCase(name))
				return type;
		
		return null;
	}
	
	public static void load(){
		new PieChart();
	}
}
