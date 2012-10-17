package com.iped.ipcam.engine;

import java.util.ArrayList;
import java.util.List;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.iped.ipcam.exception.CamManagerException;
import com.iped.ipcam.gui.UdtTools;
import com.iped.ipcam.pojo.Device;
import com.iped.ipcam.utils.Constants;

public class CamManagerImp implements ICamManager {

	
	private List<Device> deviceList = new ArrayList<Device>();
	
	//private Thread queryThread = null;
	
	private QueryCamThread queryThread = null;
	
	private int selectIndex = 0;

	private boolean stopFlag = false;
	
	private static int searchCounter = 1;
	
	private String TAG = "CamManagerImp";
	
	public CamManagerImp() {
	}
	
	@Override
	public Device addCam(String ip, String id) {
		Device d = new Device();
		d.setDeviceName("IpCam");
		d.setDeviceID(id);
		/*d.setDeviceEthIp(ip);
		d.setDeviceEthGateWay((ip.substring(0, ip.lastIndexOf(".")+1) + "1"));
		d.setDeviceEthMask("255.255.255.0");*/
		if(checkName(d)) {
			deviceList.add(d);
			return d;
		}
		return null;
	}

	private boolean checkName(Device device) {
		for(Device de:deviceList) {
			if(device.hashCode() == de.hashCode()) {
				return false;
			}
		}
		return true;
	}
	
	@Override
	public boolean addCam(Device device) {
		if(checkName(device)) {
			deviceList.add(device);
			return true;
		}
		return false;
	}
	
	@Override
	public boolean editCam(Device deviceOLd, Device deviceNew) {
		return true;
	}
	
	
	@Override
	public boolean delCam(int index) {
		deviceList.remove(index);
		return false;
	}
	
	@Override
	public void updateCam(Device device) {
		deviceList.remove(selectIndex);
		deviceList.add(selectIndex,device);
	}
	
	@Override
	public boolean delCam(String name) {
		return false;
	}
	
	@Override
	public Device getDevice(int index){
		if(index>=deviceList.size()) {
			return null;
		}
		return deviceList.get(index);
	}
	
	@Override
	public Device getSelectDevice() {
		if(selectIndex<deviceList.size()) {
			return getDevice(selectIndex);
		}
		return null;
	}
	
	@Override
	public Device getDeviceByName(String name) {
		return null;
	}
	
	@Override
	public List<Device> getCamList() {
		return deviceList;
	}

	@Override
	public boolean isOnline(String ip, int port) throws CamManagerException {
		return true;
	}
	
	@Override
	public void clearCamList() {
		selectIndex = 0;
		deviceList.clear();
	}
	
	public List<Device> getDeviceList() {
		return deviceList;
	}

	public void setDeviceList(List<Device> deviceList) {
		this.deviceList = deviceList;
	}

	public void startThread(Handler handler) {
		//stopThread();
		if(queryThread == null || !queryThread.isAlive() || stopFlag) {
			queryThread = new QueryCamThread(handler);
			queryThread.start();
		}
	}
	
	@Override
	public void stopThread() {
		stopFlag = true;
		UdtTools.stopSearch();
		if(queryThread != null && queryThread.isAlive()) {
			try {
				queryThread.join(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			queryThread = null;
		}
	}
	
	public void setSelectInde(int selectIndex) {	
		this.selectIndex = selectIndex;
	}
	
	private void updateDeviceList() {
		queryThread.update();
	}
	
	public void dismissAutoSearch() {
		queryThread.dissmiss();
	}
	
	public void updateProgress(int value) {
		queryThread.updateProgressBar(value * 10);
	}
	
	class QueryCamThread extends Thread {
		
		private Handler handler;
		
		//private int i = 0;
		
		private QueryCamThread(Handler handler) {
			this.handler = handler;
		}
		
		public void update() {
			if(!stopFlag) {
				handler.sendEmptyMessage(Constants.UPDATEDEVICELIST);
				System.out.println("update=======>" + deviceList.size());
			}
		}
		
		public void dissmiss() {
			handler.sendEmptyMessage(Constants.HIDETEAUTOSEARCH);
		}
		
		public void updateProgressBar(int value) {
			Message message = handler.obtainMessage();
			message.what = Constants.UPDATEAUTOSEARCH;
			message.arg1 = value; //int)(i*per);
			if(value>=100){
			}
			handler.sendMessage(message);
		}
		
		@Override
		public void run() {
			stopFlag = false;
			handler.postDelayed(fetchWebCamIdTask, 100);
			UdtTools.startSearch();
		}
		
		private Runnable fetchWebCamIdTask = new Runnable() {
			
			@Override
			public void run() {
				String id = UdtTools.fetchCamId();
				if(id != null) {
					Log.d(TAG, "cam id = " + id);
					searchCounter = 1;
					addCam(id, id);
					updateDeviceList();
				}else {
					if(searchCounter++>=3) {
						stopFlag = true;
					}
				}
				if(stopFlag) {
					dismissAutoSearch();
					handler.removeCallbacks(fetchWebCamIdTask);
					searchCounter = 1;
				}else {
					handler.postDelayed(fetchWebCamIdTask, 350);
				}
			}
		};
	}

}
