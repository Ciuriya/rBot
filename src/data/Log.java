package data;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import utils.Constants;

/**
 * This class handles writing the bot's output in files to keep a more permanent copy.
 * This also allows for later review of errors and such.
 * 
 * @author Smc
 */
public class Log {
	
	private static Logger infoLogger; // logs Level.INFO, Level.WARNING and Level.SEVERE
	private static Logger warningLogger; // logs Level.WARNING and Level.SEVERE
	private static Logger severeLogger; // logs Level.SEVERE

	public static void init(String p_path) {
		infoLogger = setupLogger("rBot-info", Level.INFO, "Logs/Info/log-info-%g.txt", true);
		warningLogger = setupLogger("rBot-warning", Level.WARNING, 
									"Logs/Warning/log-warning-%g.txt", false);
		severeLogger = setupLogger("rBot-severe", Level.SEVERE, 
								   "Logs/Severe/log-severe-%g.txt", false);
		
		log(Level.INFO, "Logger initialized.");
	}
	
	public static void log(Level p_level, String p_message) {
		log(p_level, p_message, null);
	}
	
	public static void log(Level p_level, String p_message, Throwable p_thrown) {
		LogRecord record = new LogRecord(p_level, p_message);
		
		if(p_thrown != null) record.setThrown(p_thrown);
		
		infoLogger.log(record);
		warningLogger.log(record);
		severeLogger.log(record);
	}
	
	private static Logger setupLogger(String p_loggerName, Level p_level, String p_file, 
									  boolean p_logInConsole) {
		Logger logger = Logger.getLogger(p_loggerName);

		logger.setLevel(p_level);
		
		if(!p_logInConsole) logger.setUseParentHandlers(false);
		
		try {
			// the file handler rotates through files one-by-one as they hit MAX_LOG_SIZE
			FileHandler handler = new FileHandler(p_file, Constants.MAX_LOG_SIZE, 50, true);
			
			handler.setFormatter(new LogFormat());
			handler.setEncoding("UTF-8");
			
			logger.addHandler(handler);
		} catch(Exception e) {
			e.printStackTrace();
		}
		
		return logger;
	}
}

/**
 * A local class that formats logger output to show useful information 
 * like timestamp and source class/function.
 * 
 * @author Smc
 */
class LogFormat extends Formatter {

	@Override
	public String format(LogRecord p_record) {
		DateFormat format = new SimpleDateFormat("[yyyy/MM/dd HH:mm:ss.SSS");
		
		format.setTimeZone(TimeZone.getDefault());
		
		String message = format.format(new Date(p_record.getMillis()));
		
		message += " [" + p_record.getLevel().getName() + "] ";
		message += "(" + p_record.getSourceClassName() + ":" + p_record.getSourceMethodName() + ")\n";
		message += formatMessage(p_record) + "\n";
		
		// if an exception is associated with this, we want to print it as well
		if(p_record.getThrown() != null) {
			final StringWriter sw = new StringWriter();
			final PrintWriter pw = new PrintWriter(sw);
			
			p_record.getThrown().printStackTrace(pw);
			
			pw.close();
			
			message += sw.toString();
		}
		
		return message + "\n";
	}
}