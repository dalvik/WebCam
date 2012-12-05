package com.iped.ipcam.utils;

import java.util.LinkedList;

import com.iped.ipcam.pojo.Image;

public class VideoQueue {

	private LinkedList<Image> list = null;
	
	
	public VideoQueue() {
		list = new LinkedList<Image>();
	}
	
	public void addNewImage(Image image) {
		if(list.size()>=3) {
			Image i = list.poll();
			if(i.bitmap != null) {
				i.bitmap.recycle();
				i = null;
			}
		}
		list.add(image);
		System.out.println("cache list size ===>" + list.size());
		//Collections.sort(list, new ImageComparator());
	}
	
	public Image removeImage() {
		return list.poll();
	}
	
	public Image getFirstImage() {
		return list.peek();
	}
	
	public void clear() {
		list.clear();
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
