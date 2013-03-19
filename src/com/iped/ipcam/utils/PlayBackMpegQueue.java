package com.iped.ipcam.utils;

import java.util.LinkedList;

import android.util.Log;

import com.iped.ipcam.pojo.PlayBackMpegInfo;

public class PlayBackMpegQueue {

	private LinkedList<PlayBackMpegInfo> mpegQueue = null;
	
	private Object lock = new Object();
	
	public PlayBackMpegQueue() {
		mpegQueue = new LinkedList<PlayBackMpegInfo>();
	}
	
	public void addMpeg(PlayBackMpegInfo pbmi) {
		synchronized (lock) {
			mpegQueue.add(pbmi);
		}
	}
	
	public int getMpegLength() {
		synchronized (lock) {
			return mpegQueue.size();
		}
	}
	
	public PlayBackMpegInfo getMpeg() {
		synchronized (lock) {
			try{
				return mpegQueue.poll();
			}catch(Exception e) {
				Log.d("PlayBackMpegQueue", "### 111 " + e.getMessage());
				return null;
			}
		}
	}
}
