package me.smc.sb.tracking;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.List;
import java.util.logging.Level;

import org.json.JSONObject;

import com.github.francesco149.koohii.Koohii;

import me.smc.sb.tourney.Map;
import me.smc.sb.utils.Log;
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
	private int backupMapId;
	private String country;
	private MapStats updatedStats;
	
	public TrackedPlay(JSONObject play, int mode){
		this.play = play;
		this.mode = mode;
		personalBestCount = 0;
		mapRank = 0;
		updatedStats = null;
		backupMapId = 0;
	}
	
	public void loadMap(){
		map = Map.getMapInfo(play.getInt("beatmap_id"), mode, true);
	}
	
	public void loadMap(int beatmapId){
		map = Map.getMapInfo(beatmapId, mode, true);
		backupMapId = beatmapId;
	}
	
	public boolean isMapLoaded(){
		return map != null;
	}
	
	/*
	 * Beatmap getters
	 */
	
	public int getBeatmapId(){
		return backupMapId != 0 ? backupMapId : play.getInt("beatmap_id");
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
	
	public double getBaseCircleSize(){
		return map.getDouble("diff_size");
	}
	
	public double getCircleSize(){
		return updatedStats != null ? updatedStats.cs : map.getDouble("diff_size");
	}
	
	public double getBaseOverallDifficulty(){
		return map.getDouble("diff_overall");
	}
	
	public double getOverallDifficulty(){
		return updatedStats != null ? updatedStats.od : map.getDouble("diff_overall");
	}
	
	public double getBaseApproachRate(){
		return map.getDouble("diff_approach");
	}
	
	public double getApproachRate(){
		return updatedStats != null ? updatedStats.ar : map.getDouble("diff_approach");
	}
	
	public double getBaseHPDrain(){
		return map.getDouble("diff_drain");
	}
	
	public double getHPDrain(){
		return updatedStats != null ? updatedStats.hp : map.getDouble("diff_drain");
	}
	
	public double getBaseStarRating(){
		return map.getDouble("difficultyrating");
	}
	
	public double getStarRating(){
		return updatedStats != null ? updatedStats.stars : map.getDouble("difficultyrating");
	}
	
	public double getBaseBPM(){
		return map.getDouble("bpm");
	}
	
	public double getBPM(){
		return updatedStats != null ? updatedStats.bpm : map.getDouble("bpm");
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
		return map.isNull("approved_date") ? null : new CustomDate(map.getString("approved_date"));
	}
	
	public int getBaseTotalLength(){
		return map.getInt("total_length");
	}
	
	public int getTotalLength(){
		return updatedStats != null ? (int) ((double) map.getInt("total_length") / updatedStats.speed) : map.getInt("total_length");
	}
	
	public String getFormattedTotalLength(){
		return Utils.toDuration(getTotalLength() * 1000);
	}
	
	public String getFormattedBaseTotalLength(){
		return Utils.toDuration(getBaseTotalLength() * 1000);
	}
	
	public int getBaseDrainLength(){
		return map.getInt("hit_length");
	}
	
	public int getDrainLength(){
		return updatedStats != null ? (int) ((double) map.getInt("hit_length") / updatedStats.speed) : map.getInt("hit_length");
	}
	
	public String getFormattedBaseDrainLength(){
		return Utils.toDuration(getBaseDrainLength() * 1000);
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
	
	public int getObjectsPlayed() {
		return getMisses() + getFifties() + getHundreds() + getThreeHundreds();
	}
	
	public double getMapCompletion() {
		return updatedStats != null ? Utils.stringToDouble(Utils.df((double) getObjectsPlayed() / (double) updatedStats.objects * 100, 2)) : 0;
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
	
	public void loadPP(){
		pp = new PPInfo(0, 0, 0, 0, 0);
		
		Utils.Login.osu();
		File osuFile = TrackingUtils.fetchOsuFile(getBeatmapId(), getBeatmapSetId());
		
		if(osuFile == null) return;
		
		osuFile.renameTo(new File(getBeatmapId() + ".osu"));
		osuFile = new File(getBeatmapId() + ".osu");
		
		try{		
			Koohii.Map map = new Koohii.Parser().map(new BufferedReader(new FileReader(osuFile)));
			Koohii.DiffCalc diff = new Koohii.DiffCalc().calc(map, getRawMods());
			Koohii.PPv2Parameters params = new Koohii.PPv2Parameters();
			params.beatmap = map;
			params.aim_stars = diff.aim;
			params.speed_stars = diff.speed;
			params.mods = getRawMods();
			params.n300 = getThreeHundreds() + getMisses();
			params.n100 = getHundreds();
			params.n50 = getFifties();
			params.nmiss = 0;
			
			double currentPP = 0;
			double ppForFC = 0;
			double aim = 0;
			double speed = 0;
			double acc = 0;
			
			params.combo = getMaxCombo();
			
			if(params.n300 - map.nsliders - map.nspinners < 0)
				params.score_version = 2;
			
			Koohii.PPv2 ppv2 = new Koohii.PPv2(params);
			ppForFC = ppv2.total;
			
			params.score_version = 1;
			
			if(!isPerfect()) params.combo = getCombo();
			
			params.n300 = getThreeHundreds();
			params.nmiss = getMisses();
			
			if(params.n300 - map.nsliders - map.nspinners < 0)
				params.score_version = 2;
			
			ppv2 = new Koohii.PPv2(params);
			
			currentPP = ppv2.total;
			aim = ppv2.aim;
			speed = ppv2.speed;
			acc = ppv2.acc;
			updatedStats = new MapStats(getMods(), getApproachRate(), getOverallDifficulty(), getHPDrain(), getCircleSize(), getBPM(), diff.total, map.objects.size());
			pp = new PPInfo(currentPP, ppForFC, Utils.stringToDouble(Utils.df(aim, 2)), Utils.stringToDouble(Utils.df(speed, 2)), Utils.stringToDouble(Utils.df(acc, 2)));
		}catch(Exception e){
			Log.logger.log(Level.INFO, "Could not load peppers: " + e.getMessage());
			e.printStackTrace();
		}finally{
			osuFile.delete();
		}
	}
	
	public PPInfo getPPInfo() {
		return pp;
	}
	
	public double getPP(){
		if(pp == null) return 0.0;
		
		return pp.getPP();
	}
	
	public double getPPForFC(){
		if(pp == null) return 0.0;
		
		return pp.getPPForFC();
	}
	
	public double getAimPP(){
		if(pp == null) return 0.0;
		
		return pp.getAimPP();
	}
	
	public double getSpeedPP(){
		if(pp == null) return 0.0;
		
		return pp.getSpeedPP();
	}
	
	public double getAccPP(){
		if(pp == null) return 0.0;
		
		return pp.getAccPP();
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
	
	public boolean compare(TrackedPlay other){
		return other.getBeatmapId() == getBeatmapId() &&
			   other.getRawMods() == getRawMods() &&
			   other.getRawScore() == getRawScore() &&
			   other.getDate().getTime() == getDate().getTime();
	}
	
	private static class MapStats {
		public double ar, od, hp, cs, bpm, stars, speed;
		public int objects;
		
		private static final double OD0_MS = 80;
		private static final double OD10_MS = 20;
		private static final double AR0_MS = 1800.0;
		private static final double AR5_MS = 1200.0;
		private static final double AR10_MS = 450.0;

		private static final double OD_MS_STEP = (OD0_MS - OD10_MS) / 10.0;
		private static final double AR_MS_STEP1 = (AR0_MS - AR5_MS) / 5.0;
		private static final double AR_MS_STEP2 = (AR5_MS - AR10_MS) / 5.0;
		
		public MapStats(List<Mods> mods, double baseAR, double baseOD, double baseHP, double baseCS, double baseBPM, double stars, int objects) {
			ar = baseAR;
			od = baseOD;
			hp = baseHP;
			cs = baseCS;
			bpm = baseBPM;
			this.objects = objects;
			this.stars = stars;
			
			speed = 1;
			double od_ar_hp_multiplier = 1;
			
			if(mods.contains(Mods.DoubleTime) || mods.contains(Mods.Nightcore))
				speed = 1.5;
			
			if(mods.contains(Mods.HalfTime))
				speed = 0.75;
			
			bpm *= speed;
			
			if(mods.contains(Mods.HardRock))
				od_ar_hp_multiplier = 1.4;
			
			if(mods.contains(Mods.Easy))
				od_ar_hp_multiplier = 0.5;
			
			ar *= od_ar_hp_multiplier;
			
	        /* convert AR into milliseconds window */
	        double arms = ar < 5.0f ? AR0_MS - AR_MS_STEP1 * ar : AR5_MS - AR_MS_STEP2 * (ar - 5.0f);

	        /* stats must be capped to 0-10 before HT/DT which brings
	        them to a range of -4.42->11.08 for OD and -5->11 for AR */
	        arms = Math.min(AR0_MS, Math.max(AR10_MS, arms));
	        arms /= speed;

	        ar = (float)(arms > AR5_MS ? (AR0_MS - arms) / AR_MS_STEP1 : 5.0 + (AR5_MS - arms) / AR_MS_STEP2);
	        
	        od *= od_ar_hp_multiplier;
	        
	        double odms = OD0_MS - Math.ceil(OD_MS_STEP * od);
	        
	        odms = Math.min(OD0_MS, Math.max(OD10_MS, odms));
	        odms /= speed;
	        od = (float)((OD0_MS - odms) / OD_MS_STEP);
	        
	        if(mods.contains(Mods.HardRock)) cs *= 1.3f;
	        if(mods.contains(Mods.Easy)) cs *= 0.5f;

	        cs = Math.min(10.0f, cs);
	        hp = Math.min(10.0f, hp * od_ar_hp_multiplier);
		}
	}
}
