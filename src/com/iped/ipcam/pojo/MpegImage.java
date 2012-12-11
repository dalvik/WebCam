package com.iped.ipcam.pojo;


public class MpegImage {

	public byte[] rgb;
	
	public String time;
	
	public MpegImage(byte[] rgb, String time) {
		this.rgb = rgb;
		this.time = time;
	}

	@Override
	public String toString() {
		return "rgb [ time=" + time + "]";
	}
	
}
