package com.iped.ipcam.pojo;

import java.util.Arrays;

public class PlayBackMpegInfo {

	private byte[] data;
	
	private int len;

	public PlayBackMpegInfo() {
		super();
	}

	public PlayBackMpegInfo(byte[] data, int len) {
		super();
		this.data = data;
		this.len = len;
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

	@Override
	public String toString() {
		return "PlayBackMpegInfo [data=" + Arrays.toString(data) + ", len="
				+ len + "]";
	}
	
	
}
