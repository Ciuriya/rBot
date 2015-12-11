package me.smc.sb.utils;

import java.io.File;
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

public class Log{

	public static Logger logger;
	
	public static void init(String path){
		logger = Logger.getLogger("rBot");
		logger.setLevel(Level.INFO);
		File log = new File(path, "log.txt");
		FileHandler fh = null;
		try{
			fh = new FileHandler(log.getAbsolutePath(), true);
			fh.setFormatter(new LogFormat());
			fh.setEncoding("UTF-8");
		}catch(Exception e){e.printStackTrace();}
		logger.addHandler(fh);	
		logger.log(Level.INFO, "Logger initialized.");
	}
	
	static class LogFormat extends Formatter{
		@Override public String format(LogRecord record){
			DateFormat dateFormat = new SimpleDateFormat("[yyyy/MM/dd HH:mm:ss.SSS]");
			dateFormat.setTimeZone(TimeZone.getDefault());
			String str = dateFormat.format(new Date(record.getMillis()));
			str += " [" + record.getLevel().getName() + "] (" + record.getSourceClassName() + ":" + record.getSourceMethodName() + ") " + "\n" + formatMessage(record) + "\n";
			if(record.getThrown() != null)
				try{
					final StringWriter sw = new StringWriter();
					final PrintWriter pw = new PrintWriter(sw);
					record.getThrown().printStackTrace(pw);
					pw.close();
					str += sw.toString();
				}catch(Exception e){}
			return str + "\n";
		}
	}
	
}
