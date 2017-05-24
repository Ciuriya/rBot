package me.smc.sb.tracking;

import me.smc.sb.utils.Utils;

public class OsuPageRequest extends OsuRequest{
	
	public OsuPageRequest(String... specifics){
		super("page", RequestTypes.HTML, specifics);
	}

	@Override
	public void send(boolean api) throws Exception{
		if(specifics.length < 2){
			answer = "invalid";
			setDone(true);
			return;
		}
		
		String[] pageGeneral = Utils.getHTMLCode("https://osu.ppy.sh/pages/include/profile-" + specifics[0] + ".php" + specifics[1]);
		
		if(pageGeneral.length == 0){
			answer = "invalid";
			setDone(true);
			return;
		}
		
		answer = pageGeneral;
		setDone(true);
	}
}
