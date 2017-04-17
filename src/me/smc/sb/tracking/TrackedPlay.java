package me.smc.sb.tracking;

import java.util.List;

import org.json.JSONObject;

import me.smc.sb.multi.Map;
import me.smc.sb.utils.Utils;

public class TrackedPlay{

	private JSONObject play;
	private JSONObject map;
	private PPInfo pp;
	private int mode;
	
	public TrackedPlay(JSONObject play, int mode){
		this.play = play;
		this.mode = mode;
		
		map = Map.getMapInfo(play.getInt("beatmap_id"), mode, false);
	}
	
	/*
	 * Beatmap getters
	 */
	
	public int getBeatmapId(){
		return play.getInt("beatmap_id");
	}
	
	public int getBeatmapSetId(){
		return map.getInt("beatmapset_id");
	}
	
	public String getArtist(){
		return map.getString("artist");
	}
	
	public String getTitle(){
		return map.getString("title");
	}
	
	public String getDifficulty(){
		return map.getString("version");
	}
	
	public String getFormattedTitle(){
		return TrackingUtils.escapeCharacters(getArtist() + " - " + getTitle() + " [" + getDifficulty() + "]");
	}
	
	public int getMode(){
		return mode;
	}
	
	public double getCircleSize(){
		return map.getDouble("diff_size");
	}
	
	public double getOverallDifficulty(){
		return map.getInt("diff_overall");
	}
	
	public double getApproachRate(){
		return map.getDouble("diff_approach");
	}
	
	public double getHPDrain(){
		return map.getDouble("diff_drain");
	}
	
	public double getStarRating(){
		return map.getDouble("difficultyrating");
	}
	
	public double getBPM(){
		return map.getDouble("bpm");
	}
	
	public int getMaxCombo(){
		return map.getInt("max_combo");
	}
	
	public String getRankedStatus(){
		return TrackingUtils.analyzeMapStatus(map.getInt("approved"));
	}
	
	public int getTotalLength(){
		return map.getInt("total_length");
	}
	
	public String getFormattedTotalLength(){
		return Utils.toDuration(getTotalLength());
	}
	
	public int getDrainLength(){
		return map.getInt("hit_length");
	}

	public String getFormattedDrainLength(){
		return Utils.toDuration(getDrainLength());
	}
	
	public String getCreator(){
		return map.getString("creator");
	}
	
	public String getSource(){
		return map.getString("source");
	}
	
	/*
	 * Play getters
	 */
	
	public long getScore(){
		return play.getLong("score");
	}
	
	public int getCombo(){
		return play.getInt("maxcombo");
	}
	
	public double getAccuracy(){
		return TrackingUtils.getAccuracy(play, mode);
	}
	
	public int getFifties(){
		return play.getInt("count50");
	}
	
	public int getHundreds(){
		return play.getInt("count100");
	}
	
	public int getThreeHundreds(){
		return play.getInt("count300");
	}
	
	public int getMisses(){
		return play.getInt("countmiss");
	}
	
	// the 100s one
	public int getKatu(){
		return play.getInt("countkatu");
	}
	
	public int getGeki(){
		return play.getInt("countgeki");
	}
	
	public boolean isPerfect(){
		return play.getInt("perfect") != 0;
	}
	
	public List<Mods> getMods(){
		return Mods.getMods(play.getInt("enabled_mods"));
	}
	
	public String getModDisplay(){
		return Mods.getModDisplay(getMods());
	}
	
	public CustomDate getDate(){
		return new CustomDate(TrackingUtils.osuDateToCurrentDate(play.getString("date")));
	}
	
	public String getRank(){
		return play.getString("rank");
	}
	
	public void setPPInfo(PPInfo info){
		pp = info;
	}
	
	public double getPP(){
		return pp.getPP();
	}
	
	public double getPPForFC(){
		return pp.getPPForFC();
	}
	
}
