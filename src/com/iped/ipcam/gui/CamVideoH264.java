package com.iped.ipcam.gui;

import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.preference.Preference;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.mobstat.StatService;
import com.iped.ipcam.bitmapfun.ImageGrid;
import com.iped.ipcam.engine.CamMagFactory;
import com.iped.ipcam.engine.ICamManager;
import com.iped.ipcam.pojo.BCVInfo;
import com.iped.ipcam.pojo.Device;
import com.iped.ipcam.utils.ByteUtil;
import com.iped.ipcam.utils.CamCmdListHelper;
import com.iped.ipcam.utils.Constants;
import com.iped.ipcam.utils.DateUtil;
import com.iped.ipcam.utils.DialogUtils;
import com.iped.ipcam.utils.FileUtil;
import com.iped.ipcam.utils.MessageUtils;
import com.iped.ipcam.utils.NetworkUtil;
import com.iped.ipcam.utils.PackageUtil;
import com.iped.ipcam.utils.PlayBackConstants;
import com.iped.ipcam.utils.StringUtils;
import com.iped.ipcam.utils.ToastUtils;
import com.iped.ipcam.utils.VideoPreviewDeviceAdapter;
import com.iped.ipcam.utils.WebCamActions;
import com.iped.ipcam.utils.WinTaiCmd;
import com.iped.ipcam.view.VideoPopupMenu;


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
	
	private static final int UPDATE_COMPONENT = 1302281010;
	
	private MyVideoView myVideoView = null;
	
	private int screenWidth = 0;
	
	private int screenHeight = 0;
	
	private IpPlayReceiver ipPlayReceiver = null;
	
	private UpdeviceListReceiver updeviceListReceiver = null;
	
	private ControlPanel rightControlPanel = null;
	
	private Thread thread = null;
	
	private static WakeLock mWakeLock;
	
	private CustomProgressDialog m_Dialog = null;
	
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
	
	private BCVControlProgressBar clearProgerss;
	
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
	
	private int [] videoPopMenuItem = {R.string.webcam_video_popup_menu_stop, R.string.webcam_video_popup_menu_cancle};
	
	private VideoPopupMenu popupMenu = null;
	
	private View rightView = null;
	
	private int playBackDeviceIndex = 0;
	
	private BCVInfo info = null;
	
	private int x_offSet = 0;
	
	private int y_offSet = 0;
	
	private RadioButton qvga = null;
	
	private RadioButton vga = null;
	
	private RadioButton qelp = null;
	
	private ConnectivityManager connectivityManager;
	
	private boolean seekingFlag = false;
	
	private long firtick = 0l;

	private long sectick = 0l;

	private long distance = 0l;

	private SharedPreferences settings = null;
	
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
				Button sendAudio = (Button) rightView.findViewById(R.id.send_audio);
				sendAudio.setText(R.string.video_preview_send_audio_close);
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
					info = (BCVInfo) bcvInfo.get("UPDATEBCV");
					clearProgerss.setProgress(info.getQuality()>0?info.getQuality():0);
					brightnessProgerss.setProgress(info.getBrightness()>0?info.getBrightness():0);
					contrastProgressbar.setProgress(info.getContrast()>0?info.getContrast():0);
					volumeProgressbar.setProgress(info.getVolume()>0?info.getVolume():0);
				}
				break;
			case Constants.WEB_CAM_CONNECT_INIT_MSG:
				Button sendAudioButton = (Button) rightView.findViewById(R.id.send_audio);
				UdtTools.sendCmdMsg(CamCmdListHelper.SetAudioTalkOff, CamCmdListHelper.SetAudioTalkOff.length());
				sendAudioButton.setText(R.string.video_preview_send_audio_open);
				break;
			case Constants.WEB_CAM_CHECK_PWD_STATE_MSG:
				ToastUtils.showToast(CamVideoH264.this, R.string.video_preview_send_audio_open_tips);
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
				playBackSeekBar.setProgress(0);
				playBackSeekBar.setMax((int)during/1000);
				Bundle bundle = msg.getData();
				if(bundle != null) {
					table2 = bundle.getByteArray("TABLE2");
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
				playBackSeekBar.init(0);
				playBackSeekBar.setProgress(0);
				if(currentTextView != null) {
					currentTextView.setText("");
				}
				if(totalTextView != null) {
					totalTextView.setText("");
				}
				playBackSeekBar.setEnabled(false);
				break;
			case Constants.UPDATE_PLAY_BACK_TIME:
				//Log.d(TAG, "##############" + playBackSeekBar.getProgress());
				String time = (String)msg.obj;
				//Log.d(TAG, "time= " + time);
				long currentDuringTime = DateUtil.formatTimeStrToLong(time) - startTime;// 当前播放时间
				//Log.d(TAG, "### play time = " +time + " currentTime=" + currentDuringTime/1000 + " startTime=" + startTime);
				if(currentDuringTime <= during){
					if(!seekingFlag) {
						playBackSeekBar.setProgress((int)(currentDuringTime/1000));
					}
				}
				//currentTextView.setText(StringUtils.makeTimeString(CamVideoH264.this, playBackSeekBar.getProgress() + 1));
				break;
			case 7001:
				myVideoView.setStopPlay(true, false);
				break;
			case MyVideoView.UPDATE_RESULATION:
				updateResulation(msg.arg1);
				break;
			case Constants.SHOW_POP_UP_TIPS_DIA_MSG:
				PopupActivity popupActivity = new PopupActivity(CamVideoH264.this, R.style.thems_tips_popup_dailog);
				popupActivity.show();
				WindowManager.LayoutParams params = popupActivity.getWindow().getAttributes();
				params.width = screenWidth*20/36;
				params.height = screenHeight/2;
				popupActivity.getWindow().setAttributes(params);
				break;
			case Constants.CONNECTERRORINFO:
				String info = (String)msg.obj;
				Toast.makeText(CamVideoH264.this, info, Toast.LENGTH_SHORT).show();
				break;
			case UPDATE_COMPONENT:
				String mode = (String)msg.obj;
				updateComponentByMode(mode);
				break;
			default:
				break;
			}
		};
	};
	
	private void startThread() {
		myVideoView.setDevice(camManager.getSelectDevice());
		myVideoView.setOnLongClickListener(longClickListener);
		myVideoView.setOnTouchListener(videoViewOnTouch);
		myVideoView.onStart();
		thread = new Thread(myVideoView);
		thread.start();
	}
	
	private void stopPlayThread() {
		myVideoView.setStopPlay(true, true);
		if(thread != null && !thread.isInterrupted()) {
			Log.d(TAG, "##############");
			thread.isInterrupted();
		}
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		settings = getSharedPreferences(WebCam.class.getName(), 0);
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
        myVideoView.init(mHandler,screenWidth, screenHeight, settings.getBoolean("REVERSE", false), settings.getInt("QUALITY", 1));
        myVideoView.setOnClickListener(this);
        LinearLayout layout = (LinearLayout) findViewById(R.id.container);
        playBackBottomlayout = (LinearLayout) layout.findViewById(R.id.play_back_bottom);
        playBackSeekBar = (MySeekBar) playBackBottomlayout.findViewById(R.id.play_back_seek_bar);
        LayoutInflater factory = LayoutInflater.from(this);
        rightView = factory.inflate(R.layout.reight_menu, null);
        listView = (ListView)rightView.findViewById(R.id.video_preview_list);
        registerListener(rightView);
        updateComponent(false);
        int w = View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED);
        int h = View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED);
        rightView.measure(w, h);
        int width =rightView.getMeasuredWidth(); //
        //int height = view.getMeasuredHeight();
		rightControlPanel = new ControlPanel(this, myVideoView,  width + ControlPanel.HANDLE_WIDTH, LayoutParams.FILL_PARENT);
		layout.addView(rightControlPanel);
		rightControlPanel.fillPanelContainer(rightView);
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
		popupMenu = new VideoPopupMenu(this, mHandler, videoPopMenuItem);
		connectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
		IntentFilter connectFilter = new IntentFilter();
		intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
		registerReceiver(connectionReceiver, connectFilter);
		
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
		
		Button leftDown = (Button) view.findViewById(R.id.left_down);
		//right.setOnClickListener(this);
		//right.setOnFocusChangeListener(this);
		leftDown.setOnClickListener(this);
		
		view.findViewById(R.id.mid).setOnClickListener(this);
		
		Button RightDown = (Button) view.findViewById(R.id.right_down);
		//right.setOnClickListener(this);
		//right.setOnFocusChangeListener(this);
		RightDown.setOnClickListener(this);
		
		Button sendAudio = (Button) view.findViewById(R.id.send_audio);
		sendAudio.setOnClickListener(this);
		
		qvga = (RadioButton) view.findViewById(R.id.qvga_button_id);
		//qvga.setOnCheckedChangeListener(checkedChangeListener);
		qvga.setOnClickListener(this);
		vga = (RadioButton) view.findViewById(R.id.vga_button_id);
		//vga.setOnCheckedChangeListener(checkedChangeListener);
		vga.setOnClickListener(this);
		qelp = (RadioButton) view.findViewById(R.id.qelp_button_id);
		//qelp.setOnCheckedChangeListener(checkedChangeListener);
		qelp.setOnClickListener(this);
		
		/**/
		Button buttonMinusZoom = (Button) view.findViewById(R.id.minus_zoom); 
		buttonMinusZoom.setOnClickListener(this);
		///画面质量
		clearProgerss = (BCVControlProgressBar) view.findViewById(R.id.clear_progressbar);
		clearProgerss.init(getText(R.string.video_preview_clear).toString());
		clearProgerss.setProgress(0);
		Button buttonAddQuality = (Button) view.findViewById(R.id.add_quality); 
		buttonAddQuality.setOnClickListener(this);
		Button buttonMinusQuality = (Button) view.findViewById(R.id.minus_quality); 
		buttonMinusQuality.setOnClickListener(this);
		
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
			if(!myVideoView.getPlayStatus()) {
				myVideoView.setStopPlay(true, false);
				return;
			}
			if(NetworkUtil.checkNetwokEnable(CamVideoH264.this)) {
				mHandler.sendEmptyMessage(Constants.WEB_CAM_SHOW_CHECK_PWD_DLG_MSG);
				myVideoView.setPlayBackFlag(false);
				updateComponent(true);
				mHandler.sendEmptyMessage(PlayBackConstants.HIDE_SEEKBAR_LAYOUT);
				new AsynMonitorSocketTask().execute("");
			} else {
				handleNetworkOpeartion(CamVideoH264.this);
			}
		}
	};
	
	
	@Override
	public void onClick(View v) {
		if(v.getId() == R.id.videoview) {
			if(firtick == 0l){
				firtick = System.currentTimeMillis();//前一次点击的时间
			}else if(sectick == 0l){
				sectick = System.currentTimeMillis();
				distance = sectick - firtick;
				if(distance > 0l && distance < 500l){
					// 时间范围自由设定，如果为true表明是连续点击；
					firtick = 0l;
					sectick = 0l;
					myVideoView.zoomCanvas();
				}else{
					//不是连续点击
					firtick = System.currentTimeMillis();//重新获取前一次点击的时间
					sectick = 0l;
					rightControlPanel.onClick();
				} 
			}
			return;
		}
		if(v.getId() == R.id.right_down){
			startActivity(new Intent(this,ImageGrid.class));
			return;
		}
		if(myVideoView.isStopPlay()) {
			//Toast.makeText(this, "return",Toast.LENGTH_SHORT).show();
			return ;
		}
		//PackageUtil.sendPTZCommond(myVideoView.getCmdSocket(), netUtil, device, WinTaiCmd.PTZ_CMD_LEFT.ordinal());
		switch(v.getId()) {
		case R.id.add_quality:
			int value0 = clearProgerss.getProgress();
			value0 += 10;
			if(value0 >=100) {
				value0 = 100;
			}
			if(PackageUtil.setBCV(camManager.getSelectDevice().getDeviceID(),CamCmdListHelper.SetCmp_Set_Quality, value0+"")>0){
				clearProgerss.setProgress(value0);
			}
			break;
		case R.id.minus_quality:
			int value1 = clearProgerss.getProgress();
			value1 -= 10;
			if(value1 <=0) {
				value1 = 0;
			}
			if(PackageUtil.setBCV(camManager.getSelectDevice().getDeviceID(),CamCmdListHelper.SetCmp_Set_Quality, value1+"")>0){
				clearProgerss.setProgress(value1);
			}
			break;
		case R.id.mid:
			settings.edit().putBoolean("REVERSE", !myVideoView.isReverseFlag()).commit();
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
			final Button sendMinAudioVolume = (Button) rightView.findViewById(R.id.send_audio);
			if(sendMinAudioVolume.getText().toString().equals(getText(R.string.video_preview_send_audio_close))) {
				int vv = volumeProgressbar.getProgress();
				if(--vv<1){
					vv = 1;
				}
				String item = CamCmdListHelper.SetAudioTalkVolume + CamCmdListHelper.audioTalk[vv-1][1] + ":" + CamCmdListHelper.audioTalk[vv-1][0];
				Log.d(TAG, "### Speak Audio " + item + "  vv = "+ (vv-1));
				int re = UdtTools.sendCmdMsg(item, item.length());
				if(re>0) {
					volumeProgressbar.setProgress(vv);
				}
				return;
			}
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
			final Button sendAddAudioVolume = (Button) rightView.findViewById(R.id.send_audio);
			if(sendAddAudioVolume.getText().toString().equals(getText(R.string.video_preview_send_audio_close))) {
				int vv = volumeProgressbar.getProgress();
				if(++vv>3){
					vv = 3;
				}
				String item = CamCmdListHelper.SetAudioTalkVolume + CamCmdListHelper.audioTalk[vv-1][1] + ":" + CamCmdListHelper.audioTalk[vv-1][0];
				Log.d(TAG, "### Speak Audio " + item + "  vv = "+ (vv-1));
				int resu = UdtTools.sendCmdMsg(item, item.length());
				if(resu>0) {
					volumeProgressbar.setProgress(vv);
				}
				return;
			}
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
			//startActivity(new Intent(this,ImageViewer.class));
			startActivity(new Intent(this,ImageGrid.class));
			break;
		case R.id.send_audio:
			Log.d(TAG, "### open oper flag = "  + f);
			if(f) {
				f = false;
				final Button sendAudio = (Button) rightView.findViewById(R.id.send_audio);
				if(sendAudio.getText().toString().equals(getText(R.string.video_preview_send_audio_open))) {// open
					new Thread(new Runnable(){
						public void run() {
							int res = UdtTools.sendCmdMsg(CamCmdListHelper.SetAudioTalkOn, CamCmdListHelper.SetAudioTalkOn.length());
							Message m = mHandler.obtainMessage();
							m.what = Constants.CONNECTERROR;
							if(res > 0) {
								int length = 20;
								byte[] recv = new byte[length];
								res = UdtTools.recvCmdMsg(recv, length);
								if(res > 0) {
									String result = new String(recv,0,res);
									Log.d(TAG, "### open result = "  + result);
									if("talk_ok".equalsIgnoreCase(result)) {
										m.arg1 = R.string.video_preview_send_audio_open_success;
										mHandler.sendEmptyMessage(Constants.SENDGETTHREEPORTMSG);
										String item = CamCmdListHelper.SetAudioTalkVolume + CamCmdListHelper.audioTalk[0][1] + ":" + CamCmdListHelper.audioTalk[0][0];
										UdtTools.sendCmdMsg(item, item.length());
										Log.d(TAG, "### Speak Audio " + item);
										myVideoView.setOpenSendAudioFlag(true);
										volumeProgressbar.setMax(3);
										volumeProgressbar.setProgress(1);
									}else if("talk_busy".equalsIgnoreCase(result)){
										m.arg1 = R.string.video_preview_send_audio_open_error_tips;
									}else {
										m.arg1 = R.string.video_preview_send_audio_open_error;
									}
								}else {
									m.arg1 = R.string.video_preview_send_audio_open_error;
								}
							}else {
								m.arg1 = R.string.video_preview_send_audio_open_error;
							}
							mHandler.sendMessage(m);
							f = true;
						}
					}).start();
				}else { //close
					String item = CamCmdListHelper.SetAudioTalkOff;
					int res = UdtTools.sendCmdMsg(item , item.length());
					Message m = mHandler.obtainMessage();
					m.what = Constants.CONNECTERROR;
					if(res > 0) {
						sendAudio.setText(R.string.video_preview_send_audio_open);
						m.arg1 = R.string.video_preview_send_audio_close_success;
						myVideoView.setOpenSendAudioFlag(false);
						if(info != null) {
							volumeProgressbar.setMax(100);
							volumeProgressbar.setProgress(info.getVolume()>0?info.getVolume():0);
						}
					}else {
						m.arg1 = R.string.video_preview_send_audio_close_error;
					}
					f = true;
					mHandler.sendMessage(m);
				}
			}
			break;
		//radion button
		case R.id.qvga_button_id:
			qvga.setChecked(true);
			vga.setChecked(false);
			qelp.setChecked(false);
			new AsynCheckResoluTask().execute(CamCmdListHelper.resolArr[0]);
			settings.edit().putInt("QUALITY", 0).commit();
			break;
		case R.id.vga_button_id:
			qvga.setChecked(false);
			vga.setChecked(true);
			qelp.setChecked(false);
			new AsynCheckResoluTask().execute(CamCmdListHelper.resolArr[1]);
			settings.edit().putInt("QUALITY", 1).commit();
			break;
		case R.id.qelp_button_id:
			qvga.setChecked(false);
			vga.setChecked(false);
			qelp.setChecked(true);
			new AsynCheckResoluTask().execute(CamCmdListHelper.resolArr[2]);
			settings.edit().putInt("QUALITY", 2).commit();
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
				new AsynSendPTZ().execute(new Integer[]{WinTaiCmd.PTZ_CMD_STOP.ordinal()});
				//PackageUtil.sendPTZCommond(WinTaiCmd.PTZ_CMD_STOP.ordinal());
			}
			
			break;
		case R.id.right_down:
			if(f) {
				f = false;
				//startActivity(new Intent(this,ImageViewer.class));
				startActivity(new Intent(this, ImageGrid.class));
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
		StatService.onResume(this);
		previewDeviceAdapter = new VideoPreviewDeviceAdapter(camManager.getCamList(), CamVideoH264.this);
		listView.setAdapter(previewDeviceAdapter);
		ToastUtils.setListViewHeightBasedOnChildren(listView);
		//Toast.makeText(CamVideoH264.this, "onResume", Toast.LENGTH_LONG).show();
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		StatService.onPause(this);
	}
	
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        screenWidth = dm.widthPixels;
        screenHeight = dm.heightPixels;
        myVideoView.init(mHandler,screenWidth, screenHeight, settings.getBoolean("REVERSE", false), settings.getInt("QUALITY", 1));
	}
	
	@Override
	protected void onStop() {
		super.onStop();
	}

	
	@Override
    protected void onDestroy() {
    	super.onDestroy();
    	if(mWakeLock.isHeld() == true) {
   		 	mWakeLock.release();
        }
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
    	
		unregisterReceiver(connectionReceiver);
    }
	
	private void showProgressDlg(int textId) {
		
		if(m_Dialog == null) {
			//m_Dialog = new ProgressDialog(this);
			m_Dialog = CustomProgressDialog.createDialog(this, R.style.CustomProgressDialog);  
		}
		if(m_Dialog != null) {
			if(BuildConfig.DEBUG) {
				Log.d(TAG, "### " + camManager.getSelectDevice().getDeviceID());
			}
			m_Dialog.setMessage(getResources().getString(textId, camManager.getSelectDevice().getDeviceID()));
			if(!m_Dialog.isShowing()) {
				m_Dialog.show();
			}
		}
	}
	
	private void hideProgressDlg() {
		if(m_Dialog != null && m_Dialog.isShowing()) {
			m_Dialog.dismiss();
			m_Dialog = null;
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
				previewDeviceAdapter = new VideoPreviewDeviceAdapter(camManager.getCamList(), CamVideoH264.this);
				listView.setAdapter(previewDeviceAdapter);
				ToastUtils.setListViewHeightBasedOnChildren(listView);
				//Toast.makeText(CamVideoH264.this, "aaa", Toast.LENGTH_LONG).show();
			}
		}	
	};

	private class IpPlayReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			if(WebCamActions.ACTION_IPPLAY.equals(intent.getAction())) {
				if(!myVideoView.getPlayStatus()) {
					myVideoView.setStopPlay(true, false);
					return;
				}
				if(NetworkUtil.checkNetwokEnable(context)) {
					mHandler.sendEmptyMessage(Constants.WEB_CAM_SHOW_CHECK_PWD_DLG_MSG);
					myVideoView.setPlayBackFlag(false);
					updateComponent(true);
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
					updateComponent(false);
					mHandler.sendEmptyMessage(PlayBackConstants.SHOW_SEEKBAR_LAYOUT);
					during = bundle.getLong("TOTALTIME");
					startTime = bundle.getLong("STARTTIME");
					playBackDeviceIndex = bundle.getInt("PLAYBACKDEVICEINDEX");
					camManager.setSelectInde(playBackDeviceIndex);
					//Toast.makeText(CamVideoH264.this, "monit", 3).show();
					mHandler.sendEmptyMessage(Constants.WEB_CAM_SHOW_CHECK_PWD_DLG_MSG);
					new AsynMonitorSocketTask().execute(indexStr);
				}
			}
		}
	}
	
	private void updateComponent(boolean flag) {
		rightView.findViewById(R.id.mid_up).setEnabled(flag); 
		rightView.findViewById(R.id.mid_down).setEnabled(flag);
		rightView.findViewById(R.id.left).setEnabled(flag);
		rightView.findViewById(R.id.right).setEnabled(flag);
		//rightView.findViewById(R.id.mid).setEnabled(flag);
		rightView.findViewById(R.id.minus_zoom).setEnabled(flag);
		rightView.findViewById(R.id.add_zoom).setEnabled(flag);
		rightView.findViewById(R.id.minus_foucs).setEnabled(flag);
		rightView.findViewById(R.id.add_foucs).setEnabled(flag);
		rightView.findViewById(R.id.minus_apertrue).setEnabled(flag);
		rightView.findViewById(R.id.add_apertrue).setEnabled(flag);
		rightView.findViewById(R.id.add_quality).setEnabled(flag);
		rightView.findViewById(R.id.minus_quality).setEnabled(flag);
		
	}
	
	class AsynMonitorSocketTask extends AsyncTask<String, Integer, Void> {
		
		@Override
		protected Void doInBackground(String... params) {
			stopPlayThread();
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			playBackFlag = params[0];
			Device device = camManager.getSelectDevice();
			String result = UdtTools.monitorSocket(device.getDeviceID());
			if(BuildConfig.DEBUG) {
				Log.d(TAG, "monitor result = " + result);
			}
			analyseResult(result);
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
	
	private void analyseResult(String result) {
		/*switch (result) {
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
		}*/
		//mHandler.sendEmptyMessage(Constants.WEB_CAM_CONNECT_INIT_MSG);
		//String random = RandomUtil.generalRandom();
		//Log.d(TAG, "random = " + random);
		if("OK".equalsIgnoreCase(result)) {
			int initRes = 1;//UdtTools.initialSocket(device.getDeviceID(),random);
			if(initRes<0) {
				Log.d(TAG, "initialSocket init error!");
				mHandler.sendEmptyMessage(Constants.WEB_CAM_HIDE_CHECK_PWD_DLG_MSG);
				MessageUtils.sendErrorMessage(mHandler,R.string.webcam_connect_init_error);
			}else {
				checkPwdState();
			}
		} else {
			MessageUtils.sendErrorMessage(mHandler,result);
			mHandler.sendEmptyMessage(Constants.WEB_CAM_HIDE_CHECK_PWD_DLG_MSG);
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
			MessageUtils.sendErrorMessage(mHandler,R.string.device_manager_pwd_set_err);
			mHandler.sendEmptyMessage(Constants.WEB_CAM_HIDE_CHECK_PWD_DLG_MSG);
		} else {
			MessageUtils.sendErrorMessage(mHandler, R.string.device_manager_time_out_or_device_off_line);
			mHandler.sendEmptyMessage(Constants.WEB_CAM_HIDE_CHECK_PWD_DLG_MSG);
		}
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
				String deviceMode = PackageUtil.getConfigMode(device);
				Log.d(TAG, "### play back vodio index = " + playBackFlag + "  device mode = " + deviceMode);
				Message msg = mHandler.obtainMessage();
				msg.what = UPDATE_COMPONENT;
				msg.obj = deviceMode;
				mHandler.sendMessage(msg);
				if(playBackFlag != null && playBackFlag.length()>0) {
					String item = CamCmdListHelper.SetCmd_Play_Back + playBackFlag;
					int res = UdtTools.sendCmdMsg(item, item.length());
					Log.d(TAG, "### send play flag res = " + res);
					if(res>0) {
						mHandler.sendEmptyMessage(Constants.CONNECTTING);
					}else {
						MessageUtils.sendErrorMessage(mHandler,R.string.device_manager_time_out_or_device_off_line);
					}
				}else {
					mHandler.sendEmptyMessage(Constants.CONNECTTING);
				}
			} else if(checkPwd == -1) {
				mHandler.sendEmptyMessage(Constants.WEB_CAM_HIDE_CHECK_PWD_DLG_MSG);
				MessageUtils.sendErrorMessage(mHandler,R.string.device_manager_pwd_set_err);
				mHandler.sendEmptyMessage(Constants.SEND_SHOW_INPUT_ONE_PASS_DIALOG_SMG);
			} else {
				mHandler.sendEmptyMessage(Constants.WEB_CAM_HIDE_CHECK_PWD_DLG_MSG);
				MessageUtils.sendErrorMessage(mHandler,R.string.device_manager_time_out_or_device_off_line);
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
			seekingFlag = true;
			if(BuildConfig.DEBUG) {
				Log.d(TAG, "### onStartTrackingTouch = " + seekBar.getProgress());
			}
		}
	
		@Override
		public void onStopTrackingTouch(SeekBar seekBar) {
			seekingFlag = false;
			int progress = seekBar.getProgress()/15;
			//currentTextView.setText(StringUtils.makeTimeString(CamVideoH264.this, progress *15));
			//currentTextView.setText(DateUtil.formatTimeToDate6(progress * 1000+startTime));
			int index = progress * 4;
			if(BuildConfig.DEBUG){
				Log.d(TAG, "progress=" + progress +" " + index  + " table2.length = " + table2.length);
			}
			if(table2 != null && table2.length>=index+4) {
					int seekPos = ByteUtil.byteToInt4(table2, index);
					Log.d(TAG, "### onStopTrackingTouch  seek value=" + seekBar.getProgress() + " timeIndex = " + progress + "  index=" + index + "  seek value = " + seekPos);
					myVideoView.setTime(DateUtil.formatTimeToDate6(progress * 1000+startTime));
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
			String item = CamCmdListHelper.SetCmd_Seek + params[0];
			int res = UdtTools.sendCmdMsg( item, item.length());
			if(BuildConfig.DEBUG)
			Log.d(TAG, "### Seek cmd = " + item + " seek result = " + res);
			return null;
		}
	}
	
	private class AsynCheckResoluTask extends AsyncTask<String, String, Void> {
		
		@Override
		protected Void doInBackground(String... params) {
			String item = CamCmdListHelper.SetVideoResol + params[0];
			int res = UdtTools.sendCmdMsg( item, item.length());
			if(BuildConfig.DEBUG) {
				Log.d(TAG, "### check resulation = " + item + " seek result = " + res );
			}
			//startActivity(new Intent(CamVideoH264.this, PopupActivity.class));
			if(res>0) {
				mHandler.sendEmptyMessage(Constants.SHOW_POP_UP_TIPS_DIA_MSG);
			}
			return null;
		}
	}
	
	private OnLongClickListener longClickListener = new OnLongClickListener() {
		
		@Override
		public boolean onLongClick(View v) {
			if(!myVideoView.getPlayStatus()) {
				if(!popupMenu.isShowing()) {
					popupMenu.showAtLocation(myVideoView, Gravity.NO_GRAVITY, x_offSet-5, y_offSet-5);
				}
			}
			return true;
		}
	};
	
	private OnTouchListener videoViewOnTouch = new OnTouchListener() {
		
		@Override
		public boolean onTouch(View v, MotionEvent event) {
			x_offSet = (int)event.getX();
			y_offSet = (int) event.getY();
			return false;
		}
	};
	
	private void updateResulation(int id) {
		 if(id == 0) {
			qvga.setChecked(true);
			vga.setChecked(false);
			qelp.setChecked(false);
			updateControlButtion(View.VISIBLE);
		}else if(id == 1) {
			qvga.setChecked(false);
			vga.setChecked(true);
			qelp.setChecked(false);
			updateControlButtion(View.VISIBLE);
		}else if(id == 2) {
			qvga.setChecked(false);
			vga.setChecked(false);
			qelp.setChecked(true);
			updateControlButtion(View.VISIBLE);
		} else {
			updateControlButtion(View.GONE);
		}
	}
	
	private void updateControlButtion(int flag) {
		qvga.setVisibility(flag);
		vga.setVisibility(flag);
		qelp.setVisibility(flag);
	}
	
	private BroadcastReceiver connectionReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			NetworkInfo mobNetInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
			NetworkInfo wifiNetInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
			if (mobNetInfo.isConnected() || wifiNetInfo.isConnected()) {//update
				Log.i(TAG, "internet connect");
				if(myVideoView.isActivated()) {
					mHandler.sendEmptyMessage(Constants.WEB_CAM_SHOW_CHECK_PWD_DLG_MSG);
					myVideoView.setPlayBackFlag(false);
					updateComponent(true);
					mHandler.sendEmptyMessage(PlayBackConstants.HIDE_SEEKBAR_LAYOUT);
					new AsynMonitorSocketTask().execute("");
				}
			} else {
				Log.i(TAG, "internet unconnect");
				stopPlayThread();
			}
		}
	};
	
	private void updateComponentByMode(String mode) {
		if(BuildConfig.DEBUG){
			Log.d(TAG, "### device mode =  " + mode);
		}
		if(mode == null || mode.length()==0 || "IP1001".equalsIgnoreCase(mode)) {
			/*
			 * [IP1001]
			 * # 没有高清分辨率 
			 * # 没有分辨率切换功能
			 * # 没有降噪强度
			 * # 没有画质调节
			 * # 没有cif分辨率（355*288）
			 */
			updateQuality(false);// 画质调节
			updateAudioTalk(true);
			updateYuntai(true);
		}else if("IP2001".equalsIgnoreCase(mode) || "IP2001C".equalsIgnoreCase(mode) || "IP2001A".equalsIgnoreCase(mode)) {
			/*
			 * [IP2001]
			 * # 没有智能监控下的方案选择 :　更清晰 | 更流畅
			 * # 没有监控模式选择
			 * # 没有QVGA分辨率
			 * 
			 */
			updateAudioTalk(true);
			updateYuntai(true);
			updateCIF(false);
			updateHD(true);
			updateVGA(true);
		}else if("IP1001S".equalsIgnoreCase(mode)) {
			/*
			 * [IP1001S]
			 * # 没有智能监控下的方案选择 :　更清晰 | 更流畅
			 * # 没有cif分辨率
			 */
			updateCIF(false);
			updateAudioTalk(true);
			updateYuntai(true);
		}else if("IP2002A".equalsIgnoreCase(mode) || "IP2002C".equalsIgnoreCase(mode)) {
			/*
			 * [IP2002A]
			 * # 没有智能监控下的方案选择 :　更清晰 | 更流畅
			 * # 没有监控模式选择
			 * # 没有QVGA分辨率
			 * # 没有语音对讲
			 * # 没有云台功能
			 * # 没有wifi
			 */
			updateAudioTalk(false);
			updateYuntai(false);
			updateCIF(true);
			updateHD(true);
			updateVGA(true);
		}else if("IP1001SC".equalsIgnoreCase(mode) || "IP1001SA".equalsIgnoreCase(mode)) {
			/*
			 * [IP1001SC]
			 * # 没有智能监控下的方案选择 :　更清晰 | 更流畅
			 * # 没有监控模式选择
			 * # 没有高清分辨率
			 * # 没有QVGA分辨率
			 */
			updateAudioTalk(true);
			updateYuntai(true);
			updateHD(false);
			updateVGA(true);
			updateCIF(false);
		}	
	}
	
	private void updateAudioTalk(boolean flag) {
		rightView.findViewById(R.id.send_audio).setEnabled(flag);
	}
	
	private void updateQuality(boolean flag) {
		rightView.findViewById(R.id.add_quality).setEnabled(flag);
		rightView.findViewById(R.id.minus_quality).setEnabled(flag);
	}
	
	private void updateYuntai(boolean flag) {
		rightView.findViewById(R.id.mid_up).setEnabled(flag);
		rightView.findViewById(R.id.mid_down).setEnabled(flag);
		rightView.findViewById(R.id.left).setEnabled(flag);
		rightView.findViewById(R.id.right).setEnabled(flag);
	}
	
	private void updateCIF(boolean flag) {
		qvga.setEnabled(flag);
	}
	
	private void updateHD(boolean flag) {
		qelp.setEnabled(flag);
	}
	
	private void updateVGA(boolean flag) {
		vga.setEnabled(flag);
	}
}