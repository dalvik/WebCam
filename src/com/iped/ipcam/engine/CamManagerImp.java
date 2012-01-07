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
import com.iped.ipcam.pojo.Device;
import com.iped.ipcam.utils.CamCmdListHelper;
import com.iped.ipcam.utils.Constants;

public class CamManagerImp implements ICamManager {

	
	private List<Device> deviceList = new ArrayList<Device>();
	
	private Thread queryThread = null;
	
	private int selectIndex = 0;
	
	private String TAG = "CamManagerImp";
	
	public CamManagerImp() {
		Device d = new Device("192.168.1.211", "Ip Camera", "192.168.1.211", Constants.TCPPORT, Constants.UDPPORT, Constants.DEFAULTWAY);
			deviceList.add(d);
	}
	
	@Override
	public Device addCam(String ip) {
		System.out.println(checkName(ip));
		Device d = new Device(ip, "Ip Camera", ip, Constants.TCPPORT, Constants.UDPPORT, Constants.DEFAULTWAY);
		if(checkName(ip)) {
			deviceList.add(d);
		}
		return d;
	}

	private boolean checkName(String ip) {
		for(Device de:deviceList) {
			if(ip.equalsIgnoreCase(de.getDeviceName())) {
				return false;
			}
		}
		return true;
	}
	
	@Override
	public Device addCam(Device device) {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public boolean delCam(int index) {
		deviceList.remove(index);
		return false;
	}
	
	@Override
	public boolean delCam(String name) {
		// TODO Auto-generated method stub
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
		// TODO Auto-generated method stub
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
	}
	
	public List<Device> getDeviceList() {
		return deviceList;
	}

	public void setDeviceList(List<Device> deviceList) {
		this.deviceList = deviceList;
	}

	public void startThread(Handler handler) {
		if(queryThread == null || !queryThread.isAlive()) {
			queryThread = new Thread(new QueryCamThread(handler));
			queryThread.start();
		}
	}
	
	@Override
	public void stopThread() {
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
	
	class QueryCamThread implements Runnable {
		
		private Handler handler;
		
		private static final String TAG = "QueryCamThread";
		
		private int i = 0;
		
		private byte [] tem = CamCmdListHelper.QueryCmd_Online.getBytes();
		
		private byte [] buffTemp = new byte[Constants.COMMNICATEBUFFERSIZE];
		
		private QueryCamThread(Handler handler) {
			this.handler = handler;
		}
		
		@Override
		public void run() {
			//float max = 100;
			//final float per = max/Constants.MAXVALUE;
			new MyTimer(handler).start();
			long l = System.currentTimeMillis();
			for(i=1; i<25; i++) {
				try {
					Thread.sleep(100);
				} catch (InterruptedException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				new Thread() {
					public void run() {
						try {
							/*if(isOnline(Constants.DEFAULTSEARCHIP + i, Constants.UDPPORT)) {
								addCam(Constants.DEFAULTSEARCHIP + i);
								handler.sendEmptyMessage(Constants.UPDATEDEVICELIST);
							}*/
							DatagramSocket datagramSocket = null;
							try {
								datagramSocket = new DatagramSocket();
								datagramSocket.setSoTimeout(Constants.DEVICESEARCHTIMEOUT);
								DatagramPacket datagramPacket = new DatagramPacket(tem, tem.length, InetAddress.getByName(Constants.DEFAULTSEARCHIP + i), Constants.UDPPORT);
								datagramSocket.send(datagramPacket);
								DatagramPacket rece = new DatagramPacket(buffTemp, buffTemp.length);
								datagramSocket.receive(rece);
								Log.d(TAG, "receive inof ");
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
						} catch (CamManagerException e) {
							Log.d(TAG, "AutoSearchThread run 0 " + e.getMessage());
						}/* finally {
							Message message = handler.obtainMessage();
							message.what = Constants.UPDATEAUTOSEARCH;
							message.arg1 = (int)(i*per);
							handler.sendMessage(message);
						}*/
					}
				}.start();
			}
			Log.d(TAG, "start thread use time : " + (System.currentTimeMillis() - l)/1000);
		}
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
						message.arg1 = i;//int)(i*per);
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
