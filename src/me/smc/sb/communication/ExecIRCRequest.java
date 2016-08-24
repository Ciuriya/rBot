package me.smc.sb.communication;

import me.smc.sb.irccommands.IRCCommand;
import me.smc.sb.utils.Utils;

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
		
		String answer = IRCCommand.handleCommand(null, null, null, msg).replaceAll("\n", "|");
		
		if(answer.length() > 0)
			Utils.info(null, null, null, answer);
	}

}
