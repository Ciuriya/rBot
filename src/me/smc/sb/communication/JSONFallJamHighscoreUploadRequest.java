package me.smc.sb.communication;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

public class JSONFallJamHighscoreUploadRequest extends IncomingRequest{

	public JSONFallJamHighscoreUploadRequest(){
		super("JSONFALLJAMHIGHSCORE---", "start");
	}
	
	@Override
	public void onRequest(String request){
		String formattedString = "";
		
		if(request.contains("{")){
			String[] splits = request.replace("JSONFALLJAMHIGHSCORE---", "").split("}");
			
			for(int i = 0; i < splits.length; i++)
				if(i < splits.length - 1) formattedString += splits[i] + "}\n";
				else formattedString += splits[i] + "}";
		}else formattedString = request.replace("JSONFALLJAMHIGHSCORE---", "");
		
		write("../../var/www/html/highscores/OnlineFallJamLeaderboard.JSON", formattedString);
	}
	
	private void write(String path, String text){
		BufferedWriter bw = null;
		
		try{
			bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(path), "UTF-8"));
			bw.write(text);
		}catch(Exception e){}
		finally{
			try{
				if(bw != null) bw.close(); 
			}catch(IOException e){e.printStackTrace();}
		}
	}
}
