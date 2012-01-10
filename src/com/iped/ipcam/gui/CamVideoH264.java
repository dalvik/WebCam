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
 * H.264�Ĺ��ܷ�Ϊ���㣬
 * ��Ƶ�����(VCL��Video Coding Layer)
 * ������ȡ��(NAL��Network Abstraction Layer)
 * 
 * ��VCL��NAL֮�䶨����һ�����ڷ��鷽ʽ�Ľӿڣ��������Ӧ����������NAL��һ���֡�
 * 	   ��������Ч�ʱ����������Ӧ�Ե�����ֱ���VCL��NAL����ɡ�VCL�����Ǳ��봦�������������ʾ��ѹ����������Ƶ�������С�
 * 	   ��VCL���ݴ���ʹ洢֮ǰ����Щ�����VCL�����ȱ�ӳ����װ��NAL��Ԫ�С�
     VCL�������ڿ���˶���������ϱ����һЩ�����ԡ�
     NAL��������²���������Զ����ݽ��з�װ��������֡�����źŸ��߼��ŵ�������ͬ����Ϣ�ȡ�
     NAL��VCL������ݣ�����ͷ��Ϣ���νṹ��Ϣ��ʵ���غɣ�NAL�����������ȷ�ؽ�����ӳ�䵽����Э���ϡ�
     NAL�����Ǹ��־����Э�飬
             ��H.323��H.324��RTP/UDP/IP�ȡ�NAL��������������H.264��Ӧ�����ŵ��������� 
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
		 -   char[] head = {0,0,0,1,0xc}; 00 00 00 00 67           5���ֽ�
		 -   char packageSequenceNumber[8],0,1,2,3,4....���ظ�              8���ֽ�
		 ��      char startTimeStamp[14] YYYYMMDDHHMMSS                14���ֽ�
		 ͷ      char lastTimeStamp[14]  ��ʼΪ111111                  14���ֽ�  
		 -   char frameRateUS[8]                                   8���ֽ�
		 -   char frameWidth[4]                                    4���ֽ�
		 -   char frameHeight[4]                                   4���ֽ�
		 ���ݰ�ͷ����                                                                                                                              57���ֽ�
		 ��������   char rowData[]                                    ����Ϊ����
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

