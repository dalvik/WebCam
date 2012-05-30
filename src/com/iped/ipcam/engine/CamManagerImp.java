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
import com.iped.ipcam.utils.PackageUtil;

public class CamManagerImp implements ICamManager {

	
	private List<Device> deviceList = new ArrayList<Device>();
	
	//private Thread queryThread = null;
	
	private QueryCamThread queryThread = null;
	
	private int selectIndex = 0;

	private int temp = -1;
	
	private int  count = 1;
	
	private String TAG = "CamManagerImp";
	
	public CamManagerImp() {
		//Device d = new Device("11111", "Ip Camera", "192.168.1.127", Constants.TCPPORT, Constants.UDPPORT, Constants.DEFAULTWAY);
		//deviceList.add(d);
	}
	
	@Override
	public Device addCam(String ip) {
		Device d = new Device(ip,"192.168.1.1","255.255.255.0","dns","dns2");
		d.setDeviceName("IpCam");
		d.setDeviceRemoteCmdPort(Constants.UDPPORT);
		d.setDeviceRemoteVideoPort(Constants.TCPPORT);
		d.setDeviceRemoteAudioPort(Constants.AUDIOPORT);
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
		//deviceOLd.setDeviceName(deviceNew.getDeviceName());
		//deviceOLd.setDeviceIp(deviceNew.getDeviceIp());
		//deviceOLd.setDeviceGateWay(deviceNew.getDeviceGateWay());
		return true;
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
		stopThread();
		queryThread = new QueryCamThread(handler);
		queryThread.setDaemon(true);
		queryThread.start();
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
	
	private void updateDeviceList() {
		queryThread.tes();
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
		
		public void tes() {
			handler.sendEmptyMessage(Constants.UPDATEDEVICELIST);
		}
		
		public void dissmiss() {
			handler.sendEmptyMessage(Constants.UPDATEDEVICELIST);
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
			for(i=1; i<=10; i++) {
				new Thread(new QueryOnline(i*26)).start();
				try {
					Thread.sleep(300);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
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
					//test(String.valueOf(i));
					boolean res = PackageUtil.CMDPackage(CamCmdListHelper.QueryCmd_Online, Constants.DEFAULTSEARCHIP + i, Constants.UDPPORT);
					if(res) {
						synchronized (deviceList) {
							addCam(Constants.DEFAULTSEARCHIP + i);
							updateDeviceList();
						}
					}
					try {
						Thread.sleep(500);
					} catch (InterruptedException e) {
						e.printStackTrace();
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
		
		public void test(String ip) {
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
