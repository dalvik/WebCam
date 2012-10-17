package com.iped.ipcam.engine;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

import com.iped.ipcam.gui.R;
import com.iped.ipcam.gui.UdtTools;
import com.iped.ipcam.pojo.Device;
import com.iped.ipcam.pojo.Video;
import com.iped.ipcam.utils.CamCmdListHelper;
import com.iped.ipcam.utils.Constants;
import com.iped.ipcam.utils.DateUtil;
import com.iped.ipcam.utils.ErrorCode;
import com.iped.ipcam.utils.PackageUtil;
import com.iped.ipcam.utils.RandomUtil;

public class VideoManagerImp implements IVideoManager {

	private VideoSearchThread videoSearchThread = null;
	
	private List<Video> videoList = new ArrayList<Video>();
	
	private Device device;
	
	private Date start;
	
	private Date end;
	
	private String id;
	
	private String  TAG = "VideoManagerImp";
	
	
	public VideoManagerImp() {
		
	}
	
	@Override
	public List<Video> getVideoList() {
		return videoList;
	}

	@Override
	public void videoSearchInit(Device device, Date start, Date end) {
		this.device = device;
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
			id = device.getDeviceID();
			int res = UdtTools.checkCmdSocketEnable(id);
			Log.d(TAG, "### UdtTools checkCmdSocketEnable result = " + res + " device id = " + id);
			if(res>0) { // socket is valid
				//handler.sendEmptyMessage(Constants.WEB_CAM_HIDE_CHECK_PWD_DLG_MSG);
				//Intent intent = new Intent(WebCamActions.QUERY_CONFIG_ACTION);
				//intent.setType(WebCamActions.QUERY_CONFIG_MINITYPE);
				HandlerThread handlerThread = new HandlerThread("test1");
				handlerThread.start();
				Handler mHandler = new Handler(handlerThread.getLooper());
				mHandler.post(fetchVideoRunnable);
			}else {
				String random = RandomUtil.generalRandom();
				//Log.d(TAG, "random = " + random);
				int result = UdtTools.monitorCmdSocket(id, random);
				Log.d(TAG, "monitor result = " + result);
				analyseResult(result, device);
			}
		}
		
		//00000000:00fff094:20120104150759-20120104151643
		public void splitFilesInfoFromBuf(byte[]files) {
			String s = new String(files);
			//System.out.println(s);
			String[] temp = s.split("\n");
			for(String t:temp) {
				//Log.d(TAG, "### temp = " + t);
				analyFileFromString(t);
			}
		}
		
		// 19 + 28 = 47
		public void analyFileFromString(String s) {
			int length = s.length();
			if(s== null || length<47) {
				Log.d(TAG, "### video ="  + s);
				return;
			}
			String index = s.substring(0, 8);
			String fileLength = s.substring(9, 17);
			String start = s.substring(18, 32);
			String end = s.substring(33, length);
			//Log.d(TAG, "### video ="  + "index=" + index + " fileLenght=" + fileLength + " start=" + start + " end=" + end);
			if((fileLength != null && fileLength.trim().length()<=0) || (end != null && end.trim().length()<=0)) {
				Video video = new Video(index, device.getDeviceName(), start, end, fileLength, id);
				videoList.add(video);
				return;
			} 
			/*if(!checkDate(DateUtil.formatTimeToDate(start), DateUtil.formatTimeToDate(end))) {
				Log.d(TAG, "########## =====");
				return ;
			}*/
			//int i = Integer.parseInt(index, 16);
			//int j = Integer.parseInt(fileLength, 16);
			Video video = new Video(index, device.getDeviceName(), start, end, fileLength, id);
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
		
		private void analyseResult(int result, Device device) {
			switch (result) {
			case ErrorCode.STUN_ERR_INTERNAL:
				sendMessage(Constants.DISSMISVIDEOSEARCHDLG, R.string.webcam_error_code_internel);
				return;
			case ErrorCode.STUN_ERR_SERVER:
				sendMessage(Constants.DISSMISVIDEOSEARCHDLG, R.string.webcam_error_code_server_not_reached);
				return;
			case ErrorCode.STUN_ERR_TIMEOUT:
				sendMessage(Constants.DISSMISVIDEOSEARCHDLG, R.string.webcam_error_code_timeout);
				return;
			case ErrorCode.STUN_ERR_INVALIDID:
				sendMessage(Constants.DISSMISVIDEOSEARCHDLG, R.string.webcam_error_code_unlegal);
				return;
			case ErrorCode.STUN_ERR_CONNECT:
				sendMessage(Constants.DISSMISVIDEOSEARCHDLG, R.string.webcam_error_code_connect_error);
				return;
			case ErrorCode.STUN_ERR_BIND:
				sendMessage(Constants.DISSMISVIDEOSEARCHDLG, R.string.webcam_error_code_bind_error);
				return;
			default:
				break;
			}
			HandlerThread handlerThread = new HandlerThread("test1");
			handlerThread.start();
			Handler mHandler = new Handler(handlerThread.getLooper());
			mHandler.post(fetchVideoRunnable);
		}
		
		private Runnable fetchVideoRunnable = new Runnable() {

			@Override
			public void run() {
				int bufLength = Constants.COMMNICATEBUFFERSIZE*100;
				byte [] buffTemp = new byte[bufLength];
				String tem = CamCmdListHelper.GetCmd_NetFiles;
				String id = device.getDeviceID();
				int res = UdtTools.sendCmdMsgById(id, tem, tem.length());
				//System.out.println("res=" + res);
				res = UdtTools.recvCmdMsgById(id, buffTemp, bufLength);
				if(res > 0) {
					byte[] recv = new byte[res];
					System.arraycopy(buffTemp, 4, recv, 0, res);
					Log.d(TAG, "### length =  " + res + " content = " + new String(recv));
					//System.out.println("### " + res + " " + new String(recv));
					splitFilesInfoFromBuf(PackageUtil.deleteZero(recv));
					updateList();
					handler.sendEmptyMessage(Constants.DISSMISVIDEOSEARCHDLG);
				}else {
					sendMessage(Constants.DISSMISVIDEOSEARCHDLG, R.string.play_back_auto_search_error_str);
				}
				//00068000:00fff5b2:20120104165801-20120104170649
			}
		};
		
		
		private void sendMessage(int msgId, int strId) {
			Message msg = handler.obtainMessage();
			msg.arg1 = strId;
			msg.what = msgId;
			handler.sendMessage(msg);
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
			id = device.getDeviceID();
			int res = UdtTools.checkCmdSocketEnable(id);
			Log.d(TAG, "UdtTools checkCmdSocketEnable result = " + res);
			if(res>0) { // socket is valid
				HandlerThread handlerThread = new HandlerThread("test1");
				handlerThread.start();
				Handler mHandler = new Handler(handlerThread.getLooper());
				mHandler.post(deleteVideoRunnable);
			}else {
				String random = RandomUtil.generalRandom();
				//Log.d(TAG, "random = " + random);
				int result = UdtTools.monitorCmdSocket(id, random);
				Log.d(TAG, "monitor result = " + result);
				analyseResult(result, device);
			}
			
			/*byte [] tem = (CamCmdListHelper.DelCmd_DeleteFiles+ startIndex + endIndex).getBytes();
			DatagramSocket datagramSocket = null;
			try {
				datagramSocket = new DatagramSocket();
				datagramSocket.setSoTimeout(Constants.VIDEOSEARCHTIMEOUT);
				DatagramPacket datagramPacket = new DatagramPacket(tem, tem.length, InetAddress.getByName(ip), Constants.LOCALCMDPORT);
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
			}*/
		}
		
		private void analyseResult(int result, Device device) {
			switch (result) {
			case ErrorCode.STUN_ERR_INTERNAL:
				sendMessage(Constants.DISSMISVIDEOSEARCHDLG,R.string.webcam_error_code_internel);
				return;
			case ErrorCode.STUN_ERR_SERVER:
				sendMessage(Constants.DISSMISVIDEOSEARCHDLG,R.string.webcam_error_code_server_not_reached);
				return;
			case ErrorCode.STUN_ERR_TIMEOUT:
				sendMessage(Constants.DISSMISVIDEOSEARCHDLG,R.string.webcam_error_code_timeout);
				return;
			case ErrorCode.STUN_ERR_INVALIDID:
				sendMessage(Constants.DISSMISVIDEOSEARCHDLG,R.string.webcam_error_code_unlegal);
				return;
			case ErrorCode.STUN_ERR_CONNECT:
				sendMessage(Constants.DISSMISVIDEOSEARCHDLG,R.string.webcam_error_code_connect_error);
				return;
			case ErrorCode.STUN_ERR_BIND:
				sendMessage(Constants.DISSMISVIDEOSEARCHDLG,R.string.webcam_error_code_bind_error);
				return;
			default:
				break;
			}
			HandlerThread handlerThread = new HandlerThread("test1");
			handlerThread.start();
			Handler mHandler = new Handler(handlerThread.getLooper());
			mHandler.post(deleteVideoRunnable);
		}
		
		private void sendMessage(int msgId, int strId) {
			Message msg = handler.obtainMessage();
			msg.arg1 = strId;
			msg.what = msgId;
			handler.sendMessage(msg);
		}
		
		private Runnable deleteVideoRunnable = new Runnable() {

			@Override
			public void run() {
				String tem = CamCmdListHelper.DelCmd_DeleteFiles + startIndex + endIndex;
				String id = device.getDeviceID();
				int res = UdtTools.sendCmdMsgById(id, tem, tem.length());
				if(res > 0) {
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
				}else {
					sendMessage(Constants.DISSMISVIDEOSEARCHDLG, R.string.video_delete_error);
				}
			}
		};
		
		
	}
}
