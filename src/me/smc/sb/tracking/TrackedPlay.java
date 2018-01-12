package me.smc.sb.tracking;

import java.util.List;

import org.json.JSONObject;

import me.smc.sb.tourney.Map;
import me.smc.sb.utils.Utils;

public class TrackedPlay{

	private JSONObject play;
	private JSONObject map = null;
	private PPInfo pp;
	private int mode;
	private int personalBestCount;
	private int mapRank;
	private double playerPPChange;
	private int playerRankChange;
	private int playerCountryRankChange;
	private String country;
	
	public TrackedPlay(JSONObject play, int mode){
		this.play = play;
		this.mode = mode;
		personalBestCount = 0;
		mapRank = 0;
	}
	
	public void loadMap(){
		map = Map.getMapInfo(play.getInt("beatmap_id"), mode, true);
	}
	
	public boolean isMapLoaded(){
		return map != null;
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
	
	public int getRawMode(){
		return mode;
	}
	
	public String getMode(){
		return TrackingUtils.convertMode(mode);
	}
	
	public double getCircleSize(){
		return map.getDouble("diff_size");
	}
	
	public double getOverallDifficulty(){
		return map.getDouble("diff_overall");
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
	
	public boolean hasMapCombo(){
		return !map.isNull("max_combo");
	}
	
	public int getMaxCombo(){
		return map.getInt("max_combo");
	}
	
	public String getRankedStatus(){
		return TrackingUtils.analyzeMapStatus(map.getInt("approved"));
	}
	
	public CustomDate getLastUpdateDate(){
		CustomDate date = new CustomDate(map.getString("last_update"));
		date.convertFromOsuDate();
		
		return date;
	}
	
	public int getTotalLength(){
		return map.getInt("total_length");
	}
	
	public String getFormattedTotalLength(){
		return Utils.toDuration(getTotalLength() * 1000);
	}
	
	public int getDrainLength(){
		return map.getInt("hit_length");
	}

	public String getFormattedDrainLength(){
		return Utils.toDuration(getDrainLength() * 1000);
	}
	
	public String getCreator(){
		return map.getString("creator");
	}
	
	public String getSource(){
		return map.getString("source");
	}
	
	public String getBackground(){
		return "http://b.ppy.sh/thumb/" + map.getInt("beatmapset_id") + "l.jpg";
	}
	
	/*
	 * Play getters
	 */
	
	public String playGet(String key){
		return play.getString(key);
	}
	
	public void playSet(String key, String value){
		play.put(key, value);
	}
	
	public long getRawScore(){
		return play.getLong("score");
	}
	
	public String getScore(){
		return Utils.veryLongNumberDisplay(getRawScore());
	}
	
	public boolean hasCombo(){
		return !play.isNull("maxcombo");
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
	
	public String getFullHitText(){
		switch(mode){
			case 0:
				return (getHundreds() > 0 ? getHundreds() + "x100 " : "") +
		        	   (getFifties() > 0 ? getFifties() + "x50 " : "") + 
		        	   (getMisses() > 0 ? getMisses() + "x miss " : "");
			case 1:
				return (getHundreds() > 0 ? getHundreds() + "x100 " : "") +
	        	   	   (getMisses() > 0 ? getMisses() + "x miss " : "");
			case 2:
				return (getKatu() > 0 ? getKatu() + "x droplets " : "") +
	    	   	   	   (getMisses() > 0 ? getMisses() + "x miss " : "");
			case 3:
				return (getGeki() > 0 ? getGeki() + "xMAX " : "") +
					   (getThreeHundreds() > 0 ? getThreeHundreds() + "x300 " : "") +
					   (getKatu() > 0 ? getKatu() + "x200 " : "") +
	        	   	   (getHundreds() > 0 ? getHundreds() + "x100 " : "") + 
	        	   	   (getFifties() > 0 ? getFifties() + "x50 " : "") + 
	        	   	   (getMisses() > 0 ? getMisses() + "x miss " : "");
			default: return "";
		}
	}
	
	public boolean isPerfect(){
		return play.getInt("perfect") != 0;
	}
	
	public List<Mods> getMods(){
		return Mods.getMods(play.getInt("enabled_mods"));
	}
	
	public int getRawMods(){
		return play.getInt("enabled_mods");
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
	
	public String getFormattedRank(){
		return getRank().replace("X", "SS");
	}
	
	public int getMapRank(){
		return mapRank;
	}
	
	public void setMapRank(int mapRank){
		this.mapRank = mapRank;
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
	
	/*
	 * Player-related getters/setters
	 */
	
	public double getPPChange(){
		return playerPPChange;
	}
	
	public String getFormattedPPChange(){
		String formatted = Utils.df(getPPChange(), 2) + "pp";
		
		if(getPPChange() > 0){
			formatted = "+" + formatted;
		}
		
		return formatted;
	}
	
	public void setPPChange(double ppChange){
		playerPPChange = ppChange;
	}
	
	public int getRankChange(){
		return playerRankChange;
	}
	
	public String getFormattedRankChange(){
		String formatted = Utils.veryLongNumberDisplay(getRankChange());
		
		if(getRankChange() < 0){
			if(formatted.startsWith("-"))
				formatted = formatted.substring(1);
			
			formatted = "+" + formatted;
		}else formatted = "-" + formatted;
		
		return formatted;
	}
	
	public void setRankChange(int rankChange){
		playerRankChange = rankChange;
	}
	
	public int getCountryRankChange(){
		return playerCountryRankChange;
	}
	
	public String getFormattedCountryRankChange(){
		String formatted = Utils.veryLongNumberDisplay(getCountryRankChange());
		
		if(getCountryRankChange() < 0){
			if(formatted.startsWith("-"))
				formatted = formatted.substring(1);
			
			formatted = "+" + formatted;
		}else if(getCountryRankChange() == 0)
			formatted = "0";
		else formatted = "-" + formatted;
		
		return formatted;
	}
	
	public void setCountryRankChange(int countryRankChange){
		playerCountryRankChange = countryRankChange;
	}
	
	public boolean isPersonalBest(){
		return personalBestCount != 0;
	}
	
	public int getPersonalBestCount(){
		return personalBestCount;
	}
	
	public void setPersonalBestCount(int personalBestCount){
		this.personalBestCount = personalBestCount;
	}
	
	public String getCountry(){
		return country;
	}
	
	public void setCountry(String country){
		this.country = country;
	}
	
}
