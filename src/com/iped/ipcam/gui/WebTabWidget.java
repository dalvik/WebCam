package com.iped.ipcam.gui;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;

import android.app.ActivityManager;
import android.app.TabActivity;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.widget.TabHost;
import android.widget.TabWidget;

import com.iped.ipcam.engine.CamMagFactory;
import com.iped.ipcam.engine.ICamManager;
import com.iped.ipcam.utils.Constants;

public class WebTabWidget extends TabActivity {

	public static TabHost tabHost = null;
	
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
		
		/*intent = new Intent(this, DeviceParamSets.class);
		tabSpec = tabHost.newTabSpec("DEVICEPARAMSETS")
		.setIndicator(resources.getString(R.string.device_params_sets_str)).setContent(intent);
		tabHost.addTab(tabSpec);*/
		
		intent = new Intent(this, PlayBack.class);
		tabSpec = tabHost.newTabSpec("PALYBACK")
		.setIndicator(resources.getString(R.string.play_back_str)).setContent(intent);
		tabHost.addTab(tabSpec);
		
		intent = new Intent(this, SystemSettings.class);
		tabSpec = tabHost.newTabSpec("SYSTEMSETTINGS")
		.setIndicator(resources.getString(R.string.system_settings_str)).setContent(intent);
		tabHost.addTab(tabSpec);
		//tabHost.setCurrentTabByTag("DEVICEMANAGER");
		tabHost.setCurrentTab(1);
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
		/* final String SetCmd_StartVideo_Tcp = "set_transport_type:tcp:PSWD=q"+ "\0";
		
		 final String ip = "192.168.1.107";
		 
		new Thread() {
			public void run() {
				for(;;) {
					Socket socket = new Socket();
					byte [] data = SetCmd_StartVideo_Tcp.getBytes();
					DatagramSocket cmdSocket;
					try {
						cmdSocket = new DatagramSocket();
						cmdSocket.setSoTimeout(1000);
						DatagramPacket datagramPacket = new DatagramPacket(data, data.length, InetAddress.getByName(ip), 60000);
						cmdSocket.send(datagramPacket);
						//byte[] b = new byte[100];
						//DatagramPacket dp = new DatagramPacket(b, b.length);
						//cmdSocket.receive(dp);
						//int l = dp.getLength();
						//System.out.println("rece =====> " + new String(b,0,l));
						//DatagramPacket rece = new DatagramPacket(buffTemp, buffTemp.length);
						SocketAddress socketAddress = new InetSocketAddress(ip, 1234);
						socket.setSoTimeout(10000);
						socket.connect(socketAddress);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} finally {
						try {
							Thread.sleep(16000);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}
			}
		}.start();*/
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
