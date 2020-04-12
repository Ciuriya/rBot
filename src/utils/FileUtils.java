package utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import data.Log;

/**
 * This class contains many utility functions related to file handling.
 * 
 * @author Smc
 */
public class FileUtils {
	
	public static List<String> readFileLines(File p_file) {
		List<String> fileLines = new ArrayList<>();
		
		try(BufferedReader br = new BufferedReader(
								new FileReader(p_file, Charset.forName("UTF-8")))) {
			String line;
			
			while((line = br.readLine()) != null)
				fileLines.add(line);
		} catch(Exception e) {
			Log.log(Level.SEVERE, "File reading error: " + p_file.getAbsolutePath(), e);
		}
		
		return fileLines;
	}
	
	public static String readFile(File p_file) {
		List<String> lines = readFileLines(p_file);
		String text = "";
		
		for(String line : lines)
			text += line;
		
		return text;
	}

	// warning: not using append mode will overwrite the file
	public static void writeToFile(File p_file, String p_text, boolean p_append) {
		try(BufferedWriter bw = new BufferedWriter(
								new FileWriter(p_file, Charset.forName("UTF-8"), p_append))) {
			bw.write(p_text);
		} catch(Exception e) {
			Log.log(Level.SEVERE, "File writing error: " + p_file.getAbsolutePath(), e);
		}
	}
}
