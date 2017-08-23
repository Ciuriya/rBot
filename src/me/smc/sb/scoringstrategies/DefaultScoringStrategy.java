package me.smc.sb.scoringstrategies;

import me.smc.sb.tourney.BanchoHandler;
import me.smc.sb.tracking.TrackedPlay;

public class DefaultScoringStrategy implements ScoringStrategy{

	@Override
	public long calculateScore(String player, TrackedPlay play, boolean scorev2, BanchoHandler handle){
		return play.getRawScore();
	}
	
}
