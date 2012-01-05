package com.iped.ipcam.pojo;

public class Video {

	private int index;
	
	private int fileLength;
	
	private String videoName;
	
	private String videoStartTime;
	
	private String videoEndTime;

	public Video() {
		super();
	}

	public Video(int index, int fileLength, String videoName,
			String videoStartTime, String videoEndTime) {
		super();
		this.index = index;
		this.fileLength = fileLength;
		this.videoName = videoName;
		this.videoStartTime = videoStartTime;
		this.videoEndTime = videoEndTime;
	}

	public String getVideoName() {
		return videoName;
	}

	public void setVideoName(String videoName) {
		this.videoName = videoName;
	}

	public String getVideoStartTime() {
		return videoStartTime;
	}

	public void setVideoStartTime(String videoStartTime) {
		this.videoStartTime = videoStartTime;
	}

	public String getVideoEndTime() {
		return videoEndTime;
	}

	public void setVideoEndTime(String videoEndTime) {
		this.videoEndTime = videoEndTime;
	}

	public int getIndex() {
		return index;
	}

	public void setIndex(int index) {
		this.index = index;
	}

	public int getFileLength() {
		return fileLength;
	}

	public void setFileLength(int fileLength) {
		this.fileLength = fileLength;
	}
	
}
