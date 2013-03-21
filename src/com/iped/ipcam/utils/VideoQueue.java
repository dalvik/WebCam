package com.iped.ipcam.utils;

import java.util.LinkedList;

import android.util.Log;

import com.iped.ipcam.pojo.JpegImage;
import com.iped.ipcam.pojo.MpegImage;

public class VideoQueue {

	private LinkedList<JpegImage> jpegImageList = null;
	
	private LinkedList<String> timeList = null;
	
	private LinkedList<MpegImage> mpegImageList = null;
	
	private Object lock = new Object();
	
	private Object obj = new Object();
	
	private String TAG = "VideoQueue";
	
	public static final int defintImageQueueLength = 3;
	
	public VideoQueue() {
		jpegImageList = new LinkedList<JpegImage>();
		timeList = new LinkedList<String>();
		mpegImageList = new LinkedList<MpegImage>();
	}
	
	public void addJpegImage(JpegImage image) {
		if(jpegImageList.size()>=4) {
			try{
				JpegImage i = jpegImageList.poll();
				if(i != null) {
					if(i.bitmap != null) {
						i.bitmap.recycle();
						i = null;
					}
				}
			}catch(Exception e) {
				Log.e(TAG, "### addJpegImage = " + e.getLocalizedMessage());
				jpegImageList.clear();
			}
		}
		jpegImageList.add(image);
	}
	
	
	public void addNewTime(String time) {
		synchronized (lock) {
			timeList.offer(time);
		}
	}
	
	public int getTimeListLength() {
		return timeList.size();
	}
	
	public int getImageListLength() {
		return jpegImageList.size();
	}
	
	public JpegImage removeImage() {
		synchronized (obj) {
			if(jpegImageList.size()>0) {
				try{
					return jpegImageList.poll();
				}catch(Exception e) {
					return null;
				}
			}else {
				return null;
			}
		}
	}
	
	public String pollTime() {
		try{
			synchronized (lock) {
				return timeList.poll();
			}
		}catch(Exception e) {
			Log.d("VideoQueue", "### 111 " + e.getMessage());
			return null;
		}
	}
	
	public JpegImage getFirstImage() {
		if(getImageListLength()>0) {
			try{
				synchronized (lock) {
					return jpegImageList.peek();
				}
			}catch(Exception e) {
				Log.d("VideoQueue", "### 2222 " + e.getLocalizedMessage());
				return null;
			}
		}else {
			return null;
		}
	}
	
	public String getFirstTime() {
		synchronized (lock) {
			if(getTimeListLength()>0) {
				try{
					return timeList.peek();
				}catch(Exception e) {
					Log.d("VideoQueue", "### 3333 " + e.getLocalizedMessage());
					return null;
				}
			}
		}
		return null;
	}
	
	public void clear() {
		synchronized (lock) {
			jpegImageList.clear();
			mpegImageList.clear();
		}
	}
	
	public void clearTime() {
		synchronized (lock) {
			timeList.clear();
		}
	}
	
	/** store mpeg4 **/
	public void addMpegImage(MpegImage mpegImage) {
		synchronized (lock) {
			mpegImageList.offer(mpegImage);
		}
	}
	
	public MpegImage getMpegImage() {
		synchronized (lock) {
			if(getMpegLength()>0) {
				try{
					return mpegImageList.poll();//.peek();
				}catch(Exception e) {
					Log.d("VideoQueue", "### 5555 " + e.getLocalizedMessage());
				}
			}
		}
		return null;
	}
	
	public void removeMpegImage() {
		synchronized (lock) {
			if(getMpegLength()>0) {
				try{
					mpegImageList.remove();
				}catch(Exception e) {
					Log.d("VideoQueue", "### 6666 " + e.getLocalizedMessage());
				}
			}
		}
	}
	
	public int getMpegLength() {
		synchronized (lock) {
			return  mpegImageList.size();
		}
	}
}
