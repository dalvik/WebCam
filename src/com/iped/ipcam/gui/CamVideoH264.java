package com.iped.ipcam.gui;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.iped.ipcam.exception.CamManagerException;
import com.iped.ipcam.pojo.Device;
import com.iped.ipcam.utils.CamCmdListHelper;
import com.iped.ipcam.utils.Constants;
import com.iped.ipcam.utils.PackageUtil;
import com.iped.ipcam.utils.ThroughNetUtil;
import com.iped.ipcam.utils.ToastUtils;

/**
     H.264的功能分为两层，
             视频编码层(VCL，Video Coding Layer)
             网络提取层(NAL，Network Abstraction Layer)
             在VCL和NAL之间定义了一个基于分组方式的接口，打包和相应的信令属于NAL的一部分。
  	   这样，高效率编码和网络适应性的任务分别由VCL和NAL来完成。VCL数据是编码处理后的输出，它表示被压缩编码后的视频数据序列。
  	   在VCL数据传输和存储之前，这些编码的VCL数据先被映射或封装进NAL单元中。
     VCL包括基于块的运动补偿、混合编码和一些新特性。
     NAL负责针对下层网络的特性对数据进行封装，包括成帧、发信号给逻辑信道、利用同步信息等。
     NAL从VCL获得数据，包括头信息、段结构信息和实际载荷，NAL的任务就是正确地将它们映射到传输协议上。
     NAL下面是各种具体的协议，
             如H.323、H.324、RTP/UDP/IP等。NAL层的引入大大提高了H.264适应复杂信道的能力。 
 * @author Administrator
 *
 */
public class CamVideoH264 extends Activity implements OnClickListener {
	
	private boolean flag = true;

	//private VideoView videoView = null;
	
	private MyVideoView myVideoView = null;
	
	private int screenWidth = 0;
	
	private int screenHeight = 0;
	
	private IpPlayReceiver ipPlayReceiver = null;
	
	private Button leftUpButton = null;
	
	private ControlPanel rightControlPanel = null;
	
	private Thread thread = null;
	
	private static WakeLock mWakeLock;
	
	private ProgressDialog m_Dialog = null;
	
	public static String currIpAddress = null;
	
	public static int currPort = 1234;
	
	public static int port1 = 1234;
	
	public static int port2 = -1;
	
	public static int port3 = -1;
	
	private Device device = null;
	
	private static ThroughNetUtil netUtil = null;
	
	private String TAG = "CamVideoH264";
	
	private Handler mHandler = new Handler() {
		public void handleMessage(android.os.Message msg) {
			//initThread();
			switch (msg.what) {
			case Constants.CONNECTTING:
				startThread();
				break;
			case Constants.SHOWCONNDIALOG:
				showProgressDlg();
				//startThread();
				break;
			case Constants.HIDECONNDIALOG:
				hideProgressDlg();
				break;
			case Constants.CONNECTERROR:
				Toast.makeText(CamVideoH264.this, getResources().getString(R.string.connection_error), Toast.LENGTH_SHORT).show();
				break;
			case Constants.SENDGETTHREEPORTMSG:
				Bundle bundle = msg.getData();
				if(bundle != null) {
					currIpAddress = bundle.getString("IPADDRESS");  //"183.128.48.201";
					port1 = bundle.getInt("PORT1");
					port2 = bundle.getInt("PORT2");
					currPort = port2;
					port3 = bundle.getInt("PORT3");
					//System.out.println("rece ip info = " +  currIpAddress + " " + port1 + " " + port2 +  " " + port3);
					int l = 2;
					byte[] b = new byte[l];
					try {
						DatagramSocket socket1 = netUtil.getPort1();
						if(socket1 != null) {
							DatagramPacket packet = new DatagramPacket(b, l, InetAddress.getByName(currIpAddress), port1);
							socket1.send(packet);
							socket1.send(packet);
							socket1.send(packet);
							System.out.println("port 1 sucess");
						}
						DatagramSocket socket2 = netUtil.getPort2();
						if(socket2 != null) {
							DatagramPacket packet2 = new DatagramPacket(b, l, InetAddress.getByName(currIpAddress), port2);
							socket2.send(packet2);
							socket2.send(packet2);
							socket2.send(packet2);
							System.out.println("port 2 sucess");
						}
						DatagramSocket socket3 = netUtil.getPort3();
						if(socket3 != null) {
							DatagramPacket packet3 = new DatagramPacket(b, l, InetAddress.getByName(currIpAddress), port3);
							socket3.send(packet3);
							socket3.send(packet3);
							socket3.send(packet3);
							System.out.println("port 3 sucess");
							
						}
						device.setUnDefine1(currIpAddress);
						device.setDeviceRemoteCmdPort(port1);
						device.setDeviceRemoteVideoPort(port2);
						device.setDeviceRemoteAudioPort(port3);
					} catch (Exception e) {
						Log.d(TAG, "----> send port " + e.getLocalizedMessage());
					} finally{
						mHandler.sendEmptyMessage(Constants.CONNECTTING);
					}
				}
				break;
			case Constants.SENDGETTHREEPORTTIMOUTMSG:
				ToastUtils.showToast(CamVideoH264.this, R.string.connection_error);
				hideProgressDlg();
				break;
			default:
				break;
			}
		};
	};
	
	private void startThread() {
		myVideoView.onStop();
		myVideoView.setDevice(device);
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
        ipPlayReceiver = new IpPlayReceiver();
        registerReceiver(ipPlayReceiver, new IntentFilter(Constants.ACTION_IPPLAY));
        myVideoView = (MyVideoView) findViewById(R.id.videoview);
        myVideoView.init(mHandler,screenWidth, screenHeight);
        LinearLayout layout = (LinearLayout) findViewById(R.id.container);
        
        LayoutInflater factory = LayoutInflater.from(this);
        View view = factory.inflate(R.layout.reight_menu, null);
        registerListener(view);
		rightControlPanel = new ControlPanel(this, myVideoView,  230, LayoutParams.FILL_PARENT);
		layout.addView(rightControlPanel);
		rightControlPanel.fillPanelContainer(view);
		PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "DoNotDimScreen");
		if(mWakeLock.isHeld() == false) {
	       mWakeLock.acquire();
	    }
	}

	private void registerListener(View view) {
		view.findViewById(R.id.left_up).setOnClickListener(this);
		view.findViewById(R.id.mid_up).setOnClickListener(this);
		view.findViewById(R.id.right_up).setOnClickListener(this);
		view.findViewById(R.id.left).setOnClickListener(this);
		view.findViewById(R.id.mid).setOnClickListener(this);
		view.findViewById(R.id.right).setOnClickListener(this);
		view.findViewById(R.id.left_down).setOnClickListener(this);
		view.findViewById(R.id.mid_down).setOnClickListener(this);
		view.findViewById(R.id.right_down).setOnClickListener(this);
		view.findViewById(R.id.minus_zoom).setOnClickListener(this);
		view.findViewById(R.id.add_zoom).setOnClickListener(this);
		view.findViewById(R.id.minus_foucs).setOnClickListener(this);
		view.findViewById(R.id.add_foucs).setOnClickListener(this);
		view.findViewById(R.id.minus_apertrue).setOnClickListener(this);
		view.findViewById(R.id.add_apertrue).setOnClickListener(this);
	}
	
	@Override
	public void onClick(View v) {
		switch(v.getId()) {
		case R.id.left_up:
			ToastUtils.showToast(CamVideoH264.this, R.string.app_name);
			break;
		case R.id.mid_up:
			
			break;
		case R.id.right_up:

			break;
		case R.id.left:
			
			break;
		case R.id.mid:

			break;
		case R.id.right:
			
			break;
		case R.id.left_down:

			break;
		case R.id.mid_down:
			
			break;
		case R.id.right_down:

			break;
		case R.id.minus_zoom:
			
			break;
		case R.id.add_zoom:

			break;
		case R.id.minus_foucs:
			
			break;
		case R.id.add_foucs:

			break;
		case R.id.minus_apertrue:
			
			break;
		case R.id.add_apertrue:

			break;
			default:
			break;
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
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        screenWidth = dm.widthPixels;
        screenHeight = dm.heightPixels;
        myVideoView.init(mHandler,screenWidth, screenHeight);
        System.out.println(screenHeight);
	}
	
	@Override
	protected void onStop() {
		super.onStop();
	}

	
	@Override
    protected void onDestroy() {
    	super.onDestroy();
    	myVideoView.onStop();
		dismissProgressDlg();
		if(ipPlayReceiver != null) {
			unregisterReceiver(ipPlayReceiver);
		}
		if(thread != null && !thread.isAlive()) {
			try {
				thread.join(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			thread = null;
		}
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
				Bundle bundle = intent.getExtras();
				if(bundle != null) {
					Object obj = bundle.getSerializable("IPPLAY");
					if(obj != null && obj instanceof Device) {
						device = (Device) obj;
						Log.d(TAG, "receive device info =" +  device);
						Message msg = mHandler.obtainMessage();
						if(device.getDeviceNetType()) {
							System.out.println(Integer.parseInt(device.getDeviceID(),16));
							netUtil = new ThroughNetUtil(mHandler,false,Integer.parseInt(device.getDeviceID(),16));
							new Thread(netUtil).start();
							msg.what = Constants.SHOWCONNDIALOG;
							mHandler.sendMessage(msg);
						} else {
							String ip = null;
							String id = bundle.getString("PLVIDEOINDEX");
							System.out.println(id+"--->");
							if(!"".equals(id)) {
								ip = device.getDeviceEthIp();
								System.out.println(id+"--->" + ip);
								try {
									PackageUtil.sendPackageNoRecvByIp(CamCmdListHelper.SetCmd_PlayNetFiles + id, ip, Constants.LOCALCMDPORT);
								} catch (CamManagerException e) {
									e.printStackTrace();
									Log.d(TAG, "play back in net = " + e.getMessage());
								}
							}
							msg.what = Constants.SHOWCONNDIALOG;
							mHandler.sendMessage(msg);
							mHandler.sendEmptyMessage(Constants.CONNECTTING);
						}
					}
				}
			}
		}
	}
	
	public static ThroughNetUtil getInstance() {
		return netUtil;
	}

}

