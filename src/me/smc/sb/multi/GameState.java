package me.smc.sb.multi;

public enum GameState{

	WAITING, VERIFYING, FIXING, RESIZING, CONFIRMING, PLAYING, PAUSED;
	
	public boolean eq(GameState state){
		return state.name().equalsIgnoreCase(this.name());
	}
	
}
