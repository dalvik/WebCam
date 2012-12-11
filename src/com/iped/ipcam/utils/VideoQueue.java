package com.iped.ipcam.utils;

import java.util.LinkedList;

import com.iped.ipcam.pojo.Image;

public class VideoQueue {

	private LinkedList<Image> jpegImageList = null;
	
	private LinkedList<String> timeList = null;
	
	public VideoQueue() {
		jpegImageList = new LinkedList<Image>();
		timeList = new LinkedList<String>();
	}
	
	public void addNewImage(Image image) {
		if(jpegImageList.size()>=4) {
			Image i = jpegImageList.poll();
			if(i.bitmap != null) {
				i.bitmap.recycle();
				i = null;
			}
		}
		jpegImageList.add(image);
		//System.out.println("image cache list size ===>" + list.size() + " " + image);
		//Collections.sort(list, new ImageComparator());
	}
	
	public void addNewTime(String time) {
		timeList.add(time);
		//System.out.println("add new time and list size ===>" + timeList.size() + " " + time);
	}
	
	public int getTimeListLength() {
		return timeList.size();
	}
	
	public int getImageListLength() {
		return jpegImageList.size();
	}
	
	public Image removeImage() {
		return jpegImageList.poll();
	}
	
	public String removeTime() {
		return timeList.poll();
	}
	
	public Image getFirstImage() {
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
