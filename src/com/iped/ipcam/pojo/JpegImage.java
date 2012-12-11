package com.iped.ipcam.pojo;

import android.graphics.Bitmap;

public class JpegImage {

	public Bitmap bitmap;
	
	public String time;
	
	public JpegImage(Bitmap bitmap, String time) {
		this.bitmap = bitmap;
		this.time = time;
	}

	@Override
	public String toString() {
		return "Image [ time=" + time + "]";
	}
	
}
