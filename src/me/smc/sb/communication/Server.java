package me.smc.sb.communication;

import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;

import me.smc.sb.utils.Log;

public class Server{

	private String ip;
	private int portReceive, portSend;
	private boolean stop = false;
	
	public Server(String ip, int portReceive, int portSend){
		this.ip = ip;
		this.portReceive = portReceive;
		this.portSend = portSend;
		
		waitForMessage();
	}
	
	private void waitForMessage(){
		Timer timer = new Timer();
		
		Thread t = new Thread(new Runnable(){
			public void run(){
				ServerSocket serverSocket = null;
				Socket workerSocket = null;
				DataInputStream socketInputStream;
				String message = "";
				
				try{
					if(serverSocket == null){
						serverSocket = new ServerSocket(portReceive, 4);
						
						Log.logger.log(Level.INFO, "Awaiting connections on port " + portReceive + "...");
						
						workerSocket = serverSocket.accept();
					}
					
					socketInputStream = new DataInputStream(workerSocket.getInputStream());
					byte[] rMsgByte = new byte[socketInputStream.available()];
					
					for(int i = 0; i < rMsgByte.length; i++) rMsgByte[i] = socketInputStream.readByte();
					
					message = new String(rMsgByte);
					
					Log.logger.log(Level.INFO, "Received message: " + message);
				}catch(Exception e){
					Log.logger.log(Level.SEVERE, e.getMessage(), e);
				}finally{		
					try{
						serverSocket.close();
						workerSocket.close();
					}catch(IOException e){
						Log.logger.log(Level.SEVERE, e.getMessage());
					}
				}
				
				IncomingRequest.handleRequest(message);
				
				Timer t = new Timer();
				t.schedule(new TimerTask(){
					@SuppressWarnings("deprecation")
					public void run(){
						timer.cancel();
						Thread.currentThread().stop();
					}
				}, 10);
				
				waitForMessage();
			}
		});
		
		timer.scheduleAtFixedRate(new TimerTask(){
			@SuppressWarnings("deprecation")
			public void run(){
				if(stop){
					t.stop();
					timer.cancel();
				}
			}
		}, 100, 100);
		
		t.start();
	}
	
	public void sendMessage(String message){
		sendMessage(ip, portSend, message);
	}
	
	public static void sendMessage(String ip, int portSend, String message){
		Socket clientSocket = null;
		OutputStream os = null;
		OutputStreamWriter osw = null;
		BufferedWriter bw = null;
		
		try{
			clientSocket = new Socket(ip, portSend);
	        os = clientSocket.getOutputStream();
	        osw = new OutputStreamWriter(os);
	        bw = new BufferedWriter(osw);
	        bw.write(message);
	        bw.flush();
		}catch(Exception e){
			Log.logger.log(Level.SEVERE, "Could not send message: " + message + "\n" + e.getMessage(), e);
		}finally{
	        try{
				if(bw != null) bw.close();
		        if(osw != null) osw.close();
		        if(os != null) os.close();
		        if(clientSocket != null) clientSocket.close();
			}catch(IOException e){
				Log.logger.log(Level.SEVERE, e.getMessage(), e);
			}
		}
	}
	
	public void stop(){
		stop = true;
	}
	
}
