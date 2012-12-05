package com.iped.ipcam.utils;

import java.util.Comparator;
import java.util.LinkedList;

import com.iped.ipcam.pojo.Image;

public class VideoQueue {

	private LinkedList<Image> list = null;
	
	private LinkedList<String> timeList = null;
	
	public VideoQueue() {
		list = new LinkedList<Image>();
		timeList = new LinkedList<String>();
	}
	
	public void addNewImage(Image image) {
		if(list.size()>=4) {
			Image i = list.poll();
			if(i.bitmap != null) {
				i.bitmap.recycle();
				i = null;
			}
		}
		list.add(image);
		//System.out.println("image cache list size ===>" + list.size() + " " + image);
		//Collections.sort(list, new ImageComparator());
	}
	
	public void addNewTime(String time) {
		timeList.add(time);
		//System.out.println("time list size ===>" + timeList.size() + " " + time);
	}
	
	public Image removeImage() {
		return list.poll();
	}
	
	public String removeTime() {
		return timeList.poll();
	}
	
	public Image getFirstImage() {
		return list.peek();
	}
	
	public String getFirstTime() {
		return timeList.peek();
	}
	
	public void clear() {
		list.clear();
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
