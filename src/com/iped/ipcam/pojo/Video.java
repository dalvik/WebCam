package com.iped.ipcam.pojo;

public class Video {

	private String index;
	
	private String videoName;
	
	private String videoStartTime;
	
	private String videoEndTime;
	
	private String fileLength;

	private String address;
	
	public Video() {
		super();
	}

	public Video(String index, String videoName, String videoStartTime,
			String videoEndTime, String fileLength, String address) {
		super();
		this.index = index;
		this.videoName = videoName;
		this.videoStartTime = videoStartTime;
		this.videoEndTime = videoEndTime;
		this.fileLength = fileLength;
		this.address = address;
	}

	public String getIndex() {
		return index;
	}

	public void setIndex(String index) {
		this.index = index;
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

	public String getFileLength() {
		return fileLength;
	}

	public void setFileLength(String fileLength) {
		this.fileLength = fileLength;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}
	
}
