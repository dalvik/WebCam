package com.iped.ipcam.engine;

import java.util.List;

import android.os.Handler;

import com.iped.ipcam.pojo.Video;

public interface IVideoManager {

	public List<Video> getVideoList();
	
	public void startSearchThread(Handler handler);
	
	
}
