package com.iped.ipcam.gui;

import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.provider.Settings;
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
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import com.iped.ipcam.engine.CamMagFactory;
import com.iped.ipcam.engine.ICamManager;
import com.iped.ipcam.pojo.BCVInfo;
import com.iped.ipcam.pojo.Device;
import com.iped.ipcam.utils.ByteUtil;
import com.iped.ipcam.utils.CamCmdListHelper;
import com.iped.ipcam.utils.Constants;
import com.iped.ipcam.utils.DateUtil;
import com.iped.ipcam.utils.DialogUtils;
import com.iped.ipcam.utils.ErrorCode;
import com.iped.ipcam.utils.FileUtil;
import com.iped.ipcam.utils.NetworkUtil;
import com.iped.ipcam.utils.PackageUtil;
import com.iped.ipcam.utils.PlayBackConstants;
import com.iped.ipcam.utils.RandomUtil;
import com.iped.ipcam.utils.StringUtils;
import com.iped.ipcam.utils.ToastUtils;
import com.iped.ipcam.utils.VideoPreviewDeviceAdapter;
import com.iped.ipcam.utils.WebCamActions;
import com.iped.ipcam.utils.WinTaiCmd;


/**
     H.264�Ĺ��ܷ�Ϊ���㣬
             ��Ƶ�����(VCL��Video Coding Layer)
             ������ȡ��(NAL��Network Abstraction Layer)
             ��VCL��NAL֮�䶨����һ�����ڷ��鷽ʽ�Ľӿڣ��������Ӧ����������NAL��һ���֡�
  	   ��������Ч�ʱ����������Ӧ�Ե�����ֱ���VCL��NAL����ɡ�VCL�����Ǳ��봦�������������ʾ��ѹ����������Ƶ�������С�
  	   ��VCL���ݴ���ʹ洢֮ǰ����Щ�����VCL�����ȱ�ӳ����װ��NAL��Ԫ�С�
     VCL�������ڿ���˶���������ϱ����һЩ�����ԡ�
     NAL��������²���������Զ����ݽ��з�װ��������֡�����źŸ��߼��ŵ�������ͬ����Ϣ�ȡ�
     NAL��VCL������ݣ�����ͷ��Ϣ���νṹ��Ϣ��ʵ���غɣ�NAL�����������ȷ�ؽ�����ӳ�䵽����Э���ϡ�
     NAL�����Ǹ��־����Э�飬
             ��H.323��H.324��RTP/UDP/IP�ȡ�NAL��������������H.264��Ӧ�����ŵ��������� 
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
	
	//private Device device = null;
	
	private ListView listView = null;
	
	private VideoPreviewDeviceAdapter previewDeviceAdapter = null;
	
	private ICamManager camManager = null;
	
	private List<Device> list;
	
	private BCVControlProgressBar brightnessProgerss;
	
	private BCVControlProgressBar contrastProgressbar;
	
	private BCVControlProgressBar volumeProgressbar;
	
	private String newPwd = "";
	
	private String playBackFlag;
	
	private MySeekBar playBackSeekBar = null;
	
	private LinearLayout playBackBottomlayout;

	private TextView currentTextView = null;
	
	private TextView totalTextView = null;
	
	private long during = 0;
	
	private long startTime = 0;
	
	private byte[] table2 = null;

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
					brightnessProgerss.setProgress(info.getBrightness()>0?info.getBrightness():0);
					contrastProgressbar.setProgress(info.getContrast()>0?info.getContrast():0);
					volumeProgressbar.setProgress(info.getVolume()>0?info.getVolume():0);
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
				new AsynMonitorSocketTask().execute("");
				break;
			case PlayBackConstants.INIT_SEEK_BAR:
				currentTextView = (TextView) playBackBottomlayout.findViewById(R.id.currenttime);
				totalTextView = (TextView) playBackBottomlayout.findViewById(R.id.totaltime);
				playBackSeekBar.setOnSeekBarChangeListener(seekBarChangeListener);
				playBackSeekBar.init(startTime);
				//currentTextView.setText(StringUtils.makeTimeString(CamVideoH264.this, 0));
				currentTextView.setText(DateUtil.formatTimeToDate6(startTime));
				Log.d(TAG, "### initSeekBar " + StringUtils.makeTimeString(CamVideoH264.this, 0));
				//totalTextView.setText(StringUtils.makeTimeString(CamVideoH264.this, during/1000));
				totalTextView.setText(DateUtil.formatTimeToDate6(during+startTime));
				Log.d(TAG, "### initSeekBar " + StringUtils.makeTimeString(CamVideoH264.this, during/1000) + " " + during/1000 + " s");
				playBackSeekBar.setEnabled(true);
				playBackSeekBar.setProgress(1);
				playBackSeekBar.setMax((int)during/1000);
				Bundle bundle = msg.getData();
				if(bundle != null) {
					table2 = bundle.getByteArray("TABLE2");
					/*int in =table2.length;
					for(int j = 0;j<in;j+=4) {
						int seekPos = ByteUtil.byteToInt4(table2, j);
						Log.d(TAG, "### index=" + j + " seekPos=" + seekPos);
					}*/
				}
				break;
			case PlayBackConstants.HIDE_SEEKBAR_LAYOUT:
				playBackBottomlayout.setVisibility(View.GONE);
				playBackBottomlayout.invalidate();
				break;
			case PlayBackConstants.SHOW_SEEKBAR_LAYOUT:
				playBackBottomlayout.setVisibility(View.VISIBLE);
				break;
			case PlayBackConstants.DISABLE_SEEKBAR:
				playBackSeekBar.setProgress(0);
				playBackSeekBar.setEnabled(false);
				break;
			case Constants.UPDATE_PLAY_BACK_TIME:
				//Log.d(TAG, "##############" + playBackSeekBar.getProgress());
				String time = (String)msg.obj;
				long currentDuringTime = DateUtil.formatTimeStrToLong(time) - startTime;// ��ǰ����ʱ��
				//Log.d(TAG, "### play time = " +time + " currentTime=" + currentTime/1000 + " startTime=" + startTime);
				if(currentDuringTime <= during){
					playBackSeekBar.setProgress((int)(currentDuringTime/1000));
				}
				//currentTextView.setText(StringUtils.makeTimeString(CamVideoH264.this, playBackSeekBar.getProgress() + 1));
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
		if(!myVideoView.isStopPlay()) {
			myVideoView.setStopPlay(true);
		}
		if(thread != null && !thread.isInterrupted()) {
			Log.d(TAG, "############## interrupt.");
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
        intentFilter.addAction(WebCamActions.ACTION_PLAY_BACK);
        intentFilter.addAction(WebCamActions.WEB_CAM_CLOSE_CONN_ACTION);
        registerReceiver(ipPlayReceiver, intentFilter);
        updeviceListReceiver = new UpdeviceListReceiver(); 
        registerReceiver(updeviceListReceiver, new IntentFilter(WebCamActions.SEND_DEVICE_LIST_UPDATE_ACTION));
        myVideoView = (MyVideoView) findViewById(R.id.videoview);
        myVideoView.init(mHandler,screenWidth, screenHeight);
        LinearLayout layout = (LinearLayout) findViewById(R.id.container);
        playBackBottomlayout = (LinearLayout) layout.findViewById(R.id.play_back_bottom);
        playBackSeekBar = (MySeekBar) playBackBottomlayout.findViewById(R.id.play_back_seek_bar);
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
		mHandler.sendEmptyMessage(PlayBackConstants.DISABLE_SEEKBAR);
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
		
		Button leftDown = (Button) view.findViewById(R.id.left_down);
		//right.setOnClickListener(this);
		//right.setOnFocusChangeListener(this);
		leftDown.setOnClickListener(this);
		
		view.findViewById(R.id.mid).setOnClickListener(this);
		
		Button RightDown = (Button) view.findViewById(R.id.right_down);
		//right.setOnClickListener(this);
		//right.setOnFocusChangeListener(this);
		RightDown.setOnClickListener(this);
		
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
			if(NetworkUtil.checkNetwokEnable(CamVideoH264.this)) {
				mHandler.sendEmptyMessage(Constants.WEB_CAM_SHOW_CHECK_PWD_DLG_MSG);
				myVideoView.setPlayBackFlag(false);
				mHandler.sendEmptyMessage(PlayBackConstants.HIDE_SEEKBAR_LAYOUT);
				new AsynMonitorSocketTask().execute("");
			} else {
				handleNetworkOpeartion(CamVideoH264.this);
			}
		}
	};
	
	
	@Override
	public void onClick(View v) {
		if(v.getId() == R.id.right_down){
			startActivity(new Intent(this,ImageViewer.class));
			return;
		}
		if(myVideoView.isStopPlay()) {
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
			if(PackageUtil.setBCV(camManager.getSelectDevice().getDeviceID(),CamCmdListHelper.SetCmp_Set_Brightness, value2+"")>0){
				brightnessProgerss.setProgress(value2);
			}
			break;
		case R.id.add_zoom:
			int value = brightnessProgerss.getProgress();
			value += 10;
			if(value >=100) {
				value = 100;
			}
			if(PackageUtil.setBCV(camManager.getSelectDevice().getDeviceID(), CamCmdListHelper.SetCmp_Set_Brightness, value+"")>0) {
				brightnessProgerss.setProgress(value);
			}
			break;
		case R.id.minus_foucs:
			int value3 = contrastProgressbar.getProgress();
			value3 -= 10;
			if(value3 <=0) {
				value3 = 0;
			}
			if(PackageUtil.setBCV(camManager.getSelectDevice().getDeviceID(),CamCmdListHelper.SetCmp_Set_Contrast, value3 +"")>0) {
				contrastProgressbar.setProgress(value3);
			}
			break;
		case R.id.add_foucs:
			int value4 = contrastProgressbar.getProgress();
			value4 += 10;
			if(value4 >=100) {
				value4 = 100;
			}
			if(PackageUtil.setBCV(camManager.getSelectDevice().getDeviceID(),CamCmdListHelper.SetCmp_Set_Contrast, value4 +"")>0){
				contrastProgressbar.setProgress(value4);
			}
			break;
		case R.id.minus_apertrue:
			int value5 = volumeProgressbar.getProgress();
			value5 -= 10;
			if(value5 <=0) {
				value5 = 0;
			}
			if(PackageUtil.setBCV(camManager.getSelectDevice().getDeviceID(), CamCmdListHelper.SetCmp_Set_Volume, value5+"")>0) {
				volumeProgressbar.setProgress(value5);
			}
			break;
		case R.id.add_apertrue:
			int value6 = volumeProgressbar.getProgress();
			value6 += 10;
			if(value6 >=100) {
				value6 = 100;
			}
			if(PackageUtil.setBCV(camManager.getSelectDevice().getDeviceID(), CamCmdListHelper.SetCmp_Set_Volume,value6+"")>0){
				volumeProgressbar.setProgress(value6);
			}
			
			break;
		case R.id.left_down:
			if(myVideoView.takePic()) {
				ToastUtils.showToast(this, R.string.webcam_takepic_success_str);
			}else {
				ToastUtils.showToast(this, R.string.webcam_takepic_error_str);
			}
			break;
		case R.id.right_down:
			startActivity(new Intent(this,ImageViewer.class));
			break;
		default:
			break;
		}/**/
	}
	
	private boolean f  = true;
	
	@Override
	public boolean onTouch(View v, MotionEvent event) {
		Log.d(TAG, "is stop = " + myVideoView.isStopPlay());
		if(myVideoView.isStopPlay()) {
			//Toast.makeText(this, "return",Toast.LENGTH_SHORT).show();
			//return false;
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
			if(f) {
				f = false;
				if(myVideoView.takePic()) {
					ToastUtils.showToast(this, R.string.webcam_takepic_success_str);
				}else {
					ToastUtils.showToast(this, R.string.webcam_takepic_error_str);
				}
				f = true;
			}
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
			if(f) {
				f = false;
				startActivity(new Intent(this,ImageViewer.class));
				f = true;
			}
			//FileUtil.openImage(this);
			//loadImage.initImage();
			/*if(imageViewerDialog == null || !imageViewerDialog.isShowing()) {
				imageViewerDialog = new ImageViewer(this, R.style.image_list_dialog);
				imageViewerDialog.show();
			}*/
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
			//System.out.println(m_Dialog + " ---" + camManager.getSelectDevice());
			m_Dialog.setMessage(getResources().getString(textId, camManager.getSelectDevice().getDeviceID()));
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
				if(NetworkUtil.checkNetwokEnable(context)) {
					mHandler.sendEmptyMessage(Constants.WEB_CAM_SHOW_CHECK_PWD_DLG_MSG);
					myVideoView.setPlayBackFlag(false);
					mHandler.sendEmptyMessage(PlayBackConstants.HIDE_SEEKBAR_LAYOUT);
					new AsynMonitorSocketTask().execute("");
				} else {
					handleNetworkOpeartion(context);
				}
			}else if(WebCamActions.ACTION_PLAY_BACK.equals(intent.getAction())) {
				Bundle bundle = intent.getExtras();
				if(bundle != null) {
					String indexStr = bundle.getString("PLVIDEOINDEX");
					myVideoView.setPlayBackFlag(true);
					mHandler.sendEmptyMessage(PlayBackConstants.SHOW_SEEKBAR_LAYOUT);
					during = bundle.getLong("TOTALTIME");
					startTime = bundle.getLong("STARTTIME");
					new AsynMonitorSocketTask().execute(indexStr);
				}
			}
		}
	}
	
	class AsynMonitorSocketTask extends AsyncTask<String, Integer, Void> {
		
		@Override
		protected Void doInBackground(String... params) {
			stopPlayThread();
			playBackFlag = params[0];
			Device device = camManager.getSelectDevice();
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
		int initRes = 1;//UdtTools.initialSocket(device.getDeviceID(),random);
		if(initRes<0) {
			Log.d(TAG, "initialSocket init error!");
			mHandler.sendEmptyMessage(Constants.WEB_CAM_HIDE_CHECK_PWD_DLG_MSG);
			sendErrorMessage(R.string.webcam_connect_init_error);
		}else {
			checkPwdState();
		}
	}
	
	private void checkPwdState() {
		Device device = camManager.getSelectDevice();
		int resu = PackageUtil.checkPwdState(device.getDeviceID());
		Log.d(TAG, "device manager checkPwdState result = " + resu);
		if(resu == 0) { // unset
			mHandler.sendEmptyMessage(Constants.WEB_CAM_HIDE_CHECK_PWD_DLG_MSG);
			mHandler.sendEmptyMessage(Constants.SEND_SHOW_INPUT_TWO_PASS_DIALOG_SMG);
		} else if(resu == 1) {// pwd seted
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
			Device device = camManager.getSelectDevice();
			int checkPwd = PackageUtil.checkPwd(device.getDeviceID(),newPwd);
			Log.d(TAG, "checkPwd result = " + checkPwd);
			if(checkPwd == 1) {
				device.setUnDefine2(newPwd);
				camManager.updateCam(device);
				FileUtil.persistentDevice(CamVideoH264.this,camManager.getCamList());
				Log.d(TAG, "### play back vodio index = " + playBackFlag);
				if(playBackFlag != null && playBackFlag.length()>0) {
					String item = CamCmdListHelper.SetCmd_Play_Back + playBackFlag;
					int res = UdtTools.sendCmdMsgById(device.getDeviceID(), item, item.length());
					Log.d(TAG, "### send play flag res = " + res);
					if(res>0) {
						mHandler.sendEmptyMessage(Constants.CONNECTTING);
					}else {
						sendErrorMessage(R.string.device_manager_time_out_or_device_off_line);
					}
				}else {
					mHandler.sendEmptyMessage(Constants.CONNECTTING);
				}
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

	private void handleNetworkOpeartion(final Context context) {
		new Thread() {  
            @Override  
            public void run() {  
                Looper.prepare();  
                new AlertDialog.Builder(context)
                .setTitle(context.getResources().getString(R.string.webcam_network_invalid_title_str))
                .setCancelable(false)  
                .setMessage(context.getResources().getString(R.string.webcam_network_invalid_message_str))
                .setNeutralButton(context.getResources().getString(R.string.webcam_network_invalid_open), new android.content.DialogInterface.OnClickListener() {  
                            @Override  
                            public void onClick(android.content.DialogInterface dialog, int which) {  
                            	Intent intent = new Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS);
                            	context.startActivity(intent);
                            }  
                        })
                        .setNegativeButton(context.getResources().getString(R.string.webcam_network_invalid_cancle), null)
                        .create().show();
                Looper.loop();  
            }  
        }.start();  
	}
	
	private OnSeekBarChangeListener seekBarChangeListener = new OnSeekBarChangeListener() {

		@Override
		public void onStartTrackingTouch(SeekBar seekBar) {
			//Log.d(TAG, "### onStartTrackingTouch = " + seekBar.getProgress());
		}
	
		@Override
		public void onStopTrackingTouch(SeekBar seekBar) {
			int progress = seekBar.getProgress()/15;
			//currentTextView.setText(StringUtils.makeTimeString(CamVideoH264.this, progress *15));
			//currentTextView.setText(DateUtil.formatTimeToDate6(progress * 1000+startTime));
			int index = progress * 4;
			if(table2 != null && table2.length>=index+4) {
					int seekPos = ByteUtil.byteToInt4(table2, index);
					Log.d(TAG, "### onStopTrackingTouch  seek value=" + seekBar.getProgress() + " timeIndex = " + progress + "  index=" + index + "  seek value = " + seekPos);
					new AsynPlayBackTask().execute(seekPos);
			}
		}
		
		@Override
		public void onProgressChanged(SeekBar seekBar, int progress,
				boolean fromUser) {
			//Log.d(TAG, "change" + progress);
			//currentTextView.setText(StringUtils.makeTimeString(CamVideoH264.this, progress));
			//currentTextView.setText(DateUtil.formatTimeToDate6(progress*1000+startTime));
			playBackSeekBar.setProgress(progress);
			//System.out.println(progress + "####" + startTime+" "+ DateUtil.formatTimeToDate6(progress+startTime));
		}
	};

	private class AsynPlayBackTask extends AsyncTask<Integer, Integer, Void> {
		
		@Override
		protected Void doInBackground(Integer... params) {
			String item = CamCmdListHelper.SetCmd_Seek + params[0]+ "\0";
			UdtTools.sendCmdMsgById( camManager.getSelectDevice().getDeviceID(), item, item.length());
			//Log.d(TAG, "### Seek cmd = " + item + " seek result = " + res);
			return null;
		}
	}

}