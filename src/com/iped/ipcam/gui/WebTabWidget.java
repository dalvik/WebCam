package com.iped.ipcam.gui;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

import android.app.ActivityManager;
import android.app.TabActivity;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.widget.TabHost;
import android.widget.TabWidget;

import com.iped.ipcam.engine.CamMagFactory;
import com.iped.ipcam.engine.ICamManager;
import com.iped.ipcam.utils.Constants;
import com.iped.ipcam.utils.ThroughNetUtil;

public class WebTabWidget extends TabActivity {

	public static TabHost tabHost = null;
	
	private Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			Bundle bundle = msg.getData();
			if(bundle != null) {
				String ip = "183.128.48.201";//bundle.getString("IPADDRESS");
				int port1 = bundle.getInt("PORT1");
				int port2 = bundle.getInt("PORT2");
				int port3 = bundle.getInt("PORT3");
				System.out.println("rece ip info = " +  ip + " " + port1 + " " + port2 +  " " + port3);
				int l = 2;
				byte[] b = new byte[l];
				try {
					DatagramSocket socket = new DatagramSocket();
					DatagramPacket packet = new DatagramPacket(b, l, InetAddress.getByName(ip), port1);
					socket.send(packet);
					socket.send(packet);
					socket.send(packet);
					System.out.println("port 1 sucess");
				} catch (Exception e) {
					e.printStackTrace();
					return;
				}
				
				try {
					DatagramSocket socket = new DatagramSocket();
					DatagramPacket packet = new DatagramPacket(b, l, InetAddress.getByName(ip), port2);
					socket.send(packet);
					socket.send(packet);
					socket.send(packet);
					System.out.println("port 2 sucess");
				} catch (Exception e) {
					e.printStackTrace();
					return;
				}
				
				try {
					DatagramSocket socket = new DatagramSocket();
					DatagramPacket packet = new DatagramPacket(b, l, InetAddress.getByName(ip), port3);
					socket.send(packet);
					socket.send(packet);
					socket.send(packet);
					System.out.println("port 3 sucess");
				} catch (Exception e) {
					e.printStackTrace();
					return;
				}
			}
			super.handleMessage(msg);
		}
	};
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.web_tab_widget);
		Resources resources = getResources();
		tabHost = getTabHost();
		TabHost.TabSpec tabSpec;
		Intent intent;
		intent = new Intent(this, CamVideoH264.class);
		//intent = new Intent(this, LeftVideoView.class);
		tabSpec = tabHost.newTabSpec(Constants.VIDEOPREVIEW)
		.setIndicator(resources.getString(R.string.default_activity_str)).setContent(intent);
		tabHost.addTab(tabSpec);
		
		intent = new Intent(this, DeviceManager.class);
		tabSpec = tabHost.newTabSpec("DEVICEMANAGER")
		.setIndicator(resources.getString(R.string.device_manager_str)).setContent(intent);
		tabHost.addTab(tabSpec);
		
		intent = new Intent(this, DeviceParamSets.class);
		tabSpec = tabHost.newTabSpec("DEVICEPARAMSETS")
		.setIndicator(resources.getString(R.string.device_params_sets_str)).setContent(intent);
		tabHost.addTab(tabSpec);
		
		intent = new Intent(this, PlayBack.class);
		tabSpec = tabHost.newTabSpec("PALYBACK")
		.setIndicator(resources.getString(R.string.play_back_str)).setContent(intent);
		tabHost.addTab(tabSpec);
		
		intent = new Intent(this, SystemSettings.class);
		tabSpec = tabHost.newTabSpec("SYSTEMSETTINGS")
		.setIndicator(resources.getString(R.string.system_settings_str)).setContent(intent);
		tabHost.addTab(tabSpec);
		tabHost.setCurrentTabByTag("DEVICEMANAGER");
		TabWidget tabWidget = tabHost.getTabWidget();
		int count = tabWidget.getChildCount();
		for(int i = 0; i < count; i++) {
			tabWidget.getChildAt(i).getLayoutParams().height  = tabWidget.getChildAt(i).getLayoutParams().height * 2/3;
			
		}
		/*new Thread(new Runnable() {
			@Override
			public void run() {
				// TODO Auto-generated method stub
				UdtTools.sendFile("/mnt/sdcard/test.amr", "5000");
			}
		}).start();
		UdtTools.recvFile("192.168.1.101", "5000", "/mnt/sdcard/test.amr", "/mnt/sdcard/abcdeeee.amr");
		*/
		new Thread(new ThroughNetUtil(handler)).start();
	}
	
	@Override
	protected void onStop() {
		super.onStop();
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		ICamManager camManager = CamMagFactory.getCamManagerInstance();
		camManager.clearCamList();
		int sdk_Version = android.os.Build.VERSION.SDK_INT;  
		if (sdk_Version >= 8) {  
			Intent startMain = new Intent(Intent.ACTION_MAIN);  
			startMain.addCategory(Intent.CATEGORY_HOME);  
			startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);  
			startActivity(startMain);  
			System.exit(0);  
		} else if (sdk_Version < 8) {  
			ActivityManager activityMgr = (ActivityManager) getSystemService(ACTIVITY_SERVICE);  
			activityMgr.restartPackage(getPackageName());  
		}
	}
}
