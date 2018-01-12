package me.smc.sb.tracking;

import java.util.List;

import me.smc.sb.utils.Utils;

public class OsuLastActiveRequest extends OsuRequest{
	
	public OsuLastActiveRequest(String... specifics){
		super("last-active", RequestTypes.HTML, specifics);
	}

	@Override
	public void send(boolean api) throws Exception{
		if(specifics.length < 1){
			answer = "invalid";
			setDone(true);
			return;
		}

		String[] page = Utils.getHTMLCode("https://osu.ppy.sh/u/" + specifics[0]);
		
		if(page.length == 0){
			answer = "failed";
			setDone(true);
			return;
		}
		
		List<String> activeLine = Utils.getNextLineCodeFromLink(page, 0, "Last Active");
		
		if(activeLine.size() == 0){
			answer = "failed";
			setDone(true);
			return;
		}
		
		answer = activeLine.get(0).split(" UTC")[0].split("Z'>")[1];
		setDone(true);
	}
}
