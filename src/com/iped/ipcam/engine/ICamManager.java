package com.iped.ipcam.engine;

import java.util.List;

import android.os.Handler;

import com.iped.ipcam.pojo.Device;

public interface ICamManager {

	public List<Device> getCamList();
	
	public Device addCam(Device device);
	
	public Device addCam(String ip);
	
	public boolean delCam(int dex);
	
	public boolean delCam(String name);
	
	public void clearCamList();
	
	public Device getDevice(int index);
	
	public Device getDeviceByName(String name);
	
	public boolean isOnline(String ip, int port);
	
	public void startThread(Handler handler);
	
	public void stopThread();
	
}
