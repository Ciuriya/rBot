package utils;

import java.util.concurrent.TimeUnit;

/**
 * This class holds various utility functions related to time.
 * 
 * @author Smc
 */
public class TimeUtils {
	
	public static String toDuration(long p_time, boolean p_displayMs){
		long millis = p_time;
		
        long days = TimeUnit.MILLISECONDS.toDays(millis);
        millis -= TimeUnit.DAYS.toMillis(days);
        
        long hours = TimeUnit.MILLISECONDS.toHours(millis);
        millis -= TimeUnit.HOURS.toMillis(hours);
        
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis);
        millis -= TimeUnit.MINUTES.toMillis(minutes);
        
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis);
        millis -= TimeUnit.SECONDS.toMillis(seconds);
        
        String display = "";
        
        if(days > 0) display += days + "d";
        if(hours > 0) display += hours + "h";
        if(minutes > 0) display += minutes + "m";
        if(seconds > 0) display += seconds + "s";
        if(millis > 0 && p_displayMs) display += millis + "ms";
        
        return display;
	}
}
