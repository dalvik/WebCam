package com.iped.ipcam.gui;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.iped.ipcam.utils.CamCmdListHelper;
import com.iped.ipcam.utils.Constants;

/**
 * H.264的功能分为两层，
 * 视频编码层(VCL，Video Coding Layer)
 * 网络提取层(NAL，Network Abstraction Layer)
 * 
 * 在VCL和NAL之间定义了一个基于分组方式的接口，打包和相应的信令属于NAL的一部分。
 * 	   这样，高效率编码和网络适应性的任务分别由VCL和NAL来完成。VCL数据是编码处理后的输出，它表示被压缩编码后的视频数据序列。
 * 	   在VCL数据传输和存储之前，这些编码的VCL数据先被映射或封装进NAL单元中。
     VCL包括基于块的运动补偿、混合编码和一些新特性。
     NAL负责针对下层网络的特性对数据进行封装，包括成帧、发信号给逻辑信道、利用同步信息等。
     NAL从VCL获得数据，包括头信息、段结构信息和实际载荷，NAL的任务就是正确地将它们映射到传输协议上。
     NAL下面是各种具体的协议，
             如H.323、H.324、RTP/UDP/IP等。NAL层的引入大大提高了H.264适应复杂信道的能力。 
 * @author Administrator
 *
 */
public class CamVideoH264 extends Activity {
	
	private boolean flag = true;

	//private VideoView videoView = null;
	
	private MyVideoView myVideoView = null;
	
	private int screenWidth = 0;
	
	private int screenHeight = 0;
	
	private IpPlayReceiver ipPlayReceiver = new IpPlayReceiver();
	
	private Button leftUpButton = null;
	
	private ControlPanel rightControlPanel = null;
	
	private Thread thread = null;
	
	private static WakeLock mWakeLock;
	
	private ProgressDialog m_Dialog = null;
	
	public static String currIpAddress = null;
	
	private String TAG = "CamVideoH264";

	private Handler mHandler = new Handler() {
		public void handleMessage(android.os.Message msg) {
			//initThread();
			switch (msg.what) {
			case Constants.CONNECTTING:
				startThread();
				break;
			case Constants.SHOWCONNDIALOG:
				String tem = (String) msg.obj;
				Log.d(TAG, "tem =" + tem + " playstatus= " +  myVideoView.getPlayStatus());
				if(currIpAddress != null && currIpAddress.contains(tem) && !myVideoView.getPlayStatus()) {
					Log.d(TAG, "playStatus =" +  myVideoView.getPlayStatus());
					//return;
				}
				currIpAddress = tem;
				showProgressDlg();
				startThread();
				break;
			case Constants.HIDECONNDIALOG:
				hideProgressDlg();
				break;
			case Constants.CONNECTERROR:
				Toast.makeText(CamVideoH264.this, getResources().getString(R.string.connection_error), Toast.LENGTH_SHORT).show();
				break;
			default:
				break;
			}
		};
	};
	
	private void startThread() {
		if(thread != null && thread.isAlive()) {
			try {
				thread.join(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		myVideoView.onStart();
		thread = new Thread(myVideoView);
		thread.start();
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        screenWidth = dm.widthPixels;
        screenHeight = dm.heightPixels;
        
        setContentView(R.layout.pre_videoview);
        myVideoView = (MyVideoView) findViewById(R.id.videoview);
        myVideoView.init(mHandler);
        LinearLayout layout = (LinearLayout) findViewById(R.id.container);
        
        LayoutInflater factory = LayoutInflater.from(this);
        View view = factory.inflate(R.layout.reight_menu, null);
		rightControlPanel = new ControlPanel(this, myVideoView,  230, LayoutParams.FILL_PARENT);
		layout.addView(rightControlPanel);
		rightControlPanel.fillPanelContainer(view);
		registerReceiver(ipPlayReceiver, new IntentFilter(Constants.ACTION_IPPLAY));
		PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "DoNotDimScreen");
		if(mWakeLock.isHeld() == false) {
	       mWakeLock.acquire();
	    }
	}

	@Override
	protected void onResume() {
		super.onResume();
		leftUpButton = (Button) findViewById(R.id.left_up);
		if(leftUpButton != null) {
			leftUpButton.measure(0, 0);
			rightControlPanel.updateControlView(leftUpButton.getMeasuredWidth() * 3);
		}
	}
	
	@Override
	protected void onStop() {
		super.onStop();
		myVideoView.onStop();
		dismissProgressDlg();
		unregisterReceiver(ipPlayReceiver);
		if(thread != null && !thread.isAlive()) {
			try {
				thread.join(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			thread = null;
		}
	}

	@Override
    protected void onDestroy() {
    	super.onDestroy();
    	 if(mWakeLock.isHeld() == true) {
    		 mWakeLock.release();
         }
    }
	
	private void showProgressDlg() {
		if(m_Dialog == null) {
			m_Dialog = new ProgressDialog(CamVideoH264.this);
			m_Dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
			m_Dialog.setCancelable(false);
			m_Dialog.setMessage(getResources().getText(R.string.connection));
		}
		m_Dialog.show();
	}
	
	private void hideProgressDlg() {
		if(m_Dialog != null && m_Dialog.isShowing()) {
			m_Dialog.hide();
		}
	}
	
	private void dismissProgressDlg() {
		if(m_Dialog != null) {
			m_Dialog.dismiss();
		}
	}
	
	private class IpPlayReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			if(intent != null) {
				String ip = intent.getStringExtra("IPPLAY");
				if(ip != null && ip.length()>0) {
					Log.d(TAG, "receive ip =" +  ip);
					Message msg = mHandler.obtainMessage();
					msg.obj = ip;
					msg.what = Constants.SHOWCONNDIALOG;
					mHandler.sendMessage(msg);
				}
			}
		}
		
	}
	
	private class QueryDeviceThread implements Runnable {

		private byte[] bufTemp = new byte[Constants.COMMNICATEBUFFERSIZE];
		
		@Override
		public void run() {
			System.out.println("started..");
			DatagramSocket datagramSocket = null;
			try {
				datagramSocket = new DatagramSocket();
				datagramSocket.setSoTimeout(600);
				byte [] tem = CamCmdListHelper.QueryCmd_Online.getBytes();
				System.arraycopy(tem, 0, bufTemp, 0, tem.length);
				System.out.println(InetAddress.getByName("192.168.1.211"));
				DatagramPacket packet = new DatagramPacket(tem, tem.length, InetAddress.getByName("192.168.1.141"), Constants.UDPPORT); 
				datagramSocket.send(packet);
				System.out.println("send..");
			} catch (SocketException e) {
				e.printStackTrace();
				System.out.println(e.getMessage());
			} catch (UnknownHostException e) {
				e.printStackTrace();
				System.out.println(e.getMessage());
			} catch (IOException e) {
				e.printStackTrace();
				System.out.println(e.getMessage());
			}
			DatagramPacket dp = new DatagramPacket(bufTemp, bufTemp.length);
			try {
				datagramSocket.receive(dp);
				String info = new String(bufTemp);
				System.out.println("receive inof = : " + info);
			} catch (IOException e) {
				e.printStackTrace();
				System.out.println("----" + e.getLocalizedMessage());
			}
			System.out.println("over..");
		}
		
	}
	
	private class SocketThread implements Runnable {
		private Socket socket = null;
		private DataInputStream dis = null;
		/**
		 -   char[] head = {0,0,0,1,0xc}; 00 00 00 00 67           5个字节
		 -   char packageSequenceNumber[8],0,1,2,3,4....不重复              8个字节
		 报      char startTimeStamp[14] YYYYMMDDHHMMSS                14个字节
		 头      char lastTimeStamp[14]  开始为111111                  14个字节  
		 -   char frameRateUS[8]                                   8个字节
		 -   char frameWidth[4]                                    4个字节
		 -   char frameHeight[4]                                   4个字节
		 数据包头长：                                                                                                                              57个字节
		 数据内容   char rowData[]                                    余下为数据
		00 00 00 01 67
		42 00 1e ab 40 58 09 32
		00 00 00 01 68 ce 38 80 00 00 00 01 65 88
		82
		 */
		@Override
		public void run() {
			try {
				SocketAddress socketAddress = new InetSocketAddress("192.168.1.121", 1234);
				int i = 0;
				byte[] b = new byte[512];
				socket = new Socket();
				socket.connect(socketAddress, 15000);
				dis = new DataInputStream(socket.getInputStream());
				String temp = "";
				FileOutputStream fos = new FileOutputStream(new File("/sdcard/"+ System.currentTimeMillis() + ".h264"));
				while ((i = dis.read(b)) != -1 && flag) {
					temp = new String(b, 0, i, "ISO-8859-1");
					fos.write(b, 0, i);
					fos.flush();
				}
			} catch (UnknownHostException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}

