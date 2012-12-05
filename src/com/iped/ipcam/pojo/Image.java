package com.iped.ipcam.pojo;

import android.graphics.Bitmap;

public class Image {

	public Bitmap bitmap;
	
	public String time;
	
	public Image(Bitmap bitmap, String time) {
		this.bitmap = bitmap;
		this.time = time;
	}

	@Override
	public String toString() {
		return "Image [ time=" + time + "]";
	}
	
}
