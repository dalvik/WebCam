package com.iped.ipcam.engine;

import java.util.Date;
import java.util.List;

import android.os.Handler;

import com.iped.ipcam.pojo.Video;

public interface IVideoManager {

	public List<Video> getVideoList();
	
	public void videoSearchInit(String device, Date start, Date end);
	
	public void startSearchThread(Handler handler);
	
	
}
