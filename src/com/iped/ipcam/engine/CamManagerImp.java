package com.iped.ipcam.engine;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.iped.ipcam.exception.CamManagerException;
import com.iped.ipcam.gui.UdtTools;
import com.iped.ipcam.pojo.Device;
import com.iped.ipcam.utils.CamCmdListHelper;
import com.iped.ipcam.utils.Constants;
import com.iped.ipcam.utils.PackageUtil;

public class CamManagerImp implements ICamManager {

	
	private List<Device> deviceList = new ArrayList<Device>();
	
	//private Thread queryThread = null;
	
	private QueryCamThread queryThread = null;
	
	private int selectIndex = 0;

	private int temp = -1;
	
	private int  count = 1;
	
	private boolean stopFlag = false;
	
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
		DatagramSocket datagramSocket = null;
		byte [] tem = CamCmdListHelper.QueryCmd_Online.getBytes();
		
		byte [] buffTemp = new byte[Constants.COMMNICATEBUFFERSIZE];
		try {
			datagramSocket = new DatagramSocket();
			datagramSocket.setSoTimeout(Constants.DEVICESEARCHTIMEOUT);
			DatagramPacket datagramPacket = new DatagramPacket(tem, tem.length, InetAddress.getByName(ip), port);
			datagramSocket.send(datagramPacket);
			DatagramPacket rece = new DatagramPacket(buffTemp, buffTemp.length);
			datagramSocket.receive(rece);
			Log.d(TAG, "receive inof ");
			return true;
		} catch (SocketException e) {
			//Log.d(TAG, "CamManagerImp isOnline : " + e.getLocalizedMessage());
			throw new CamManagerException(e);
		} catch (UnknownHostException e) {
			//Log.d(TAG, "CamManagerImp isOnline : " + e.getLocalizedMessage());
			throw new CamManagerException(e);
		} catch (IOException e) {
			//Log.d(TAG, "CamManagerImp isOnline : " + e.getLocalizedMessage());
			throw new CamManagerException(e);
		} finally {
			if(datagramSocket != null) {
				datagramSocket.close();
				datagramSocket = null;
			}
		}
		
	}
	
	@Override
	public void clearCamList() {
		deviceList.clear();
		selectIndex = 0;
	}
	
	public List<Device> getDeviceList() {
		return deviceList;
	}

	public void setDeviceList(List<Device> deviceList) {
		this.deviceList = deviceList;
	}

	public void startThread(Handler handler) {
		//stopThread();
		queryThread = new QueryCamThread(handler);
		queryThread.setDaemon(true);
		queryThread.start();
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
		
		private int i = 0;
		
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
			/*Message message = handler.obtainMessage();
			message.what = Constants.UPDATEAUTOSEARCH;
			message.arg1 = i; //int)(i*per);
			handler.sendMessage(message);*/
		}
		
		public void updateProgressBar(int value) {
			Message message = handler.obtainMessage();
			message.what = Constants.UPDATEAUTOSEARCH;
			message.arg1 = value; //int)(i*per);
			if(value>=100){
				count = 1;
				temp = -1;
			}
			handler.sendMessage(message);
		}
		
		@Override
		public void run() {
			handler.postDelayed(fetchWebCamIdTask, 50);
			UdtTools.startSearch();
			stopFlag = true;
			/*for(i=1; i<=10; i++) {
				new Thread(new QueryOnline(i*26)).start();
				try {
					Thread.sleep(300);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}*/
		}
		
		private Runnable fetchWebCamIdTask = new Runnable() {
			
			@Override
			public void run() {
				if(stopFlag) {
					stopSearch();
					dismissAutoSearch();
				}else {
					String id = UdtTools.fetchCamId();
					if(id != null) {
						Log.d(TAG, "cam id = " + id);
						addCam(id, id);
						updateDeviceList();
					}
					handler.postDelayed(fetchWebCamIdTask, 10);
				}
			}
		};
		
		private void stopSearch() {
			handler.removeCallbacks(fetchWebCamIdTask);
			String id = UdtTools.fetchCamId();
			while(id != null) {
				Log.d(TAG, "cam id = " + id);
				addCam(id, id);
				updateDeviceList();
			}
		}
	}
	
	
	class QueryOnline implements Runnable {

		private int index;

		byte [] tem = CamCmdListHelper.QueryCmd_Online.getBytes();
		
		public QueryOnline(int index) {
			this.index = index;
		}
		
		
		@Override
		public void run() {
			int i = 0;
			for(i=index-25; i<=index; i++){
					if(i>1 && i<255) {
						if(!stopFlag) {
						//test(String.valueOf(i));
						String devId = PackageUtil.CMDPackage(CamCmdListHelper.QueryCmd_Online, Constants.DEFAULTSEARCHIP + i, Constants.LOCALCMDPORT);
						if(devId != null) {
							synchronized (deviceList) {
								addCam(Constants.DEFAULTSEARCHIP + i, devId);
								updateDeviceList();
								Log.d(TAG, Constants.DEFAULTSEARCHIP + i + "---------" +  devId  + " stopflag=" + stopFlag);
							}
						}
						try {
							Thread.sleep(500);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					  }
					}
					synchronized (deviceList) {
						count++;	
					}
					//System.out.println(count + " " + i + " " + count/26);
					if(temp<count/26) {
						temp = count/26;
						updateProgress(temp);
					}
			}
			dismissAutoSearch();
		}
		
		/*public void test(String ip) {
			DatagramSocket datagramSocket = null;
			try {
				datagramSocket = new DatagramSocket();
				datagramSocket.setSoTimeout(Constants.DEVICESEARCHTIMEOUT);
				DatagramPacket datagramPacket = new DatagramPacket(tem, CamCmdListHelper.QueryCmd_Online.length(), InetAddress.getByName("192.168.1." + ip), Constants.UDPPORT);
				datagramSocket.send(datagramPacket);
				DatagramPacket rece = new DatagramPacket(tem, tem.length);
				datagramSocket.receive(rece);
				synchronized (deviceList) {
					addCam(Constants.DEFAULTSEARCHIP + ip);
					updateDeviceList();
				}
				Log.d(TAG, "receive inof //////////////" + "192.168.1." + ip);
			} catch (SocketException e) {
				//Log.d(TAG, "CamManagerImp isoffline : " + (Constants.DEFAULTSEARCHIP + ip) + " " + e.getLocalizedMessage());
			} catch (UnknownHostException e) {
				//Log.d(TAG, "CamManagerImp isoffline : " + (Constants.DEFAULTSEARCHIP + ip) + " " + e.getLocalizedMessage());
			} catch (IOException e) {
				//Log.d(TAG, "CamManagerImp isoffline : " + (Constants.DEFAULTSEARCHIP + ip) + " " + e.getLocalizedMessage());
			} finally {
				if(datagramSocket != null) {
					datagramSocket.disconnect();
					datagramSocket.close();
					datagramSocket = null;
				}
			}
		}*/
	}
	
	
	
	
class MyTimer extends Thread {
		
		private int i = 0;
		
		private Handler handler;
		
		public MyTimer(Handler handler) {
			this.handler = handler;
		}
		
		public void run() {
			float max = 100;
			try {
				while(i<max) {
					i++;
					try {
						Thread.sleep(200);
					} catch (InterruptedException e) {
						e.printStackTrace();
					} finally {
						Message message = handler.obtainMessage();
						message.what = Constants.UPDATEAUTOSEARCH;
						message.arg1 = i; //int)(i*per);
						handler.sendMessage(message);
					}
				}
			} catch(Exception  e) {
				
			} finally {
				Message message = handler.obtainMessage();
				message.what = Constants.HIDETEAUTOSEARCH;
				handler.sendMessage(message);
			}
			
		}
	}
}
