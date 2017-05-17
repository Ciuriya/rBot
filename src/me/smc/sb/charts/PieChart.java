package me.smc.sb.charts;

import java.util.ArrayList;
import java.util.List;

import com.googlecode.charts4j.Color;
import com.googlecode.charts4j.Fills;
import com.googlecode.charts4j.GChart;
import com.googlecode.charts4j.GCharts;
import com.googlecode.charts4j.LegendPosition;
import com.googlecode.charts4j.Slice;

import me.smc.sb.utils.Utils;

public class PieChart extends ChartType{

	public PieChart(){
		super("pie");
	}

	public GChart generateChart(String[][] values, int totalVal, String title){
		List<Slice> slices = new ArrayList<>();
		
		for(int i = 0; i < values.length; i++){
			int val = Utils.stringToInt(values[i][1]);
			double percent = ((double) val / (double) totalVal) * 100;
			
			if(val > 0) slices.add(Slice.newSlice((int) Math.ceil(percent), Color.newColor(Utils.getRandomHexColor().substring(1)), values[i][0]));
		}
		
		com.googlecode.charts4j.PieChart chart = GCharts.newPieChart(slices);
		chart.setTitle(title, Color.WHITE, 36);
		chart.setSize(512, 368);
		chart.setBackgroundFill(Fills.newSolidFill(Color.newColor(Color.WHITE, 0)));
		chart.setLegendPosition(LegendPosition.BOTTOM);
		
		return chart;
	}
	
}
