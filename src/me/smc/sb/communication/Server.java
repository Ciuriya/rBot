package me.smc.sb.communication;

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

import me.smc.sb.irccommands.IRCCommand;
import me.smc.sb.main.Main;
import me.smc.sb.utils.Log;
import me.smc.sb.utils.Utils;
import net.dv8tion.jda.OnlineStatus;
import net.dv8tion.jda.entities.Guild;
import net.dv8tion.jda.entities.User;

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
				
				Guild stomtServer = Main.api.getGuildById("118553122904735745");
				
				if(message.equalsIgnoreCase("REQUEST_ONLINE_USERS")){
					String names = "REQUESTED_USERS:";
					
					for(User user : stomtServer.getUsers())
						if(user != null && 
							user.getOnlineStatus() != null &&
							!user.getOnlineStatus().equals(OnlineStatus.OFFLINE) &&
							!user.getOnlineStatus().equals(OnlineStatus.UNKNOWN))
							names += user.getUsername() + "`" + user.getId() + ":";
					
					sendMessage(names.substring(0, names.length() - 1));
				}else if(message.toUpperCase().startsWith("REQUEST_ID:")){
					String name = message.replace("REQUEST_ID:", "");
					
					User user = null;
					for(User u : stomtServer.getUsers())
						if(u.getUsername().equalsIgnoreCase(name)){
							user = u;
							break;
						}
					
					if(user != null) sendMessage("REQUESTED_ID:" + user.getId());
				}else if(message.toUpperCase().startsWith("REQUEST_NAME:")){
					String id = message.replace("REQUEST_NAME:", "");
					
					User user = null;
					for(User u : stomtServer.getUsers())
						if(u.getId().equalsIgnoreCase(id)){
							user = u;
							break;
						}
					
					if(user != null)
						sendMessage("REQUESTED_NAME:" + user.getUsername());
				}else if(message.toUpperCase().startsWith("EXECIRC")){
					String msg = "";
					String[] args = message.split(" ");
					for(int i = 1; i < args.length; i++)
						msg += " " + args[i];
					msg = msg.substring(1);
					
					System.out.println("Trimmed message: " + msg);
					
					sendMessage(IRCCommand.handleCommand(null, null, null, msg).replaceAll("\n", "|"));
				}else if(message.contains(":")){
					String[] split = message.split(":");
					
					String medium = split[0];
					
					switch(medium.toLowerCase()){
						case "discord":
							if(Main.api.getUserById(split[1]) != null)
								Utils.infoBypass(Main.api.getUserById(split[1]).getPrivateChannel(), "Your verification code is " + split[2]); 	
							break;
						case "osu": 
							String user = split[1].replaceAll(" ", "_");
							try{
								Main.ircBot.sendIRC().joinChannel(user);
							}catch(Exception ex){
								Log.logger.log(Level.INFO, "Could not talk to " + user + "!");
							}
							
							Main.ircBot.sendIRC().message(user, "Your verification code is " + split[2]);
							break;
						default: break;
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
	
	public void sendMessage(String message){
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
