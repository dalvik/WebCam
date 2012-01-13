package com.iped.ipcam.engine;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.os.Handler;
import android.util.Log;

import com.iped.ipcam.pojo.Video;
import com.iped.ipcam.utils.CamCmdListHelper;
import com.iped.ipcam.utils.Constants;
import com.iped.ipcam.utils.DateUtil;
import com.iped.ipcam.utils.PackageUtil;

public class VideoManagerImp implements IVideoManager {

	private VideoSearchThread videoSearchThread = null;
	
	private List<Video> videoList = new ArrayList<Video>();
	
	private String deviceName;
	
	private Date start;
	
	private Date end;
	
	private String  TAG = "VideoManagerImp";
	
	
	public VideoManagerImp() {
		
	}
	
	@Override
	public List<Video> getVideoList() {
		return videoList;
	}

	@Override
	public void videoSearchInit(String device, Date start, Date end) {
		this.deviceName = device;
		this.start = start;
		this.end = end;
	}
	
	@Override
	public void startSearchThread(Handler handler) {
		if(videoSearchThread != null && !videoSearchThread.isAlive()) {
			try {
				videoSearchThread.join(10);
				videoSearchThread = null;
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		videoSearchThread = new VideoSearchThread(handler);
		videoSearchThread.start();
	}
	
	@Override
	public boolean deleteFiles(Handler handler,  String startIndex, String endIndex, String ip) {
		new Thread(new DeleteFileThread(handler, startIndex, endIndex, ip)).start();
		return false;
	}
	
	@Override
	public boolean removeVideoByIndex(String index) {
		int l = videoList.size();
		for (int i = 0; i < l; i++) {
			Video video = videoList.get(i);
			if(video.getIndex().equalsIgnoreCase(index)) {
				videoList.remove(i);
				return true;
			}
		}
		return false;
	}
	
	@Override
	public void clearVideoList() {
		videoList.clear();
	}
	

	class VideoSearchThread extends Thread {
		
		private Handler handler;
		
		public VideoSearchThread(Handler handler) {
			this.handler = handler;
		}
		
		@Override
		public void run() {
			byte [] buffTemp = new byte[Constants.COMMNICATEBUFFERSIZE];
			byte [] tem = CamCmdListHelper.GetCmd_NetFiles.getBytes();
			DatagramSocket datagramSocket = null;
			//00068000:00fff5b2:20120104165801-20120104170649
			try {
				datagramSocket = new DatagramSocket();
				datagramSocket.setSoTimeout(Constants.VIDEOSEARCHTIMEOUT);
				System.arraycopy(tem, 0, buffTemp, 0, tem.length);
				System.out.println(deviceName);
				DatagramPacket datagramPacket = new DatagramPacket(buffTemp, buffTemp.length, InetAddress.getByName(deviceName), 60000);
				datagramSocket.send(datagramPacket);
				DatagramPacket rece = new DatagramPacket(buffTemp, buffTemp.length);
				datagramSocket.receive(rece);
				splitFilesInfoFromBuf(PackageUtil.deleteZero(buffTemp));
				updateList();
			} catch (SocketException e) {
				e.printStackTrace();
			} catch (UnknownHostException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				if(datagramSocket != null) {
					datagramSocket.close();
				}
				handler.sendEmptyMessage(Constants.DISSMISVIDEOSEARCHDLG);
			}
		}
		
		
		//00000000:00fff094:20120104150759-20120104151643
		public void splitFilesInfoFromBuf(byte[]files) {
			String s = new String(files);
			System.out.println(s);
			String[] temp = s.split("\n");
			for(String t:temp) {
				analyFileFromString(t);
			}
		}
		
		// 19 + 28 = 47
		public void analyFileFromString(String s) {
			int length = s.length();
			if(s== null || length<47) {
				return;
			}
			String index = s.substring(0, 8);
			String fileLength = s.substring(9, 17);
			String start = s.substring(18, 32);
			String end = s.substring(33, length);
			if((fileLength != null && fileLength.trim().length()<=0) || (end != null && end.trim().length()<=0)) {
				Video video = new Video(index, deviceName, start, end, fileLength, deviceName);
				videoList.add(video);
				return;
			} 
			if(!checkDate(DateUtil.formatTimeToDate(start), DateUtil.formatTimeToDate(end))) {
				return ;
			}
			//int i = Integer.parseInt(index, 16);
			//int j = Integer.parseInt(fileLength, 16);
			//System.out.println("index=" + index + " fileLenght=" + fileLength + " start=" + start + " end=" + end);
			Video video = new Video(index, deviceName, start, end, fileLength, deviceName);
			videoList.add(video);
		}
		
		private boolean checkDate(Date d1, Date d2) {
			if(start.before(d1) && end.after(d2)) {
				return true;
			}
			return false;
		}
		
		public void updateList() {
			handler.sendEmptyMessage(Constants.UPDATEVIDEOLIST);
		}
	}

	private class DeleteFileThread implements Runnable {

		private Handler handler;
		
		private String startIndex;
		
		private String endIndex;
		
		private String ip;
		
		public DeleteFileThread(Handler handler, String startIndex, String endIndex, String ip) {
			this.handler = handler;
			this.startIndex = startIndex;
			this.endIndex = endIndex;
			this.ip = ip;
		}
		
		@Override
		public void run() {
			byte [] tem = (CamCmdListHelper.DelCmd_DeleteFiles+ startIndex + endIndex).getBytes();
			DatagramSocket datagramSocket = null;
			try {
				datagramSocket = new DatagramSocket();
				datagramSocket.setSoTimeout(Constants.VIDEOSEARCHTIMEOUT);
				DatagramPacket datagramPacket = new DatagramPacket(tem, tem.length, InetAddress.getByName(ip), Constants.UDPPORT);
				datagramSocket.send(datagramPacket);
				DatagramPacket rece = new DatagramPacket(tem, tem.length);
				datagramSocket.receive(rece);
				String info = new String(tem);
				Log.d(TAG, "Receive inof //////////////" + info);
				if(info != null && info.toUpperCase().contains("OK")) {
					if(!startIndex.equals(endIndex.trim())) {
						clearVideoList();
						handler.sendEmptyMessage(Constants.DELETEFILESUCCESS);
					} else {
						boolean flag = removeVideoByIndex(startIndex);
						if(flag) {
							handler.sendEmptyMessage(Constants.DELETEFILESUCCESS);
						} else {
							handler.sendEmptyMessage(Constants.DELETEFILEERROR);
						}
					}
			   }
			} catch (IOException e) {
				handler.sendEmptyMessage(Constants.DELETEFILEERROR);
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
}
