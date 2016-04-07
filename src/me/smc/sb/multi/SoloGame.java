package me.smc.sb.multi;

public class SoloGame extends Game{

	public SoloGame(Match match){
		super(match);
	}

	@Override
	public void initialInvite(){
		Player fPlayer = match.getFirstTeam().getPlayers().get(0);
		Player sPlayer = match.getSecondTeam().getPlayers().get(0);
		captains.add(fPlayer.getName().replaceAll(" ", "_"));
		captains.add(sPlayer.getName().replaceAll(" ", "_"));
		
		fPlayer.setSlot(-1);
		sPlayer.setSlot(-1);
		
		for(String player : captains)
			invitePlayer(player);
	}

	@Override
	public void allowTeamInvites(){}

	@Override
	protected void playerLeft(String message){ //If player leaves for more than a certain time, game lost for him
		
	}

}
