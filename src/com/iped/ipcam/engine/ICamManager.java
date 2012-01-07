package com.iped.ipcam.engine;

import java.util.List;

import android.os.Handler;

import com.iped.ipcam.exception.CamManagerException;
import com.iped.ipcam.pojo.Device;

public interface ICamManager {

	public List<Device> getCamList();
	
	public Device addCam(Device device);
	
	public Device addCam(String ip);
	
	public boolean delCam(int dex);
	
	public boolean delCam(String name);
	
	public void clearCamList();
	
	public Device getDevice(int index);
	
	public Device getSelectDevice();
	
	public Device getDeviceByName(String name);
	
	public boolean isOnline(String ip, int port) throws CamManagerException;
	
	public void setSelectInde(int selectIndex);
	
	public void startThread(Handler handler);
	
	public void stopThread();
	
}
