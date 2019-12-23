package me.smc.sb.tracking;

public class RecentPlay{
	
	private int beatmapId;
	private CustomDate date;
	private int rank;
	
	public RecentPlay(int beatmapId, CustomDate date, int rank){
		this.beatmapId = beatmapId;
		this.date = date;
		this.rank = rank;
	}
	
	public boolean isDateValid(CustomDate otherDate, int secondsLeeway){
		CustomDate beforeDate = new CustomDate(date.getDate());
		CustomDate afterDate = new CustomDate(date.getDate());
		
		beforeDate.add(-secondsLeeway);
		afterDate.add(secondsLeeway);
		
		if(otherDate.after(beforeDate) && afterDate.after(otherDate)) return true;
		
		return false;
	}
	
	public CustomDate getDate(){
		return date;
	}
	
	public int getBeatmapId(){
		return beatmapId;
	}
	
	public int getRank(){
		return rank;
	}
	
	public boolean eq(RecentPlay other){
		return beatmapId == other.getBeatmapId() &&
			   date.equals(other.date) &&
			   rank == other.getRank();
	}
}
