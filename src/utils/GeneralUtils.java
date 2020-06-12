package utils;

/**
 * This class contains general utility functions.
 * 
 * @author Smc
 */
public class GeneralUtils {
	
	public static void sleep(int p_milliseconds) {
		try {
			Thread.sleep(p_milliseconds);
		} catch(InterruptedException e) { }
	}
}
