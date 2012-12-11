package com.iped.ipcam.utils;

import java.util.LinkedList;

import com.iped.ipcam.pojo.JpegImage;
import com.iped.ipcam.pojo.MpegImage;

public class VideoQueue {

	private LinkedList<JpegImage> jpegImageList = null;
	
	private LinkedList<String> timeList = null;
	
	private LinkedList<MpegImage> mpegImageList = null;
	
	public VideoQueue() {
		jpegImageList = new LinkedList<JpegImage>();
		timeList = new LinkedList<String>();
		mpegImageList = new LinkedList<MpegImage>();
	}
	
	public void addJpegImage(JpegImage image) {
		if(jpegImageList.size()>=4) {
			JpegImage i = jpegImageList.poll();
			if(i.bitmap != null) {
				i.bitmap.recycle();
				i = null;
			}
		}
		jpegImageList.add(image);
	}
	
	
	public void addNewTime(String time) {
		timeList.add(time);
	}
	
	public int getTimeListLength() {
		return timeList.size();
	}
	
	public int getImageListLength() {
		return jpegImageList.size();
	}
	
	public JpegImage removeImage() {
		return jpegImageList.poll();
	}
	
	public String removeTime() {
		return timeList.poll();
	}
	
	public JpegImage getFirstImage() {
		return jpegImageList.peek();
	}
	
	public String getFirstTime() {
		return timeList.peek();
	}
	
	public void clear() {
		jpegImageList.clear();
	}
	
	public void clearTime() {
		timeList.clear();
	}
	
	/** store mpeg4 **/
	public void addMpegImage(MpegImage mpegImage) {
		if(mpegImageList.size()>=4) {
			MpegImage i = mpegImageList.remove();//.poll();
			i.rgb = null;
		}
		mpegImageList.add(mpegImage);
	}
	
	public MpegImage getMpegImage() {
		return mpegImageList.peek();
	}
	
	public void removeMpegImage() {
		mpegImageList.remove();
	}
	
	public int getMpegLength() {
		return  mpegImageList.size();
	}
	/*private class ImageComparator implements Comparator<Image> {
		
		@Override
		public int compare(Image image1, Image image2) {
			if(image1.time.compareTo(image2.time)>=0){
				return -1000;
			}
			return 1000;
		}
		
	}*/
}
