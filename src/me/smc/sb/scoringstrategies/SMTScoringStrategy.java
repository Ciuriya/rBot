package me.smc.sb.scoringstrategies;

import java.util.List;

import org.apache.commons.math3.fitting.WeightedObservedPoint;
import org.apache.commons.math3.fitting.WeightedObservedPoints;

import me.smc.sb.main.Main;
import me.smc.sb.tourney.BanchoHandler;
import me.smc.sb.tourney.ReadyManager;
import me.smc.sb.tracking.Mods;
import me.smc.sb.tracking.TrackedPlay;
import me.smc.sb.utils.Utils;

public class SMTScoringStrategy implements ScoringStrategy{

	@Override
	public long calculateScore(String player, TrackedPlay play, boolean scorev2, BanchoHandler handle){
		if(play.getRawMode() == 0 && !scorev2){
			List<String> osuFile = fetchOsuFile(play.getBeatmapId());
			
			play.loadMap();
			
			int mapCombo = play.getMaxCombo();
			int maxCombo = play.getCombo();
			double comboScore = 0;
			double accScore = 0;
			double fcPercentage = (double) maxCombo / (double) mapCombo;
			double mainFCPercentage = fcPercentage;
			double residualFCPercentage = 0;
			double missPenalty = 0;
			double closestCombo = 0;
			double closestScore = 0;
			// 1k = 1.0047x 1.5k = 1.0518x 2k = 1.10318x 2.5k = 1.157835x 5k = 1.46729x
			double accLengthModifier = mapCombo >= 1000 ? (Math.pow(mapCombo / 150.0, 1.25) / 10 - 1) / 15 + 1 : 1; 

			List<Mods> mods = play.getMods();
			WeightedObservedPoints obs = new WeightedObservedPoints();
			
			long mainComboScore = (long) (estimateScore(osuFile, play, obs, mods, scorev2) / 1.1);
			long restOfScore = play.getRawScore() - mainComboScore;
			
			if(mainComboScore <= play.getRawScore() && mapCombo - maxCombo > 10){
				for(WeightedObservedPoint p : obs.toList())
					if(p.getY() > closestScore && p.getY() <= restOfScore){
						closestCombo = p.getX();
						closestScore = p.getY();
					}
				
				residualFCPercentage = (double) closestCombo / 2 / (double) mapCombo;
				fcPercentage += residualFCPercentage;
			}
			
			if(play.getMisses() > 0){
				missPenalty = Math.log(play.getMisses() + 1) / 20;
				
				if(missPenalty > 0.2) missPenalty = 0.2;
				
				fcPercentage -= missPenalty;
			}
			
			if(fcPercentage > 1) fcPercentage = 1;
			if(fcPercentage < 0) fcPercentage = 0;
			
			comboScore = fcPercentage * 100;
			
			double acc = play.getAccuracy();
			double od = calculateOverallDiff(mods, play.getOverallDifficulty());
			double accModifier = Math.pow(od, 2) / 50;
			
			if(accModifier < 1) accModifier = 1;
			
			accModifier *= accLengthModifier;
			if(acc >= 94) accScore = (Math.pow(acc - 90, 1.7) * 1.5 * accModifier) / 1.3 * 50;
			else if(acc < 94) accScore = Math.pow(acc / 100 + 1, 10) / 2 * accModifier; // 80 = 178.5, 90 = 306.55, 92 = 340.39, 93 = 358.54 94 = 377.56
			
			comboScore *= 50;
			
			play.playSet("acc_score", accScore + "");
			play.playSet("combo_score", comboScore + "");
			
			long score = Math.round(comboScore + accScore);
			
			if(handle != null && player != null){
				String message = player + " scored " + Utils.veryLongNumberDisplay(score) + 
								 " | Combo: " + Utils.veryLongNumberDisplay(Utils.df(comboScore)) + 
								 " (Main: " + Utils.veryLongNumberDisplay(Utils.df(mainFCPercentage * 5000)) + 
								 ", Rest: " + Utils.veryLongNumberDisplay(Utils.df(residualFCPercentage * 5000)) + 
								 ", Miss: -" + Utils.veryLongNumberDisplay(Utils.df(missPenalty * 5000)) +
								 ") | Accuracy: " + Utils.df(accScore) + " (" + Utils.df(accModifier) + "x)";
				handle.sendMessage(message, false);
				Utils.info(Main.api.getGuildById("118553122904735745").getTextChannelById("392791710461329419"), 
						   "https://osu.ppy.sh/beatmaps/" + play.getBeatmapId() + "\n" + message);
				
			}
			
			return score;
			
		}
		
		return play.getRawScore();	
	}
	
	private double calculateModMultiplier(List<Mods> mods, boolean scorev2){
		double multiplier = 1.0;
		
		for(Mods mod : mods)
			multiplier *= ReadyManager.getModMultiplier(mod.getShortName(), scorev2);
		
		return multiplier;
	}
	
	// only estimating for the biggest combo for now
	private long estimateScore(List<String> osuFile, TrackedPlay play, WeightedObservedPoints obs, List<Mods> mods, boolean scorev2){
		long score = 0;
		boolean start = false;
		int combo = 0;
		int sliderCount = 0;
		int objectCount = 0;
		int extraSliderCombo = 0;
		double diffPoints = play.getCircleSize() + play.getOverallDifficulty() + play.getHPDrain();
		int diffMultiplier = 2;
		double modMultiplier = calculateModMultiplier(mods, scorev2);
		
		if(diffPoints >= 6 && diffPoints <= 12) diffMultiplier = 3;
		else if(diffPoints >= 13 && diffPoints <= 17) diffMultiplier = 4;
		else if(diffPoints >= 18 && diffPoints <= 24) diffMultiplier = 5;
		else if(diffPoints >= 25 && diffPoints <= 30) diffMultiplier = 6;
		
		for(String line : osuFile){
			if(line.startsWith("[HitObjects]")) start = true;
			else if(start && line.length() > 2){
				objectCount++;
				
				int type = Integer.parseInt(line.split(",")[3]); // 1 = circle, 2 = slider, 8 = spinner
				
				while(type - 16 > 0) type -= 16;
				
				if(type - 4 > 0) type -= 4;
				
				if(type == 2) sliderCount++;
			}
		}
		
		start = false;
		extraSliderCombo = play.getMaxCombo() - objectCount;
		
		double extraComboRatio = (double) extraSliderCombo / (double) sliderCount;
		double ratio100 = (double) objectCount / (double) play.getHundreds();
		int objectsSinceLast100 = 0;
		
		for(String line : osuFile){
			if(line.startsWith("[HitObjects]")) start = true;
			else if(start && line.length() > 2){
				long objectScore = 0;
				int type = Integer.parseInt(line.split(",")[3]); // 1 = circle, 2 = slider, 8 = spinner
				
				while(type - 16 > 0) type -= 16;
				
				if(type - 4 > 0) type -= 4;
				
				if(type == 2){
					int repeats = Integer.parseInt(line.split(",")[6]) - 1;
					int extraCombo = (int) Math.ceil(extraComboRatio);
					
					combo += extraCombo;
					extraSliderCombo -= extraCombo;
					sliderCount--;
					extraComboRatio = (double) extraSliderCombo / (double) sliderCount;
					objectScore += 60 + repeats * 30 + extraCombo * 10; // start, end and repeat give 30
				}
				
				if(type == 8){
					double spinnerLength = ((double) Integer.parseInt(line.split(",")[5]) - (double) Integer.parseInt(line.split(",")[2])) / 1000.0; // seconds
					int spins = (int) Math.ceil(spinnerLength * 6.6666666666); // 6.66666 is roughly 400spm
					
					objectScore += ((spins / 2) * 100) + ((spins / 2) * 1000);
				}
				
				int baseScore = 300;
				
				if(objectsSinceLast100 > ratio100){
					objectsSinceLast100 = 0;
					baseScore = 100;
				}
				
				objectScore += baseScore + (baseScore * (((combo == 0 ? 0 : combo - 1) * diffMultiplier * modMultiplier) / 25));
				
				if(baseScore != 100) objectsSinceLast100++;
				
				score += objectScore;
				
				obs.add(combo, score);
				
				combo++;
				
				if(combo >= play.getCombo()){
					combo = 0;
					
					return score;
				}
			}
		}
		
		return score;
	}
	
	// reference: https://github.com/Tillerino/osuApiConnector/blob/master/osuApiConnector/src/main/java/org/tillerino/osuApiModel/OsuApiBeatmap.java
	public double calculateOverallDiff(List<Mods> mods, double originalOD){
		double od = originalOD;
		
		for(Mods mod : mods){
			switch(mod){
				case HardRock: 
					od *= 1.4; 
					if(od > 10) od = 10;
					break;
				case Easy: od *= 0.5; break;
				case DoubleTime: case Nightcore:
					od = msToOd(odToMs(od) * 2 / 3);
				case HalfTime:
					od = msToOd(odToMs(od) * 4 / 3);
				default: break;
			}
		}
		
		return od;
	}
	
	private double odToMs(double od){
		return 79.5 - Math.ceil(6 * od);
	}
	
	private double msToOd(double ms){
		return (79.5 - ms) / 6;
	}
}
