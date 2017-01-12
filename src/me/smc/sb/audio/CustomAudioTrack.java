package me.smc.sb.audio;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

public class CustomAudioTrack{

	private AudioTrack track;
	private String url;
	
	public CustomAudioTrack(AudioTrack track, String url){
		this.track = track;
		this.url = url;
	}

	public AudioTrack getTrack(){
		return track;
	}
	
	public String getURL(){
		return url;
	}
}
