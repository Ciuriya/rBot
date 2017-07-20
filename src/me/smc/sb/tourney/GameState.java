package me.smc.sb.tourney;

public enum GameState{
	
	WAITING, ROLLING, SELECTING, BANNING, READYING, VERIFYING, FIXING, CONFIRMING, PLAYING, PAUSED, ENDED;
	
	public boolean eq(GameState state){
		return state.name().equalsIgnoreCase(this.name());
	}
}
