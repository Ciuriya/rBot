package me.smc.sb.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Configuration{

	private File configFile;
	private List<String> configLines;
	
	public Configuration(String path, String fileName){
		this(new File(path, fileName));
	}
	
	public Configuration(File file){
		configFile = file;
		try{
			if(!configFile.exists()) configFile.createNewFile();
			configLines = readSmallTextFile(configFile);
		}catch(IOException e){e.printStackTrace();}
	}
	
	public void delete(){
		configLines.clear();
		configFile.delete();
	}
	
	public void deleteKey(String key){
		PrintWriter pw = null;
		try{
			pw = new PrintWriter(new BufferedWriter(new FileWriter(configFile, false)));
			boolean started = false;
			if(configLines != null)
				for(String str : new ArrayList<String>(configLines))
					if(str.startsWith(key + ":list")){
						started = true;
						configLines.remove(str);
					}else if(started && str.startsWith("- ")) configLines.remove(str);
					else if(str.startsWith(key + ":")){
						configLines.remove(str);
						started = false;
					}else{
						pw.println(str);
						started = false;
					}
		}catch(Exception e){e.printStackTrace();}
		finally{
			if(pw != null) pw.close();
			try{configLines = readSmallTextFile(configFile);
			}catch(IOException e){e.printStackTrace();}
		}
	}
	
	public List<String> getLines(){
		return configLines;
	}
	
	public String getFileName(){
		return configFile.getName().replace(".txt", "");
	}
	
	public boolean getBoolean(String key){
		try{
			String value = getValue(key);
			if(value == "") return false;
			return Boolean.parseBoolean(getValue(key));
		}catch(Exception e){
			return false;
		}
	}
	
	public int getInt(String key){
		try{return Integer.parseInt(getValue(key));}catch(Exception e){return 0;}
	}
	
	public long getLong(String key){
		try{return Long.parseLong(getValue(key));}catch(Exception e){return 0;}
	}
	
	public double getDouble(String key){
		try{return Double.parseDouble(getValue(key));}catch(Exception e){return 0;}
	}
	
	public ArrayList<String> getStringList(String key){
		ArrayList<String> list = new ArrayList<String>();
		boolean started = false;
		if(configLines != null)
			for(String str : configLines){
				if(str.equalsIgnoreCase(key + ":list")) started = true;
				else if(started && str.startsWith("- ")) list.add(str.substring(2, str.length()));
				else started = false;
			}
		return list;
	}
	
	public void removeFromStringList(String key, String val, boolean sort){
		ArrayList<String> list = getStringList(key), refilled = new ArrayList<String>();
		for(String str : list) if(!str.equalsIgnoreCase(val)) refilled.add(str);
		writeStringList(key, refilled, sort);
	}
	
	
	public String getValue(String key){
		if(configLines != null)
			for(String str : configLines)
				if(str.startsWith(key + ":"))
					return str.substring((key + ":").length());
		return "";
	}
	
	public void writeStringList(String key, ArrayList<String> list, boolean sort){
		PrintWriter pw = null;
		if(sort) Collections.sort(list);
		try{
			pw = new PrintWriter(new BufferedWriter(new FileWriter(configFile, false)));
			boolean started = false;
			if(configLines != null)
				for(String str : configLines)
					if(str.startsWith(key + ":list"))
						started = true;
					else if(started && str.startsWith("- ")){
						//do nothing
					}else{
						started = false;
						pw.println(str);
					}
			pw.println(key + ":list");
			if(list.size() > 0)
				for(String str : list)
					pw.println("- " + str);
		}catch(Exception e){e.printStackTrace();}
		finally{
			if(pw != null) pw.close();
			try{configLines = readSmallTextFile(configFile);
			}catch(IOException e){e.printStackTrace();}
		}
	}
	
	public void appendToStringList(String key, String val, boolean sort){
		ArrayList<String> list = getStringList(key);
		list.add(val);
		writeStringList(key, list, sort);
	}
	
	public void writeValue(String key, Object object){
		String value = object == null ? "" : object.toString();
		BufferedWriter bw = null;
		try{
			bw = new BufferedWriter(new OutputStreamWriter(
				    new FileOutputStream(configFile.getName()), "UTF-8"));
			boolean written = false;
			if(configLines != null)
				for(String str : configLines)
					if(str.startsWith(key + ":")){
						bw.write(key + ":" + (value == null ? "" : value));
						bw.newLine();
						written = true;
					}else{
						bw.write(str);
						bw.newLine();
					}
			if(!written){
				bw.write(key + ":" + (value == null ? "" : value));
				bw.newLine();
			}
		}catch(Exception e){}
		finally{
			try{
				if(bw != null) bw.close(); 
				configLines = readSmallTextFile(configFile);
			}catch(IOException e){e.printStackTrace();}
		}
	}
	
	public static List<String> readSmallTextFile(final File configFile) throws IOException{
		//BufferedReader bufferedReader = new BufferedReader(new FileReader(configFile));
		BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(configFile.getName()), "UTF-8"));
		List<String> fileLines = null;
		try{
			String line = bufferedReader.readLine();
			fileLines = new ArrayList<String>();
			while(line != null){
				fileLines.add(line);
				line = bufferedReader.readLine();
			}
		}catch(IOException e){e.printStackTrace();}
		finally{bufferedReader.close();}
		return fileLines;
	}
	
}