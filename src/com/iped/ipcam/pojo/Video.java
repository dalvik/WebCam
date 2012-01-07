package com.iped.ipcam.pojo;

public class Video {

	private int index;
	
	private String videoName;
	
	private String videoStartTime;
	
	private String videoEndTime;
	
	private int fileLength;

	private String address;
	
	public Video() {
		super();
	}

	public Video(int index, String videoName, String videoStartTime,
			String videoEndTime, int fileLength, String address) {
		super();
		this.index = index;
		this.videoName = videoName;
		this.videoStartTime = videoStartTime;
		this.videoEndTime = videoEndTime;
		this.fileLength = fileLength;
		this.address = address;
	}

	public int getIndex() {
		return index;
	}

	public void setIndex(int index) {
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

	public int getFileLength() {
		return fileLength;
	}

	public void setFileLength(int fileLength) {
		this.fileLength = fileLength;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	@Override
	public String toString() {
		return "Video [address=" + address + ", fileLength=" + fileLength
				+ ", index=" + index + ", videoEndTime=" + videoEndTime
				+ ", videoName=" + videoName + ", videoStartTime="
				+ videoStartTime + "]";
	}


	
}
