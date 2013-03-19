package com.iped.ipcam.pojo;


public class MpegImage {

	public byte[] rgb;
	
	public String time;
	
	public int dataType;
	
	public int dataLength;
	
	public MpegImage(byte[] rgb, String time, int dataType, int dataLength) {
		super();
		this.rgb = rgb;
		this.time = time;
		this.dataType = dataType;
		this.dataLength = dataLength;
	}

	@Override
	public String toString() {
		return "rgb [ time=" + time + "]";
	}
	
}
