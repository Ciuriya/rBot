package me.smc.sb.tracking;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.logging.Level;

import me.smc.sb.utils.Log;
import me.smc.sb.utils.Utils;

public class CustomDate{

	private String date;
	
	public CustomDate(){
		date = Utils.toDate(Utils.getCurrentTimeUTC(), "yyyy-MM-dd HH:mm:ss");
	}
	
	public CustomDate(String date){
		this.date = date;
	}
	
	public String getDate(){
		return date;
	}
	
	public int getYear(){
		return Utils.stringToInt(date.split("-")[0]);
	}
	
	public int getMonth(){
		return Utils.stringToInt(date.split("-")[1]);
	}
	
	public int getDay(){
		return Utils.stringToInt(date.split("-")[2].split(" ")[0]);
	}
	
	public int getHour(){
		return Utils.stringToInt(date.split(":")[0].split(" ")[1]);
	}
	
	public int getMinute(){
		return Utils.stringToInt(date.split(":")[1]);
	}
	
	public int getSecond(){
		return Utils.stringToInt(date.split(":")[2]);
	}
	
	public int getValue(int index){
		switch(index){
			case 0: return getYear();
			case 1: return getMonth();
			case 2: return getDay();
			case 3: return getHour();
			case 4: return getMinute();
			case 5: return getSecond();
			default: return 0;
		}
	}
	
	public void convertFromOsuDate(){
		DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		formatter.setTimeZone(TimeZone.getTimeZone("Asia/Singapore"));
		
		Date oDate = null;
		
		try{
			oDate = formatter.parse(date);
		}catch(ParseException e){
			Log.logger.log(Level.SEVERE, e.getMessage(), e);
		}
		
		formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
		date = formatter.format(oDate);
	}
	
	public boolean add(int seconds){
		DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		
		try{
			date = formatter.format(new Date(formatter.parse(date).getTime() + (seconds * 1000)));
			return true;
		}catch (ParseException e1){
			return false;
		}
	}
	
	public boolean after(CustomDate oDate){
		return after(oDate, 0);
	}
	
	public boolean after(CustomDate oDate, int index){
		int localVal = getValue(index);
		int otherVal = oDate.getValue(index);
		
		if(localVal > otherVal || index == 5)
			return true;
		else if(localVal == otherVal)
			return after(oDate, index + 1);
		
		return false;
	}
}
