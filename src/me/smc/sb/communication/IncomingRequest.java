package me.smc.sb.communication;

import java.util.ArrayList;
import java.util.List;

public abstract class IncomingRequest{

	private String syntax;
	private String operation; //eq, cont, start, end
	private static List<IncomingRequest> incomingRequests;
	
	public IncomingRequest(String syntax, String operation){
		this.syntax = syntax.toLowerCase();
		this.operation = operation;
	}
	
	public boolean checkSyntax(String request){
		String localRequest = request.toLowerCase();
		
		switch(operation){
			case "eq": return localRequest.equalsIgnoreCase(syntax);
			case "cont": return localRequest.contains(syntax);
			case "start": return localRequest.startsWith(syntax);
			case "end": return localRequest.endsWith(syntax);
			default: return false;
		}
	}
	
	public static void handleRequest(String request){
		for(IncomingRequest ir : incomingRequests)
			if(ir.checkSyntax(request)){
				ir.onRequest(request);
				break;
			}
	}
	
	public static void registerRequests(){
		incomingRequests = new ArrayList<>();
		incomingRequests.add(new DiscordIDRequest());
		incomingRequests.add(new DiscordNameRequest());
		incomingRequests.add(new DiscordOnlineUsersRequest());
		incomingRequests.add(new ExecIRCRequest());
		incomingRequests.add(new VerifyUserRequest());
	}
	
	public abstract void onRequest(String request);
	
}
