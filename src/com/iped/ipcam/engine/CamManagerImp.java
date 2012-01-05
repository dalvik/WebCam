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

import com.iped.ipcam.pojo.Device;
import com.iped.ipcam.utils.CamCmdListHelper;
import com.iped.ipcam.utils.Constants;

public class CamManagerImp implements ICamManager {

	private List<Device> deviceList = new ArrayList<Device>();
	
	private Thread queryThread = null;
	
	public CamManagerImp() {

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
	public Device getDeviceByName(String name) {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public List<Device> getCamList() {
		return deviceList;
	}

	@Override
	public boolean isOnline(String ip, int port) {
		// TODO Auto-generated method stub
		return false;
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
			queryThread.interrupt();
			queryThread = null;
		}
	}
	
	class QueryCamThread implements Runnable {
		
		private DatagramSocket datagramSocket = null;
		
		private DatagramPacket datagramPacket = null;
		
		private byte [] buffTemp = new byte[Constants.COMMNICATEBUFFERSIZE];
		
		private Handler handler;
		
		private static final String TAG = "QueryCamThread";
		
		private QueryCamThread(Handler handler) {
			this.handler = handler;
		}
		
		
		@Override
		public void run() {
			byte [] tem = CamCmdListHelper.QueryCmd_Online.getBytes();
			float max = 100;
			float per = max/Constants.MAXVALUE;
			for(int i=1; i<255; i++) {
				try {
					datagramSocket = new DatagramSocket();
					datagramSocket.setSoTimeout(Constants.DEVICESEARCHTIMEOUT);
					datagramPacket = new DatagramPacket(tem, tem.length, InetAddress.getByName(Constants.DEFAULTSEARCHIP + i), Constants.UDPPORT);
					//datagramPacket = new DatagramPacket(tem, tem.length, InetAddress.getByName("192.168.1.121"), 60000);
					datagramSocket.send(datagramPacket);
					DatagramPacket rece = new DatagramPacket(buffTemp, buffTemp.length);
					datagramSocket.receive(rece);
					String info = new String(buffTemp);
					if(info != null) {
						Log.d(TAG, "receive inof = : " + info);
						addCam(Constants.DEFAULTSEARCHIP + i);
						handler.sendEmptyMessage(Constants.UPDATEDEVICELIST);
					}
				} catch (UnknownHostException e) {
					Log.d(TAG, "AutoSearchThread run 1 " + e.getMessage());
					continue;
				} catch (SocketException e) {
					Log.d(TAG, "AutoSearchThread run 2 " + e.getMessage());
					continue;
				} catch (IOException e) {
					Log.d(TAG, "AutoSearchThread run 3 " + e.getMessage());
					continue;
				} finally {
					datagramSocket.close();
					datagramSocket = null;
					Message message = handler.obtainMessage();
					message.what = Constants.UPDATEAUTOSEARCH;
					message.arg1 = (int)(i*per);
					handler.sendMessage(message);
				}
			}
		}
	}
}
