package me.smc.sb.tourney;

public enum GameState{
	
	WAITING, ROLLING, SELECTING, BANNING, READYING, PRESTART, VERIFYING, PLAYING, CONFIRMING, PAUSED, ENDED;
	
	public boolean eq(GameState state){
		return state.name().equalsIgnoreCase(this.name());
	}
}
