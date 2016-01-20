package me.smc.sb.utils;

import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;

import me.itsghost.jdiscord.OnlineStatus;
import me.itsghost.jdiscord.talkable.GroupUser;
import me.smc.sb.main.Main;

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
				PrintWriter out = null;
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
					if(out != null) out.close();
					
					try{
						serverSocket.close();
						workerSocket.close();
					}catch(IOException e){
						Log.logger.log(Level.SEVERE, e.getMessage());
					}
				}
				
				me.itsghost.jdiscord.Server stomtServer = Main.api.getServerById("118553122904735745");
				
				if(message.equalsIgnoreCase("REQUEST_ONLINE_USERS")){
					String names = "REQUESTED_USERS:";
					
					for(GroupUser user : stomtServer.getConnectedClients())
						if(user != null && 
							user.getUser() != null && 
							user.getUser().getOnlineStatus() != null &&
							!user.getUser().getOnlineStatus().equals(OnlineStatus.OFFLINE) &&
							!user.getUser().getOnlineStatus().equals(OnlineStatus.UNKNOWN))
							names += user.getUser().getUsername() + "`" + user.getUser().getId() + ":";
					
					sendMessage(names.substring(0, names.length() - 1));
				}else if(message.startsWith("REQUEST_ID:")){
					String name = message.replace("REQUEST_ID:", "");
					
					if(stomtServer.getGroupUserByUsername(name) != null)
						sendMessage("REQUESTED_ID:" + stomtServer.getGroupUserByUsername(name).getUser().getId());
				}else if(message.startsWith("REQUEST_NAME:")){
					String id = message.replace("REQUEST_NAME:", "");
					
					if(stomtServer.getGroupUserById(id) != null)
						sendMessage("REQUESTED_NAME:" + stomtServer.getGroupUserById(id).getUser().getUsername());
				}else if(message.contains(":")){
					String[] split = message.split(":");
					
					if(Main.api.getUserById(split[0]) != null){
						Utils.infoBypass(Main.api.getUserById(split[0]).getGroup(), "Your verification code is " + split[1]); 	
					}
				}
				
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
	
	private void sendMessage(String message){
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
			//Utils.infoBypass(Main.api.getUserById("77631618088435712").getGroup(), e.getMessage());
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
