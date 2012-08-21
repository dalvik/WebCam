package com.iped.ipcam.gui;

import java.util.List;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ListView;

import com.iped.ipcam.engine.CamMagFactory;
import com.iped.ipcam.engine.ICamManager;
import com.iped.ipcam.pojo.BCVInfo;
import com.iped.ipcam.pojo.Device;
import com.iped.ipcam.utils.CamCmdListHelper;
import com.iped.ipcam.utils.Constants;
import com.iped.ipcam.utils.DialogUtils;
import com.iped.ipcam.utils.ErrorCode;
import com.iped.ipcam.utils.FileUtil;
import com.iped.ipcam.utils.PackageUtil;
import com.iped.ipcam.utils.RandomUtil;
import com.iped.ipcam.utils.ToastUtils;
import com.iped.ipcam.utils.VideoPreviewDeviceAdapter;
import com.iped.ipcam.utils.WebCamActions;
import com.iped.ipcam.utils.WinTaiCmd;


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
public class CamVideoH264 extends Activity implements OnClickListener, OnTouchListener {
	
	//private VideoView videoView = null;
	
	private MyVideoView myVideoView = null;
	
	private int screenWidth = 0;
	
	private int screenHeight = 0;
	
	private IpPlayReceiver ipPlayReceiver = null;
	
	private UpdeviceListReceiver updeviceListReceiver = null;
	
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
	
	private ListView listView = null;
	
	private VideoPreviewDeviceAdapter previewDeviceAdapter = null;
	
	private ICamManager camManager = null;
	
	private List<Device> list;
	
	private BCVControlProgressBar brightnessProgerss;
	
	private BCVControlProgressBar contrastProgressbar;
	
	private BCVControlProgressBar volumeProgressbar;
	
	private String newPwd = "";
	
	private String TAG = "CamVideoH264";
	
	private Handler mHandler = new Handler() {
		public void handleMessage(android.os.Message msg) {
			//initThread();
			switch (msg.what) {
			case Constants.CONNECTTING:
				startThread();
				break;
			case Constants.SHOWCONNDIALOG:
				showProgressDlg(R.string.connection);
				//startThread();
				break;
			case Constants.HIDECONNDIALOG:
				hideProgressDlg();
				break;
			case Constants.CONNECTERROR:
				ToastUtils.showToast(CamVideoH264.this, msg.arg1);
				//Toast.makeText(CamVideoH264.this, getResources().getString(R.string.connection_error), Toast.LENGTH_SHORT).show();
				break;
			case Constants.SENDGETTHREEPORTMSG:
				break;
			case Constants.SENDGETTHREEPORTTIMOUTMSG:
				ToastUtils.showToast(CamVideoH264.this, R.string.connection_error);
				hideProgressDlg();
				break;
			case Constants.SEND_SHOW_ONE_PWD_FIELD_CONFIG_MSG:
			case Constants.SEND_SHOW_TWO_PWD_FIELD_PREVIEW_MSG:
				Device device = camManager.getSelectDevice();
				String pwd = (String) msg.obj;
				device.setUnDefine2(pwd);
				int checkPwd = PackageUtil.checkPwd(device.getDeviceID(), device.getUnDefine2());
				if(checkPwd == 1) {
					camManager.updateCam(device);
					Intent intent2 = new Intent();
					Bundle bundle2 = new Bundle();
					bundle2.putString("PLVIDEOINDEX",""); 
					bundle2.putSerializable("IPPLAY", device);
					intent2.putExtras(bundle2);
					intent2.setAction(WebCamActions.ACTION_IPPLAY);
					sendBroadcast(intent2);
				} else {
					ToastUtils.showToast(CamVideoH264.this, R.string.device_manager_pwd_set_err);
				}
				break;
			case Constants.SEND_UPDATE_BCV_INFO_MSG:
				Bundle bcvInfo = msg.getData();
				if(bcvInfo != null) {
					BCVInfo info = (BCVInfo) bcvInfo.get("UPDATEBCV");
					brightnessProgerss.setProgress(info.getBrightness());
					contrastProgressbar.setProgress(info.getContrast());
					volumeProgressbar.setProgress(info.getVolume());
				}
				break;
			case Constants.WEB_CAM_CONNECT_INIT_MSG:
				
				break;
			case Constants.WEB_CAM_CHECK_PWD_STATE_MSG:
				
				break;
			case Constants.WEB_CAM_SHOW_CHECK_PWD_DLG_MSG:
				showProgressDlg(R.string.webcam_check_pwd_dialog_str);
				break;
			case Constants.WEB_CAM_HIDE_CHECK_PWD_DLG_MSG:
				hideProgressDlg();
				break;
			case Constants.SEND_SHOW_INPUT_TWO_PASS_DIALOG_SMG:
				DialogUtils.inputTwoPasswordDialog(CamVideoH264.this, camManager.getSelectDevice(), mHandler, Constants.WEB_CAM_CHECK_PWD_MSG);
				break;
			case Constants.SEND_SHOW_INPUT_ONE_PASS_DIALOG_SMG:
				DialogUtils.inputOnePasswordDialog(CamVideoH264.this, mHandler, Constants.WEB_CAM_CHECK_PWD_MSG);
				break;
			case Constants.WEB_CAM_CHECK_PWD_MSG:
				mHandler.sendEmptyMessage(Constants.WEB_CAM_SHOW_CHECK_PWD_DLG_MSG);
				newPwd = (String) msg.obj;
				new AsynCheckPwdTask().execute(0);
				/*HandlerThread handlerThread = new HandlerThread("test5");
				handlerThread.start();
				Handler handler = new Handler();
				handler.post(checkPwdRunnable);*/
				//mHandler.removeCallbacks(checkPwdRunnable);
				//mHandler.post(checkPwdRunnable);
				//new CheckPwdThread().start();
				break;
			case Constants.WEB_CAM_RECONNECT_MSG:
				mHandler.sendEmptyMessage(Constants.WEB_CAM_SHOW_CHECK_PWD_DLG_MSG);
				new AsynMonitorSocketTask().execute(0);
				break;
			default:
				break;
			}
		};
	};
	
	private void startThread() {
		myVideoView.setDevice(camManager.getSelectDevice());
		myVideoView.onStart();
		thread = new Thread(myVideoView);
		thread.start();
	}
	
	private void stopPlayThread() {
		if(!myVideoView.isStop()) {
			myVideoView.onStop();
		}
		if(thread != null && !thread.isInterrupted()) {
			thread.isInterrupted();
		}
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
        IntentFilter intentFilter =  new IntentFilter(WebCamActions.ACTION_IPPLAY);
        intentFilter.addAction(WebCamActions.WEB_CAM_CLOSE_CONN_ACTION);
        registerReceiver(ipPlayReceiver, intentFilter);
        updeviceListReceiver = new UpdeviceListReceiver(); 
        registerReceiver(updeviceListReceiver, new IntentFilter(WebCamActions.SEND_DEVICE_LIST_UPDATE_ACTION));
        myVideoView = (MyVideoView) findViewById(R.id.videoview);
        myVideoView.init(mHandler,screenWidth, screenHeight);
        LinearLayout layout = (LinearLayout) findViewById(R.id.container);
        
        LayoutInflater factory = LayoutInflater.from(this);
        View view = factory.inflate(R.layout.reight_menu, null);
        listView = (ListView)view.findViewById(R.id.video_preview_list);
        registerListener(view);
        int w = View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED);
        int h = View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED);
        view.measure(w, h);
        int width =view.getMeasuredWidth(); //
        //int height = view.getMeasuredHeight();
		rightControlPanel = new ControlPanel(this, myVideoView,  width + ControlPanel.HANDLE_WIDTH, LayoutParams.FILL_PARENT);
		layout.addView(rightControlPanel);
		rightControlPanel.fillPanelContainer(view);
		camManager = CamMagFactory.getCamManagerInstance();
		list = camManager.getCamList();
		listView.setOnItemClickListener(itemClickListener);
		previewDeviceAdapter = new VideoPreviewDeviceAdapter(list, this);
		listView.setAdapter(previewDeviceAdapter);
		PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "DoNotDimScreen");
		if(mWakeLock.isHeld() == false) {
	       mWakeLock.acquire();
	    }
	}

	private void registerListener(View view) {
		Button up = (Button) view.findViewById(R.id.mid_up); 
		//up.setOnFocusChangeListener(this);
		//up.setOnClickListener(this);
		up.setOnTouchListener(this);
		
		Button down = (Button) view.findViewById(R.id.mid_down);
		//down.setOnClickListener(this);
		//down.setOnFocusChangeListener(this);
		down.setOnTouchListener(this);
		
		Button left = (Button) view.findViewById(R.id.left);
		//left.setOnClickListener(this);
		//left.setOnFocusChangeListener(this);
		left.setOnTouchListener(this);
		
		Button right = (Button) view.findViewById(R.id.right);
		//right.setOnClickListener(this);
		//right.setOnFocusChangeListener(this);
		right.setOnTouchListener(this);
		
		view.findViewById(R.id.mid).setOnClickListener(this);
		/**/
		Button buttonMinusZoom = (Button) view.findViewById(R.id.minus_zoom); 
		buttonMinusZoom.setOnClickListener(this);
		brightnessProgerss = (BCVControlProgressBar) view.findViewById(R.id.brightness_progressbar);
		brightnessProgerss.init(getText(R.string.video_preview_brightness).toString());
		brightnessProgerss.setProgress(0);
		Button buttonAddZoom = (Button) view.findViewById(R.id.add_zoom); 
		buttonAddZoom.setOnClickListener(this);
		Button buttonMinusFocus = (Button) view.findViewById(R.id.minus_foucs); 
		buttonMinusFocus.setOnClickListener(this);
		contrastProgressbar = (BCVControlProgressBar) view.findViewById(R.id.contrast_progressbar);
		contrastProgressbar.init(getText(R.string.video_preview_contrast).toString());
		contrastProgressbar.setProgress(0);
		Button buttonAddFocus = (Button) view.findViewById(R.id.add_foucs); 
		buttonAddFocus.setOnClickListener(this);
		Button buttonMinusApertrue = (Button) view.findViewById(R.id.minus_apertrue); 
		buttonMinusApertrue.setOnClickListener(this);
		
		volumeProgressbar = (BCVControlProgressBar) view.findViewById(R.id.volume_progressbar);
		volumeProgressbar.init(getText(R.string.video_preview_volume).toString());
		volumeProgressbar.setProgress(0);
		
		Button buttonAddApertrue = (Button) view.findViewById(R.id.add_apertrue); 
		buttonAddApertrue.setOnClickListener(this);
	}
	
	private OnItemClickListener itemClickListener = new OnItemClickListener() {

		@Override
		public void onItemClick(AdapterView<?> arg0, View arg1, int index,
				long arg3) {
			listView.requestFocusFromTouch();
			listView.setSelection(index);
			camManager.setSelectInde(index);
			myVideoView.onStop();
			mHandler.sendEmptyMessage(Constants.WEB_CAM_SHOW_CHECK_PWD_DLG_MSG);
			new AsynMonitorSocketTask().execute(0);
		}
	};
	
	
	@Override
	public void onClick(View v) {
		if(myVideoView.isStop()) {
			//Toast.makeText(this, "return",Toast.LENGTH_SHORT).show();
			return ;
		}
		//PackageUtil.sendPTZCommond(myVideoView.getCmdSocket(), netUtil, device, WinTaiCmd.PTZ_CMD_LEFT.ordinal());
		switch(v.getId()) {
		case R.id.mid:
			myVideoView.setReverseFlag(!myVideoView.isReverseFlag());
			break;
		case R.id.minus_zoom:
			int value2 = brightnessProgerss.getProgress();
			value2 -= 10;
			if(value2 <=0) {
				value2 = 0;
			}
			brightnessProgerss.setProgress(value2);
			PackageUtil.setBCV(device.getDeviceID(),CamCmdListHelper.SetCmp_Set_Brightness, value2+"");
			break;
		case R.id.add_zoom:
			int value = brightnessProgerss.getProgress();
			value += 10;
			if(value >=100) {
				value = 100;
			}
			brightnessProgerss.setProgress(value);
			PackageUtil.setBCV(device.getDeviceID(), CamCmdListHelper.SetCmp_Set_Brightness, value+"");
			break;
		case R.id.minus_foucs:
			int value3 = contrastProgressbar.getProgress();
			value3 -= 10;
			if(value3 <=0) {
				value3 = 0;
			}
			contrastProgressbar.setProgress(value3);
			PackageUtil.setBCV(device.getDeviceID(),CamCmdListHelper.SetCmp_Set_Contrast, value3 +"");
			break;
		case R.id.add_foucs:
			int value4 = contrastProgressbar.getProgress();
			value4 += 10;
			if(value4 >=100) {
				value4 = 100;
			}
			contrastProgressbar.setProgress(value4);
			PackageUtil.setBCV(device.getDeviceID(),CamCmdListHelper.SetCmp_Set_Contrast, value4 +"");
			break;
		case R.id.minus_apertrue:
			int value5 = volumeProgressbar.getProgress();
			value5 -= 10;
			if(value5 <=0) {
				value5 = 0;
			}
			volumeProgressbar.setProgress(value5);
			//PackageUtil.setBCV(CamCmdListHelper.SetCmp_Set_Volume, value5+"");
			break;
		case R.id.add_apertrue:
			int value6 = volumeProgressbar.getProgress();
			value6 += 10;
			if(value6 >=100) {
				value6 = 100;
			}
			volumeProgressbar.setProgress(value6);
			//PackageUtil.setBCV(CamCmdListHelper.SetCmp_Set_Volume, value6+"");
			break;
		default:
			break;
		}/**/
	}
	
	@Override
	public boolean onTouch(View v, MotionEvent event) {
		Log.d(TAG, "is stop = " + myVideoView.isStop());
		if(myVideoView.isStop()) {
			//Toast.makeText(this, "return",Toast.LENGTH_SHORT).show();
			return false;
		}
		switch(v.getId()) {
		case R.id.left_up:
			break;
		case R.id.mid_up:
			if(event.getAction() == MotionEvent.ACTION_DOWN) {
				if(!myVideoView.isReverseFlag()) {
					//PackageUtil.sendPTZCommond(WinTaiCmd.PTZ_CMD_UP.ordinal());
					new AsynSendPTZ().execute(new Integer[]{WinTaiCmd.PTZ_CMD_UP.ordinal()});
				}else {
					//PackageUtil.sendPTZCommond(WinTaiCmd.PTZ_CMD_DOWN.ordinal());
					new AsynSendPTZ().execute(new Integer[]{WinTaiCmd.PTZ_CMD_DOWN.ordinal()});
				}
			} else if(event.getAction() == MotionEvent.ACTION_UP) {
				//PackageUtil.sendPTZCommond(WinTaiCmd.PTZ_CMD_STOP.ordinal());
				new AsynSendPTZ().execute(new Integer[]{WinTaiCmd.PTZ_CMD_STOP.ordinal()});
			}
			break;
		case R.id.right_up:

			break;
		case R.id.left:
			if(event.getAction() == MotionEvent.ACTION_DOWN) {
				if(!myVideoView.isReverseFlag()) {
					//PackageUtil.sendPTZCommond(WinTaiCmd.PTZ_CMD_LEFT.ordinal());
					new AsynSendPTZ().execute(new Integer[]{WinTaiCmd.PTZ_CMD_LEFT.ordinal()});
				} else {
					//PackageUtil.sendPTZCommond(WinTaiCmd.PTZ_CMD_RIGHT.ordinal());
					new AsynSendPTZ().execute(new Integer[]{WinTaiCmd.PTZ_CMD_RIGHT.ordinal()});
				}
			} else if(event.getAction() == MotionEvent.ACTION_UP) {
				//PackageUtil.sendPTZCommond(WinTaiCmd.PTZ_CMD_STOP.ordinal());
				new AsynSendPTZ().execute(new Integer[]{WinTaiCmd.PTZ_CMD_STOP.ordinal()});
			}
			break;
		case R.id.right:
			if(event.getAction() == MotionEvent.ACTION_DOWN) {
				if(!myVideoView.isReverseFlag()) {
					//PackageUtil.sendPTZCommond( WinTaiCmd.PTZ_CMD_RIGHT.ordinal());
					new AsynSendPTZ().execute(new Integer[]{WinTaiCmd.PTZ_CMD_RIGHT.ordinal()});
				} else {
					//PackageUtil.sendPTZCommond( WinTaiCmd.PTZ_CMD_LEFT.ordinal());
					new AsynSendPTZ().execute(new Integer[]{WinTaiCmd.PTZ_CMD_LEFT.ordinal()});
				}
			} else if(event.getAction() == MotionEvent.ACTION_UP) {
				//.sendPTZCommond(WinTaiCmd.PTZ_CMD_STOP.ordinal());
				new AsynSendPTZ().execute(new Integer[]{WinTaiCmd.PTZ_CMD_STOP.ordinal()});
			}
			break;
		case R.id.left_down:

			break;
		case R.id.mid_down:
			if(event.getAction() == MotionEvent.ACTION_DOWN) {
				if(!myVideoView.isReverseFlag()) {
					new AsynSendPTZ().execute(new Integer[]{WinTaiCmd.PTZ_CMD_DOWN.ordinal()});
					//PackageUtil.sendPTZCommond(WinTaiCmd.PTZ_CMD_DOWN.ordinal());
				}else {
					new AsynSendPTZ().execute(new Integer[]{WinTaiCmd.PTZ_CMD_UP.ordinal()});
					//PackageUtil.sendPTZCommond(WinTaiCmd.PTZ_CMD_UP.ordinal());
				}
			} else if(event.getAction() == MotionEvent.ACTION_UP) {
				new AsynSendPTZ().execute(new Integer[]{WinTaiCmd.PTZ_CMD_DOWN.ordinal()});
				//PackageUtil.sendPTZCommond(WinTaiCmd.PTZ_CMD_STOP.ordinal());
			}
			
			break;
		case R.id.right_down:

			break;
			default:
			break;
		}
		return false;
	}
	
	@Override
	protected void onResume() {
		super.onResume();
	}
	
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        screenWidth = dm.widthPixels;
        screenHeight = dm.heightPixels;
        myVideoView.init(mHandler,screenWidth, screenHeight);
	}
	
	@Override
	protected void onStop() {
		super.onStop();
	}

	
	@Override
    protected void onDestroy() {
    	super.onDestroy();
    	Log.d(TAG, "close---==== onDestroy");
    	myVideoView.onStop();
		dismissProgressDlg();
		if(ipPlayReceiver != null) {
			unregisterReceiver(ipPlayReceiver);
		}
		if(updeviceListReceiver != null) {
			unregisterReceiver(updeviceListReceiver);
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
	
	private void showProgressDlg(int textId) {
		if(m_Dialog == null) {
			m_Dialog = new ProgressDialog(CamVideoH264.this);
			m_Dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		}
		if(m_Dialog != null) {
			m_Dialog.setMessage(getResources().getString(textId, device.getDeviceID()));
			if(!m_Dialog.isShowing()) {
				m_Dialog.show();
			}
		}
	}
	
	private void hideProgressDlg() {
		if(m_Dialog != null && m_Dialog.isShowing()) {
			m_Dialog.dismiss();
		}
	}
	
	private void dismissProgressDlg() {
		if(m_Dialog != null) {
			m_Dialog.dismiss();
		}
	}
	
	private class UpdeviceListReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			if(WebCamActions.SEND_DEVICE_LIST_UPDATE_ACTION.equals(intent.getAction())) {
				previewDeviceAdapter = new VideoPreviewDeviceAdapter(list, CamVideoH264.this);
				listView.setAdapter(previewDeviceAdapter);
				ToastUtils.setListViewHeightBasedOnChildren(listView);
			}
			
		}	
		
	};
	
	private class IpPlayReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			if(WebCamActions.ACTION_IPPLAY.equals(intent.getAction())) {
				mHandler.sendEmptyMessage(Constants.WEB_CAM_SHOW_CHECK_PWD_DLG_MSG);
				new AsynMonitorSocketTask().execute(0);
			}
		}
	}
	
	class AsynMonitorSocketTask extends AsyncTask<Integer, Integer, Void> {
		
		@Override
		protected Void doInBackground(Integer... params) {
			stopPlayThread();
			device = camManager.getSelectDevice();
			int result = UdtTools.monitorSocket(device.getDeviceID());
			Log.d(TAG, "monitor result = " + result);
			analyseResult(result, device);
			return null;
		}
		
	}
	
	class AsynSendPTZ extends AsyncTask<Integer, Integer, Void> {
	 @Override
	 	protected Void doInBackground(Integer... params) {
		 	PackageUtil.sendPTZCommond(params[0]);
	 		return null;
	 	}	
	}
	
	private void analyseResult(int result, Device device) {
		switch (result) {
		case ErrorCode.STUN_ERR_INTERNAL:
			sendErrorMessage(R.string.webcam_error_code_internel);
			mHandler.sendEmptyMessage(Constants.WEB_CAM_HIDE_CHECK_PWD_DLG_MSG);
			return;
		case ErrorCode.STUN_ERR_SERVER:
			sendErrorMessage(R.string.webcam_error_code_server_not_reached);
			mHandler.sendEmptyMessage(Constants.WEB_CAM_HIDE_CHECK_PWD_DLG_MSG);
			return;
		case ErrorCode.STUN_ERR_TIMEOUT:
			sendErrorMessage(R.string.webcam_error_code_timeout);
			mHandler.sendEmptyMessage(Constants.WEB_CAM_HIDE_CHECK_PWD_DLG_MSG);
			return;
		case ErrorCode.STUN_ERR_INVALIDID:
			sendErrorMessage(R.string.webcam_error_code_unlegal);
			mHandler.sendEmptyMessage(Constants.WEB_CAM_HIDE_CHECK_PWD_DLG_MSG);
			return;
		case ErrorCode.STUN_ERR_CONNECT:
			sendErrorMessage(R.string.webcam_error_code_connect_error);
			mHandler.sendEmptyMessage(Constants.WEB_CAM_HIDE_CHECK_PWD_DLG_MSG);
			return;
		case ErrorCode.STUN_ERR_BIND:
			sendErrorMessage(R.string.webcam_error_code_bind_error);
			mHandler.sendEmptyMessage(Constants.WEB_CAM_HIDE_CHECK_PWD_DLG_MSG);
			return;
		default:
			break;
		}
		//mHandler.sendEmptyMessage(Constants.WEB_CAM_CONNECT_INIT_MSG);
		String random = RandomUtil.generalRandom();
		Log.d(TAG, "random = " + random);
		int initRes = UdtTools.initialSocket(device.getDeviceID(),random);
		if(initRes<0) {
			Log.d(TAG, "initialSocket init error!");
			mHandler.sendEmptyMessage(Constants.WEB_CAM_HIDE_CHECK_PWD_DLG_MSG);
			sendErrorMessage(R.string.webcam_connect_init_error);
		}else {
			checkPwdState();
		}
	}
	
	private void checkPwdState() {
		int resu = PackageUtil.checkPwdState(device.getDeviceID());
		Log.d(TAG, "device manager checkPwdState result = " + resu);
		if(resu == 0) { // unset
			mHandler.sendEmptyMessage(Constants.WEB_CAM_HIDE_CHECK_PWD_DLG_MSG);
			mHandler.sendEmptyMessage(Constants.SEND_SHOW_INPUT_TWO_PASS_DIALOG_SMG);
		} else if(resu == 1) {// pwd seted
			device = camManager.getSelectDevice();
			if(device.getUnDefine2() != null && device.getUnDefine2().length()>0) {
				Message mesg = mHandler.obtainMessage();
				mesg.obj  = device.getUnDefine2();
				mesg.what = Constants.WEB_CAM_CHECK_PWD_MSG;
				mHandler.sendMessage(mesg);
			}else {
				mHandler.sendEmptyMessage(Constants.WEB_CAM_HIDE_CHECK_PWD_DLG_MSG);
				mHandler.sendEmptyMessage(Constants.SEND_SHOW_INPUT_ONE_PASS_DIALOG_SMG);
			}
		} else if(resu == 2){
			sendErrorMessage(R.string.device_manager_pwd_set_err);
			mHandler.sendEmptyMessage(Constants.WEB_CAM_HIDE_CHECK_PWD_DLG_MSG);
		} else {
			sendErrorMessage(R.string.device_manager_time_out_or_device_off_line);
			mHandler.sendEmptyMessage(Constants.WEB_CAM_HIDE_CHECK_PWD_DLG_MSG);
		}
	}
	
	private void sendErrorMessage(int errorStrId) {
		Message msg = mHandler.obtainMessage();
		msg.what = Constants.CONNECTERROR;
		msg.arg1 = errorStrId;
		mHandler.sendMessage(msg);
	}
	
	private class AsynCheckPwdTask extends AsyncTask<Integer, Integer, Void> {
		@Override
		protected Void doInBackground(Integer... params) {
			int checkPwd = PackageUtil.checkPwd(device.getDeviceID(),newPwd);
			Log.d(TAG, "checkPwd result = " + checkPwd);
			if(checkPwd == 1) {
				device.setUnDefine2(newPwd);
				camManager.updateCam(device);
				FileUtil.persistentDevice(CamVideoH264.this,camManager.getCamList());
				mHandler.sendEmptyMessage(Constants.CONNECTTING);
			} else if(checkPwd == -1) {
				mHandler.sendEmptyMessage(Constants.WEB_CAM_HIDE_CHECK_PWD_DLG_MSG);
				sendErrorMessage(R.string.device_manager_pwd_set_err);
				mHandler.sendEmptyMessage(Constants.SEND_SHOW_INPUT_ONE_PASS_DIALOG_SMG);
			} else {
				mHandler.sendEmptyMessage(Constants.WEB_CAM_HIDE_CHECK_PWD_DLG_MSG);
				sendErrorMessage(R.string.device_manager_time_out_or_device_off_line);
			}
			return null;
		}
	}
}

