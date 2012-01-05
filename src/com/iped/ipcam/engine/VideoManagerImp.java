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

import com.iped.ipcam.pojo.Video;
import com.iped.ipcam.utils.CamCmdListHelper;
import com.iped.ipcam.utils.Constants;

public class VideoManagerImp implements IVideoManager {

	private VideoSearchThread videoSearchThread = null;
	
	private List<Video> videoList = new ArrayList<Video>();
	
	public VideoManagerImp() {
		
	}
	
	@Override
	public List<Video> getVideoList() {
		return videoList;
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

	class VideoSearchThread extends Thread {
		
		private Handler handler;
		
		public VideoSearchThread(Handler handler) {
			this.handler = handler;
		}
		
		@Override
		public void run() {
			byte [] buffTemp = new byte[Constants.COMMNICATEBUFFERSIZE];
			byte [] tem = CamCmdListHelper.GetCmd_NetFiles.getBytes();
			DatagramSocket datagramSocket;
			//00068000:00fff5b2:20120104165801-20120104170649
			try {
				datagramSocket = new DatagramSocket();
				datagramSocket.setSoTimeout(Constants.VIDEOSEARCHTIMEOUT);
				System.arraycopy(tem, 0, buffTemp, 0, tem.length);
				DatagramPacket datagramPacket = new DatagramPacket(buffTemp, buffTemp.length, InetAddress.getByName("192.168.1.211"), 60000);
				datagramSocket.send(datagramPacket);
				System.out.println("sended, and wait rece....");
				DatagramPacket rece = new DatagramPacket(buffTemp, buffTemp.length);
				datagramSocket.receive(rece);
				splitFilesInfoFromBuf(deleteZero(buffTemp));
				updateList();
			} catch (SocketException e) {
				e.printStackTrace();
			} catch (UnknownHostException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				handler.sendEmptyMessage(Constants.DISSMISVIDEOSEARCHDLG);
			}
		}
		
		private byte[] deleteZero(byte[]files) {
			int index = 0;
			int length = files.length;
			if(length<8) {
				return files;
			}
			for(int i=8;i<length;i++){
				if(files[i] == 0) {
					break;
				}
				index = i;
			}
			byte[] temp = new byte[index-8];
			System.arraycopy(files, 8, temp, 0, index-8);
			return temp;
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
			int i = Integer.parseInt(index, 16);
			int l = Integer.parseInt(fileLength, 16);
			//System.out.println("index=" + index + " fileLenght=" + fileLength + " start=" + start + " end=" + end);
			Video video = new Video(i, l, "test", start, end);
			videoList.add(video);
		}
		
		
		public void updateList() {
			handler.sendEmptyMessage(Constants.UPDATEVIDEOLIST);
		}
	}
}
