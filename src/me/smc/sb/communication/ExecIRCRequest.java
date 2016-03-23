package me.smc.sb.communication;

import me.smc.sb.irccommands.IRCCommand;
import me.smc.sb.main.Main;

public class ExecIRCRequest extends IncomingRequest{

	public ExecIRCRequest(){
		super("EXECIRC", "start");
	}

	@Override
	public void onRequest(String request){
		String msg = "";
		String[] args = request.split(" ");
		for(int i = 1; i < args.length; i++)
			msg += " " + args[i];
		msg = msg.substring(1);
		
		Main.server.sendMessage(IRCCommand.handleCommand(null, null, null, msg).replaceAll("\n", "|"));
	}

}
