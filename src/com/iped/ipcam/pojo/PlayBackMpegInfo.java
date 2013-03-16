package com.iped.ipcam.pojo;

import java.util.Arrays;

public class PlayBackMpegInfo {

	private byte[] data;
	
	private int len;
	
	private String time;

	public PlayBackMpegInfo() {
		super();
	}

	public PlayBackMpegInfo(byte[] data, int len, String time) {
		super();
		this.data = data;
		this.len = len;
		this.time = time;
	}

	public byte[] getData() {
		return data;
	}

	public void setData(byte[] data) {
		this.data = data;
	}

	public int getLen() {
		return len;
	}

	public void setLen(int len) {
		this.len = len;
	}

	public String getTime() {
		return time;
	}

	public void setTime(String time) {
		this.time = time;
	}

	@Override
	public String toString() {
		return "PlayBackMpegInfo [data=" + Arrays.toString(data) + ", len="
				+ len + ", time=" + time + "]";
	}

	
	
}
