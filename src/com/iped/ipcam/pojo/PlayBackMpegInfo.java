package com.iped.ipcam.pojo;


public class PlayBackMpegInfo {

	public byte[] data;
	
	public int len;
	
	public String time;

	public int type;
	
	public PlayBackMpegInfo() {
		super();
	}

	public PlayBackMpegInfo(byte[] data, int len, String time, int type) {
		super();
		this.data = data;
		this.len = len;
		this.time = time;
		this.type = type;
	}

	
	
}
