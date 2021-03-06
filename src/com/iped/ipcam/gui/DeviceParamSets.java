package com.iped.ipcam.gui;

import java.net.DatagramSocket;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.app.ProgressDialog;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.Spinner;
import android.widget.TimePicker;
import android.widget.Toast;

import com.baidu.mobstat.StatService;
import com.iped.ipcam.engine.CamMagFactory;
import com.iped.ipcam.engine.CamParasSetImp;
import com.iped.ipcam.engine.ICamManager;
import com.iped.ipcam.exception.CamManagerException;
import com.iped.ipcam.pojo.Device;
import com.iped.ipcam.pojo.WifiConfig;
import com.iped.ipcam.utils.CamCmdListHelper;
import com.iped.ipcam.utils.Constants;
import com.iped.ipcam.utils.DateUtil;
import com.iped.ipcam.utils.DialogUtils;
import com.iped.ipcam.utils.FileUtil;
import com.iped.ipcam.utils.PackageUtil;
import com.iped.ipcam.utils.ParaUtil;
import com.iped.ipcam.utils.ProgressUtil;
import com.iped.ipcam.utils.ThroughNetUtil;
import com.iped.ipcam.utils.ToastUtils;
import com.iped.ipcam.utils.WirelessAdapter;

public class DeviceParamSets extends Activity implements OnClickListener {

	private ICamManager camManager = CamMagFactory.getCamManagerInstance();

	private CamParasSetImp camParasSet = CamMagFactory.getCamParasSetInstance();

	private Button paramBackButton = null;
	
	private Device device = null;

	private EditText deviceNameEditText = null;

	private EditText deviceIdEditText = null;

	private EditText versionEditText = null;

	private EditText tfCardEditText = null;

	private EditText sdCardEditText = null;

	private RadioGroup changeStorageMode = null;

	private EditText valueableRecordeTimeEditText = null;

	private RadioGroup netWorkModeSet = null;

	private RadioGroup wireuseNetworkSet = null;

	private EditText wireuseIPAddress = null;

	private EditText wireuseGeteWayAddess = null;

	private EditText wireuseSubAddess = null;

	private EditText wireuseDNS1Address = null;

	private EditText wireuseDNS2Address = null;

	private RadioGroup wirelessNetworkSet = null;

	private EditText wirelessIPAddress = null;

	private EditText wirelessGeteWayAddess = null;

	private EditText wirelessSubAddess = null;

	private EditText wirelessDNS1Address = null;

	private EditText wirelessDNS2Address = null;

	// 无线列表
	private EditText apnEditText = null;
	
	private EditText apnPwdEditText = null;
	
	private Spinner wirelessSpinner = null;

	private RadioGroup soundFlagSet = null;
	
	private RadioGroup mobileAlarmSet = null;
	
	private Spinner sensitvSet = null;
	
	private RadioButton recordSetOne = null;
	
	private Spinner recordRateOne = null;
	
	private Spinner recordFrameSizeOne = null;
	
	private RadioButton selfSetMonitor = null;
	
	private RadioButton selfIntelMonitor = null;
	
	private Spinner selfSetMonitorOneRate = null;
	
	private Spinner selfSetMonitorOneFrameSize = null;
	
	private EditText alarmEmailEditText = null;
	
	private EditText securityVisitPass = null;
	
	private Button dateSetButton = null;
	
	private Button timeSetButton = null;
	
	private Button updateSystemTime = null;
	
	private Button modifySecurityPwdButton = null;
	
	private Map<String, String> paraMap = null; 
	
	private DatagramSocket tmpDatagramSocket = null;
	
	private String ip;
	
	private int port1;
	
	private List<WifiConfig> wifiList = null;
	
	private String TAG = "DeviceParamSets";
	
	private ProgressDialog m_Dialog = null;
	
	private RadioGroup intelligenMonitorRadioGrop = null;
	
	private int monitorSetFlag = 0;
	
	private Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case Constants.SHOWQUERYCONFIGDLG:
				ProgressUtil.showProgress(R.string.device_params_request_config_str, DeviceParamSets.this);
				camParasSet.getCamPara(device, this);
				break;
			case Constants.HIDEQUERYCONFIGDLG:
				int w = msg.arg1;
				if(w == -1 || w == -2) {
					ToastUtils.showToast(DeviceParamSets.this, R.string.device_manager_pwd_set_err);
					DeviceParamSets.this.finish();
				} else {
					paraMap = camParasSet.getParaMap();
					initializeEditText(paraMap);
					ProgressUtil.hideProgress();
				}
				break;
			case Constants.QUERYCONFIGERROR:
				ProgressUtil.hideProgress();
				ToastUtils.showToast(DeviceParamSets.this, R.string.device_params_request_error_str);
				DeviceParamSets.this.finish();
				break;
			case Constants.SENDGETTHREEPORTMSG:
				ThroughNetUtil netUtil = null;
				tmpDatagramSocket = netUtil.getPort1();
				handler.sendEmptyMessage(Constants.SENDDATAWHENMODIFYCONFIG);
				ProgressUtil.hideProgress();
				if(netUtil != null) {
					Bundle bd = msg.getData();
					if(bd != null) {
						ip = bd.getString("IPADDRESS");
						port1 = bd.getInt("PORT1");
						//int port2 = bd.getInt("PORT2");
						//int port3 = bd.getInt("PORT3");
						String cmd = null;
						try {
							device.setUnDefine1(ip);
							device.setDeviceRemoteCmdPort(port1);
							int checkPwd = PackageUtil.checkPwd(device.getDeviceID(),device.getUnDefine2());
							if(checkPwd == 1) {
								cmd = PackageUtil.CMDPackage2(netUtil, CamCmdListHelper.GetCmd_Config + device.getUnDefine2(), ip, port1);
								paraMap = new LinkedHashMap<String,String>();
								ParaUtil.putParaByString(cmd, paraMap);
								initializeEditText(paraMap);
							} else {
								DialogUtils.inputOnePasswordDialog(DeviceParamSets.this, handler, Constants.SEND_REQUERY_CONFIG_PWD_ERROR);
							}
						} catch (CamManagerException e) {
							handler.sendEmptyMessage(Constants.QUERYCONFIGERROR);
							Log.d(TAG, "CamManagerException = " + e.getLocalizedMessage());
							return;
						}
					}
				}
				break;
			case Constants.SENDGETUNFULLPACKAGEMSG:
				//showToast(R.string.device_manager_find_device_id_error);
				ProgressUtil.hideProgress();
				Toast.makeText(DeviceParamSets.this, getText(R.string.device_params_request_config_error_str), Toast.LENGTH_LONG).show();
				handler.sendEmptyMessage(Constants.HIDETEAUTOSEARCH);
				DeviceParamSets.this.finish();
				break;
			case Constants.SENDGETTHREEPORTTIMOUTMSG:
				handler.sendEmptyMessage(Constants.SENDGETUNFULLPACKAGEMSG);
				break;
			case Constants.SETCONFIGDLG:
				ProgressUtil.showProgress(R.string.device_params_set_config_str, DeviceParamSets.this);
				break;
			case Constants.SENDDATAWHENMODIFYCONFIG:
				break;
			case Constants.SENDSETCONFIGSUCCESSMSG:
				//ToastUtils.showToast(DeviceParamSets.this, R.string.device_params_set_config_success_str);
				DeviceParamSets.this.finish();
				break;
			case Constants.SENDSETCONFIGERRORMSG:
				ProgressUtil.hideProgress();
				//ToastUtils.showToast(DeviceParamSets.this, R.string.device_params_set_config_error_str);
				break;
			case Constants.RETSETCONFIGSUCCESS:
				ToastUtils.showToast(DeviceParamSets.this, R.string.device_params_reset_config_success_str);
				handler.sendEmptyMessage(Constants.SENDSETCONFIGSUCCESSMSG);
				break;
			case Constants.RETSETCONFIGERROR:
				ToastUtils.showToast(DeviceParamSets.this, R.string.device_params_reset_config_error_str);
				handler.sendEmptyMessage(Constants.SENDSETCONFIGSUCCESSMSG);
				break;
			case Constants.SENDSEARCHWIRELESSMSG:
				ProgressUtil.showProgress(R.string.device_params_apn_set_search_wireless_str, DeviceParamSets.this);
				break;
			case Constants.SENDSEARCHWIRELESSSUCCESSMSG:
				ToastUtils.showToast(DeviceParamSets.this, R.string.device_params_apn_set_search_wireless_success_str);
				initWirelessList();
				break;
			case Constants.SENDSEARCHWIRELESSERRORMSG:
				ToastUtils.showToast(DeviceParamSets.this, R.string.device_params_apn_set_search_wireless_error_str);
				break;
			case Constants.SENDCONFIGMSG:
				send(paraMap);
				break;
			case Constants.SEND_REQUERY_CONFIG_PWD_ERROR:
				String pwd = (String) msg.obj;
				String cmd = "";
				try {
					device.setUnDefine2(pwd);
					int checkPwd = PackageUtil.checkPwd(device.getDeviceID(),device.getUnDefine2());
					if(checkPwd == 1) {
						camManager.updateCam(device);
						cmd = PackageUtil.CMDPackage2(null, CamCmdListHelper.GetCmd_Config + pwd, ip, port1);
						paraMap = new LinkedHashMap<String,String>();
						ParaUtil.putParaByString(cmd, paraMap);
						initializeEditText(paraMap);
					} else {
						ToastUtils.showToast(DeviceParamSets.this, R.string.device_manager_pwd_set_err);
						DeviceParamSets.this.finish();
					}
				} catch (CamManagerException e) {
					handler.sendEmptyMessage(Constants.QUERYCONFIGERROR);
					Log.d(TAG, "CamManagerException = " + e.getLocalizedMessage());
					return;
				}
				break;
			case Constants.SEND_UPDATE_NEW_PASSWORD_MSG:
				String newPwd = (String) msg.obj;
				device.setUnDefine2(newPwd);
				camManager.updateCam(device);
				break;
			default:
				break;
			}
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.device_param_sets);
		lookupEditText();
		device = camManager.getSelectDevice();
		handler.sendEmptyMessage(Constants.SHOWQUERYCONFIGDLG);
	}

	private void lookupEditText() {
		paramBackButton = (Button) findViewById(R.id.common_back);
		deviceNameEditText = (EditText) findViewById(R.id.device_param_set_name);
		deviceIdEditText = (EditText) findViewById(R.id.device_param_set_inentify);
		versionEditText = (EditText) findViewById(R.id.device_param_set_version);
		tfCardEditText = (EditText) findViewById(R.id.device_param_set_tfcard_tatal_space);
		sdCardEditText = (EditText) findViewById(R.id.device_param_set_sdcard_tatal_space);
		changeStorageMode = (RadioGroup) findViewById(R.id.device_param_set_storage_device_mode);
		valueableRecordeTimeEditText = (EditText) findViewById(R.id.device_param_set_valueable_recorde_time);
		netWorkModeSet = (RadioGroup) findViewById(R.id.device_param_set_network_mode);
		wireuseNetworkSet = (RadioGroup) findViewById(R.id.device_param_set_wireuse_network_mode);
		wireuseIPAddress = (EditText) findViewById(R.id.device_param_set_wireuse_ip_address);
		wireuseGeteWayAddess = (EditText) findViewById(R.id.device_param_set_wireuse_gateway_address);
		wireuseSubAddess = (EditText) findViewById(R.id.device_param_set_wireuse_subway_address);
		wireuseDNS1Address = (EditText) findViewById(R.id.device_param_set_wireuse_dns1_address);
		wireuseDNS2Address = (EditText) findViewById(R.id.device_param_set_wireuse_dns2_address);
		wirelessNetworkSet = (RadioGroup) findViewById(R.id.device_param_set_wireless_network_mode);
		wirelessIPAddress = (EditText) findViewById(R.id.device_param_set_wireless_ip_address);
		wirelessGeteWayAddess = (EditText) findViewById(R.id.device_param_set_wireless_gateway_address);
		wirelessSubAddess = (EditText) findViewById(R.id.device_param_set_wireless_subway_address);
		wirelessDNS1Address = (EditText) findViewById(R.id.device_param_set_wireless_dns1_address);
		wirelessDNS2Address = (EditText) findViewById(R.id.device_param_set_wireless_dns2_address);
		apnEditText = (EditText) findViewById(R.id.device_params_apn_set_name_id);
		apnPwdEditText = (EditText) findViewById(R.id.device_params_apn_set_pwd_id);
		findViewById(R.id.device_params_apn_set_search_wireles_id).setOnClickListener(this);
		wirelessSpinner = (Spinner) findViewById(R.id.device_params_apn_set_wireless_list_id);
		
		//frameRateSpinner = (Spinner) findViewById(R.id.device_params_set_recorde_frame_rate_one_id);
		recordSetOne = (RadioButton) findViewById(R.id.device_params_video_record_set_one_id);
		recordRateOne = (Spinner) findViewById(R.id.device_params_set_recorde_frame_rate_one_id);
		recordFrameSizeOne = (Spinner) findViewById(R.id.device_params_set_recorde_frame_size_id);
		
		selfSetMonitor = (RadioButton) findViewById(R.id.device_params_monitor_self_mode_set_id);
		selfIntelMonitor = (RadioButton) findViewById(R.id.device_params_monitor_intelligent_mode_set_id);
		selfSetMonitorOneRate = (Spinner) findViewById(R.id.device_params_monitor_one_frame_rate_id);
		selfSetMonitorOneFrameSize = (Spinner) findViewById(R.id.device_params_monitor_one_frame_size_id);
		
		soundFlagSet = (RadioGroup) findViewById(R.id.device_params_other_set_sound_open_id);
		mobileAlarmSet = (RadioGroup) findViewById(R.id.device_params_other_set_mobile_alarm_flag_id);
		sensitvSet = (Spinner) findViewById(R.id.device_params_other_set_mobile_alarm_offset_prompt_id);
		alarmEmailEditText = (EditText) findViewById(R.id.device_params_other_set_mobile_alarm_email_id);
		securityVisitPass = (EditText) findViewById(R.id.device_params_other_set_security_id);
		
		dateSetButton = (Button) findViewById(R.id.device_params_other_set_date_id);
		timeSetButton = (Button) findViewById(R.id.device_params_other_set_time_id);
		updateSystemTime = (Button) findViewById(R.id.device_params_other_set_system_time_id);
		modifySecurityPwdButton = (Button) findViewById(R.id.device_params_other_modify_security_pwd_id);
		dateSetButton.setOnClickListener(this);
		timeSetButton.setOnClickListener(this);
		updateSystemTime.setOnClickListener(this);
		modifySecurityPwdButton.setOnClickListener(this);
		findViewById(R.id.device_params_set_factory_button_id).setOnClickListener(this);
		findViewById(R.id.device_params_set_commit_button_id).setOnClickListener(this);
		findViewById(R.id.device_params_set_concle_button_id).setOnClickListener(this);

		intelligenMonitorRadioGrop = (RadioGroup) findViewById(R.id.device_params_monitor_intelligent_mode_radiogroup_id);
	}


	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.common_back:
			DeviceParamSets.this.finish();
			break;
		case R.id.device_params_apn_set_search_wireles_id:
			handler.sendEmptyMessage(Constants.SENDSEARCHWIRELESSMSG);
			searchWireless();
			break;
		case R.id.device_params_set_factory_button_id:
			new AlertDialog.Builder(this)
			.setTitle(getText(R.string.device_params_reset_config_title_str))
			.setMessage(getString(R.string.device_params_reset_config_message_str))
	        .setPositiveButton((getText(R.string.device_params_reset_config_sure_str)),
	        new DialogInterface.OnClickListener() {
	            public void onClick(DialogInterface dialog, int whichButton) {
	            	handler.sendEmptyMessage(Constants.SETCONFIGDLG);
	            	resetFactory();
	            }
	        }).setNegativeButton((getText(R.string.device_params_reset_config_cancle_str)), null)
	        .create().show();
			
			break;
		case R.id.device_params_set_commit_button_id:
			handler.sendEmptyMessage(Constants.SETCONFIGDLG);
			collectionData();
			break;
		case R.id.device_params_set_concle_button_id:
			this.finish();
			break;
		case R.id.device_params_other_set_date_id:
			Calendar calendar = Calendar.getInstance();
			Date date = DateUtil.formatTimeToDate5(dateSetButton.getText().toString());
			calendar.setTime(date);
			new DatePickerDialog(DeviceParamSets.this, new OnDateSetListener() {
				
				@Override
				public void onDateSet(DatePicker view, int year, int monthOfYear,
						int dayOfMonth) {
					dateSetButton.setText(format(year) + "-" + format(monthOfYear) + "-" + format(dayOfMonth));
				}
			}, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH)+ 1, Calendar.DAY_OF_MONTH).show();
			break;
		case R.id.device_params_other_set_time_id:
			Calendar calendar2 = Calendar.getInstance();
			Date date2 = DateUtil.formatTimeToDate5(timeSetButton.getText().toString());
			calendar2.setTime(date2);
			new TimePickerDialog(DeviceParamSets.this, new TimePickerDialog.OnTimeSetListener() {
				
				@Override
				public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
					timeSetButton.setText(format(hourOfDay) + ":" + format(minute) + ":00");
				}
			}, calendar2.get(Calendar.HOUR_OF_DAY), calendar2.get(Calendar.MINUTE), true).show();
			break;
		case R.id.device_params_other_set_system_time_id:
			String command = CamCmdListHelper.SetCmd_Set_Time + DateUtil.formatTimeToDate3(System.currentTimeMillis());
			int res = UdtTools.sendCmdMsgById(device.getDeviceID(), command, command.length());
			if(res>0) {
				ToastUtils.showToast(DeviceParamSets.this, R.string.device_params_other_set_system_success_str);
			}else {
				ToastUtils.showToast(DeviceParamSets.this, R.string.device_params_other_set_system_error_str);
			}
			break;
		case R.id.device_params_other_modify_security_pwd_id:
			DialogUtils.inputThreadPasswordDialog(DeviceParamSets.this, device, handler, Constants.SEND_UPDATE_NEW_PASSWORD_MSG,tmpDatagramSocket,ip,port1);
			break;
		default:
			break;
		}
	}
	
	private void initializeEditText(final Map<String, String> paraMap) {
		paramBackButton.setOnClickListener(this);
		deviceNameEditText.setText(device.getDeviceName());
		deviceIdEditText.setText(paraMap.containsKey("cam_id")? paraMap.get("cam_id") : "");
		deviceIdEditText.setEnabled(false);
		versionEditText.setText(paraMap.containsKey("version")? paraMap.get("version") : getCurrentVersion());
		versionEditText.setEnabled(false);
		tfCardEditText.setText(paraMap.containsKey("tfcard_maxsize")? FileUtil.formetFileSize(Long.parseLong(paraMap.get("tfcard_maxsize")) * 1024 * 1024) : "");
		tfCardEditText.setEnabled(false);
		sdCardEditText.setText(paraMap.containsKey("sdcard_maxsize")? FileUtil.formetFileSize(Long.parseLong(paraMap.get("sdcard_maxsize")) * 1024 * 1024) : "");
		sdCardEditText.setEnabled(false);
		valueableRecordeTimeEditText.setEnabled(false);
		changeStorageMode.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			
			@Override
			public void onCheckedChanged(RadioGroup group, int checkedId) {
				changeStorageMode.check(checkedId);
			}
		});
		//valueableRecordeTimeEd.setText("");
		if(paraMap.containsKey("net_mode")){
			String s = paraMap.get("net_mode");
			if("eth_only".equals(s)) {
				netWorkModeSet.check(R.id.device_param_set_network_mode_wireuse);
			}else if("wlan_only".equals(s)) {
				netWorkModeSet.check(R.id.device_param_set_network_mode_wireless);
			} else {
				netWorkModeSet.check(R.id.device_param_set_network_mode_intelligent);
			}
		}
		// wire use net set
		wireuseNetworkSet.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			
			@Override
			public void onCheckedChanged(RadioGroup group, int checkedId) {
				if(checkedId == R.id.device_param_set_wireuse_network_mode_auto) {
					lockWireuseCommont(false);
				} else {
					lockWireuseCommont(true);
				}
			}
		});
		if(paraMap.containsKey("udhcpc")) {
			String s = paraMap.get("udhcpc");
			if("1".equals(s)) {
				wireuseNetworkSet.check(R.id.device_param_set_wireuse_network_mode_auto);
				lockWireuseCommont(false);
			} else {
				wireuseNetworkSet.check(R.id.device_param_set_wireuse_network_mode_static);
			}
		}
		wireuseIPAddress.setText(paraMap.containsKey("e_ip")? paraMap.get("e_ip") : "");
		wireuseGeteWayAddess.setText(paraMap.containsKey("e_gw")? paraMap.get("e_gw") : "");
		wireuseSubAddess.setText(paraMap.containsKey("e_mask")? paraMap.get("e_mask") : "");
		wireuseDNS1Address.setText(paraMap.containsKey("e_dns1")? paraMap.get("e_dns1") : "");
		wireuseDNS2Address.setText(paraMap.containsKey("e_dns2")? paraMap.get("e_dns2") : "");
		
		// wire less net set
		wirelessNetworkSet.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			
			@Override
			public void onCheckedChanged(RadioGroup group, int checkedId) {
				if(checkedId == R.id.device_param_set_wireless_network_mode_audo) {
					lockWirelessCommont(false);
				} else {
					lockWirelessCommont(true);
				}
				
			}
		});
		
		if(paraMap.containsKey("udhcpc")) {
			String s = paraMap.get("udhcpc");
			if("1".equals(s)) {
				wirelessNetworkSet.check(R.id.device_param_set_wireless_network_mode_audo);
				lockWirelessCommont(false);
			} else {
				wirelessNetworkSet.check(R.id.device_param_set_wireless_network_static);
			}
		}
		wirelessIPAddress.setText(paraMap.containsKey("w_ip")? paraMap.get("w_ip") : "");
		wirelessGeteWayAddess.setText(paraMap.containsKey("w_gw")? paraMap.get("w_gw") : "");
		wirelessSubAddess.setText(paraMap.containsKey("w_mask")? paraMap.get("w_mask") : "");
		wirelessDNS1Address.setText(paraMap.containsKey("w_dns1")? paraMap.get("w_dns1") : "");
		wirelessDNS2Address.setText(paraMap.containsKey("w_dns2")? paraMap.get("w_dns2") : "");
		
		if(paraMap.containsKey("r_mode")) {
			String s = paraMap.get("r_mode");
			if("inteligent".equals(s)) { //normal
				recordSetOne.setChecked(true);
			} else {
				recordSetOne.setChecked(true);
			}
		}
		// 帧率
		if(paraMap.containsKey("r_nor_speed")) {
			String s = paraMap.get("r_nor_speed");
			if("2".equals(s)) {
				recordRateOne.setSelection(1);
			} else if("4".equals(s)){
				recordRateOne.setSelection(2);
			}else if("8".equals(s)){
				recordRateOne.setSelection(3);
			}else if("12".equals(s)){
				recordRateOne.setSelection(4);
			}else if("24".equals(s)){
				recordRateOne.setSelection(5);
			} else {
				recordRateOne.setSelection(0);
			}
		}
		
		// 画面尺寸
		if(paraMap.containsKey("r_reso")) {
			String s = paraMap.get("r_reso");
			if("qvga".equalsIgnoreCase(s)){
				recordFrameSizeOne.setSelection(1);
			}else {
				recordFrameSizeOne.setSelection(0);
			}
		}
		/*
		 * 0        有智能监控下的方案选择 :　更清晰 | 更流畅    有监控模式选择
		 * 1   没有智能监控下的方案选择 :　更清晰 | 更流畅  没有监控模式选择
		 * 2   没有智能监控下的方案选择 :　更清晰  | 更流畅 
		 */
		monitorSetFlag = 0;
		if(paraMap.containsKey("model")) {
			String model = paraMap.get("model");
			if("IP2001".equalsIgnoreCase(model) 
					|| "IP2002A".equalsIgnoreCase(model) || "IP2002C".equalsIgnoreCase(model)
					|| "IP1001SA".equalsIgnoreCase(model)|| "IP1001SC".equalsIgnoreCase(model)
					|| "IP2001C".equalsIgnoreCase(model)
					) {
				monitorSetFlag = 1;
			} else if("IP1001S".equalsIgnoreCase(model)) {
				monitorSetFlag = 2;
			}
			System.out.println("model=" + model);
		}
		if(monitorSetFlag == 1){
			intelligenMonitorRadioGrop.setVisibility(View.GONE);
			selfIntelMonitor.setEnabled(false);
			selfSetMonitor.setEnabled(false);
			selfSetMonitorOneRate.setEnabled(false);
		} else if(monitorSetFlag == 2) {
			intelligenMonitorRadioGrop.setVisibility(View.GONE);
		}

		//监控模式
		if(paraMap.containsKey("mon_mode")) {
			String s = paraMap.get("mon_mode");
			if("inteligent".equalsIgnoreCase(s)){//inteligent normal
				if(monitorSetFlag == 1){
					selfSetMonitor.setChecked(false);
					selfIntelMonitor.setChecked(false);
				}else {
					selfSetMonitor.setChecked(false);
					selfIntelMonitor.setChecked(true);
				}
				intelligenMonitorRadioGrop.check(R.id.device_params_monitor_intelligent_mode_fluency_set_id);
			}else if("inteligent2".equalsIgnoreCase(s)){
				if(monitorSetFlag == 1){
					selfSetMonitor.setChecked(false);
					selfIntelMonitor.setChecked(false);
				}else {
					selfSetMonitor.setChecked(false);
					selfIntelMonitor.setChecked(true);
				}
				intelligenMonitorRadioGrop.check(R.id.device_params_monitor_intelligent_mode_distinct_set_id);
			} else {
				selfSetMonitor.setChecked(true);
				selfIntelMonitor.setChecked(false);
			}
		}
		
		intelligenMonitorRadioGrop.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(RadioGroup group, int checkedId) {
				if(checkedId == R.id.device_params_monitor_intelligent_mode_fluency_set_id) {
					paraMap.put("mon_mode", "inteligent");
				}else {
					paraMap.put("mon_mode", "inteligent2");
				}
			}
		});
		
		selfSetMonitor.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				selfSetMonitor.setChecked(true);
				selfIntelMonitor.setChecked(false);
				if(monitorSetFlag == 0) {
					findViewById(R.id.device_params_monitor_intelligent_mode_fluency_set_id).setEnabled(false);
					findViewById(R.id.device_params_monitor_intelligent_mode_distinct_set_id).setEnabled(false);
				}
			}
		});
		selfIntelMonitor.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				selfSetMonitor.setChecked(false);
				selfIntelMonitor.setChecked(true);	
				if(monitorSetFlag == 0) {
					findViewById(R.id.device_params_monitor_intelligent_mode_fluency_set_id).setEnabled(true);
					findViewById(R.id.device_params_monitor_intelligent_mode_distinct_set_id).setEnabled(true);
				
				}
			}
		});
		
		
		//监控模式的帧率
		if(paraMap.containsKey("rate")) {
			String s = paraMap.get("rate");
			if("2".equals(s)) {
				selfSetMonitorOneRate.setSelection(1);
			} else if("4".equals(s)){
				selfSetMonitorOneRate.setSelection(2);
			}else if("8".equals(s)){
				selfSetMonitorOneRate.setSelection(3);
			}else if("12".equals(s)){
				selfSetMonitorOneRate.setSelection(4);
			}else if("24".equals(s)){
				selfSetMonitorOneRate.setSelection(5);
			} else {
				selfSetMonitorOneRate.setSelection(0);
			}
		}
		
		// 监控模式的画面尺寸
		if(paraMap.containsKey("r_reso")) {
			String s = paraMap.get("r_reso");
			if("qvga".equalsIgnoreCase(s)){
				selfSetMonitorOneFrameSize.setSelection(1);
			}else {
				selfSetMonitorOneFrameSize.setSelection(0);
			}
		}
		
		if(paraMap.containsKey("snd_duplex")) {
			String s = paraMap.get("snd_duplex");
			if("1".equals(s)) {
				soundFlagSet.check(R.id.device_params_other_set_sound_open_flag_id);
			} else {
				soundFlagSet.check(R.id.device_params_other_set_sound_close_flag_id);
			}
		}
		
		if(paraMap.containsKey("email_alarm")) {
			String s = paraMap.get("email_alarm");
			if("1".equals(s)) {
				mobileAlarmSet.check(R.id.device_params_other_set_mobile_alarm_flag_open_id);
			} else {
				mobileAlarmSet.check(R.id.device_params_other_set_mobile_alarm_flag_close_id);
			}
		}
		
		if(paraMap.containsKey("r_sensitivity")) {
			String s = paraMap.get("r_sensitivity");
			if("2".equals(s)) {
				sensitvSet.setSelection(1);
			} else if("3".equals(s)) {
				sensitvSet.setSelection(2);
			} else  {
				sensitvSet.setSelection(0);
			}
		}
		
		apnEditText.setText(paraMap.containsKey("w_ssid")? paraMap.get("w_ssid") : "");
		apnPwdEditText.setText(paraMap.containsKey("w_key")? paraMap.get("w_key") : "");
		alarmEmailEditText.setText(paraMap.containsKey("mailbox")? paraMap.get("mailbox") : "");
		securityVisitPass.setText(paraMap.containsKey("pswd")? paraMap.get("pswd") : "");
		String systemTime = paraMap.get("system_time");
		dateSetButton.setText((systemTime != null && systemTime.length()>0)? DateUtil.formatTimeStrToTimeStr2(systemTime):DateUtil.formatTimeStrToTimeStr2(DateUtil.formatTimeToDate3(System.currentTimeMillis())));
		timeSetButton.setText((systemTime != null && systemTime.length()>0)? DateUtil.formatTimeStrToTimeStr3(systemTime):DateUtil.formatTimeStrToTimeStr3(DateUtil.formatTimeToDate3(System.currentTimeMillis())));
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		ProgressUtil.dismissProgress();
		//UdtTools.freeConnectionById(device.getDeviceID());
		UdtTools.freeCmdSocket();
	}

	private void lockWireuseCommont(boolean flag) {
		wireuseIPAddress.setEnabled(flag);
		wireuseGeteWayAddess.setEnabled(flag);
		wireuseSubAddess.setEnabled(flag);
		wireuseDNS1Address.setEnabled(flag);
		wireuseDNS2Address.setEnabled(flag);
	}
	
	private void lockWirelessCommont(boolean flag) {
		wirelessIPAddress.setEnabled(flag);
		wirelessGeteWayAddess.setEnabled(flag);
		wirelessSubAddess.setEnabled(flag);
		wirelessDNS1Address.setEnabled(flag);
		wirelessDNS2Address.setEnabled(flag);
	}
	
	private void collectionData() {
		if(paraMap.containsKey("name")){
			paraMap.put("name", deviceNameEditText.getText().toString().trim());
		}
		String tmp = null;
		switch(netWorkModeSet.getCheckedRadioButtonId()) {
		case R.id.device_param_set_network_mode_wireuse:
			tmp = "eth_only";
			break;
		case R.id.device_param_set_network_mode_wireless:
			tmp = "wlan_only";
			break;
			default:
				tmp = "inteligent";
				break;
		}
		if(paraMap.containsKey("inet_mode")){
			paraMap.put("inet_mode", tmp);
			
		}
		//有线网络设置
		int i = 0;
		switch(wireuseNetworkSet.getCheckedRadioButtonId()) {
		case R.id.device_param_set_wireuse_network_mode_auto:
			i = 1;
			break;
			default:
				i = 0;
				break;
		}
		if(paraMap.containsKey("udhcpc")){
			paraMap.put("udhcpc", i +"");
		}
		
		/*
		paraMap.put("e_ip", wireuseIPAddress.getText().toString().trim());
		paraMap.put("e_gw", wireuseGeteWayAddess.getText().toString().trim());
		paraMap.put("e_mask", wireuseSubAddess.getText().toString().trim());
		paraMap.put("e_dns1", wireuseDNS1Address.getText().toString().trim());
		paraMap.put("e_dns2", wireuseDNS2Address.getText().toString().trim());
		*/
		//无线网络设置 单选框与有线网络设置相同 此处不再设置
		/*
		paraMap.put("w_ip", wirelessIPAddress.getText().toString().trim());
		paraMap.put("w_gw", wirelessGeteWayAddess.getText().toString().trim());
		paraMap.put("w_mask", wirelessSubAddess.getText().toString().trim());
		paraMap.put("w_dns1", wirelessDNS1Address.getText().toString().trim());
		paraMap.put("w_dns2", wirelessDNS2Address.getText().toString().trim());
		*/
		// 录像设置
		if(recordSetOne.isChecked()) {
			tmp = "inteligent";
		}else {
			tmp = "normal";
		}
		if(paraMap.containsKey("r_mode")){
			paraMap.put("r_mode", tmp);
		}
		
		// 帧率
		switch(recordRateOne.getSelectedItemPosition()) {
		case 0:
			i = 1;
			break;
		case 1:
			i = 2;
			break;
		case 2:
			i = 4;
			break;
		case 3:
			i = 8;
			break;
		case 4:
			i = 12;
			break;
			default:
				i = 24;
				break;
		}
		
		if(paraMap.containsKey("r_nor_speed")){
			paraMap.put("r_nor_speed", i + "");
		}
		// 画面尺寸
		switch(recordFrameSizeOne.getSelectedItemPosition()) {
		case 0:
			tmp = "vga";
			break;
		case 1:
			tmp = "qvga";
			break;
			default:
				tmp = "720P";
				break;
		}
		if(paraMap.containsKey("r_reso")){
			paraMap.put("r_reso", tmp);
		}
		
		//监控模式
		if(selfSetMonitor.isChecked()) {
			if(paraMap.containsKey("mon_mode")){
				paraMap.put("mon_mode", "normal");
			}
		}else {
			if(paraMap.containsKey("mon_mode")){
				paraMap.put("mon_mode", "inteligent");
			}
		}
		
		//监控模式的帧率
		switch(selfSetMonitorOneRate.getSelectedItemPosition()) {
		case 0:
			i = 1;
			break;
		case 1:
			i = 2;
			break;
		case 2:
			i = 4;
			break;
		case 3:
			i = 8; 
			break;
		case 4:
			i = 12;
			break;
			default:
				i = 24;
				break;
		}
		
		if(paraMap.containsKey("rate")){
			paraMap.put("rate", i + "");
		}
		
		// 监控模式的画面尺寸 与录像模式相同 此处不在设置
		
		//其他设置
		/*
		switch(soundFlagSet.getCheckedRadioButtonId()) {
		case R.id.device_params_other_set_sound_open_flag_id:
			paraMap.put("snd_duplex", "1");
			break;
			default:
				paraMap.put("snd_duplex", "0");
				break;
		}

		switch(mobileAlarmSet.getCheckedRadioButtonId()) {
		case R.id.device_params_other_set_mobile_alarm_flag_open_id:
			paraMap.put("email_alarm", "1");
			break;
			default:
				paraMap.put("email_alarm", "0");
				break;
		}
		*/
		// 灵敏度
		/*
		switch(recordFrameSizeOne.getSelectedItemPosition()) {
		case 0:
			paraMap.put("r_sensitivity", "1");
			break;
		case 1:
			paraMap.put("r_sensitivity", "2");
			break;
			default:
				paraMap.put("r_sensitivity", "3");
				break;
		}
		paraMap.put("mailbox", alarmEmailEditText.getText().toString().trim());
		*/
		//paraMap.put("password", securityVisitPass.getText().toString().trim());
		/*
		if(wifiList != null) {
			WifiConfig config = wifiList.get(wirelessSpinner.getSelectedItemPosition());
			paraMap.put("w_ssid", config.getSsid());
			paraMap.put("inet_wlan_proto", config.getProto());
			System.out.println("=======>" + config.getKey_mgmt());
			if("WPA-PSK".equalsIgnoreCase(config.getKey_mgmt())) {
				paraMap.put("w_key", config.getKey_mgmt());
				paraMap.put("inet_wlan_psk", apnPwdEditText.getText().toString().trim());
			} else {
				paraMap.put("inet_wlan_key_mgmt", config.getKey_mgmt());
				paraMap.put("inet_wlan_wep_key0", apnPwdEditText.getText().toString().trim());
			}
			paraMap.put("inet_wlan_pairwise", config.getPairwise());
			paraMap.put("inet_wlan_group",config.getGroup());
		}
		*/
		/*if(paraMap.containsKey("system_time")){
			paraMap.remove("system_time");
		}
		if(paraMap.containsKey("system_time")){
			paraMap.remove("system_time");
		}
		if(paraMap.containsKey("system_time")){
			paraMap.remove("system_time");
		}
		if(paraMap.containsKey("system_time")){
			paraMap.remove("system_time");
		}*/
		//paraMap.put("system_time",dateSetButton.getText().toString().replaceAll("-", "") + timeSetButton.getText().toString().replaceAll(":", ""));
		//device.setDeviceName(deviceNameEditText.getText().toString().trim());
		//device.setDeviceEthIp(wireuseIPAddress.getText().toString().trim());
		//device.setDeviceEthGateWay(wireuseGeteWayAddess.getText().toString().trim());
		//device.setDeviceEthMask(wireuseSubAddess.getText().toString().trim());
		//device.setDeviceEthDNS1(wireuseDNS1Address.getText().toString().trim());
		//device.setDeviceEthDNS2(wireuseDNS2Address.getText().toString().trim());
		//device.setDeviceWlanIp(wirelessIPAddress.getText().toString().trim());
		//device.setDeviceWlanGateWay(wirelessGeteWayAddess.getText().toString().trim());
		//device.setDeviceWlanMask(wirelessSubAddess.getText().toString().trim());
		//device.setDeviceWlanDNS1(wirelessDNS1Address.getText().toString().trim());
		//device.setDeviceEthDNS2(wirelessDNS2Address.getText().toString().trim());
		//camManager.updateCam(device);
		
		handler.sendEmptyMessage(Constants.SENDCONFIGMSG);
	}
	
	public void send(final Map<String, String> paraMap) {
		new Thread(new Runnable(){
			@Override
			public void run() {
				device.setDeviceName(deviceNameEditText.getText().toString().trim());
				camManager.updateCam(device);
				FileUtil.persistentDevice(DeviceParamSets.this,camManager.getCamList());
				String ws = ParaUtil.enCapsuPara(paraMap);
				int l = ws.length();
				StringBuffer sb = new StringBuffer(CamCmdListHelper.SetCmd_Config);
				int res = UdtTools.sendCmdMsgById(device.getDeviceID(), sb.toString(), sb.length());
				if(res>0) {
					sb.delete(0, sb.length());
					if(l>1000) {
						sb.append(l);
					} else {
						sb.append("0" + l);
					}
					sb.append(ws);
					res = UdtTools.sendCmdMsgById(device.getDeviceID(), sb.toString(), sb.length());
					if(res > 0) {
						Message msg = handler.obtainMessage();
						msg.what = Constants.SENDSETCONFIGSUCCESSMSG;
						handler.sendMessageDelayed(msg, 500);
					} else {
						handler.sendEmptyMessage(Constants.SENDSETCONFIGERRORMSG);
					}
				}else {
					handler.sendEmptyMessage(Constants.SENDSETCONFIGERRORMSG);
				}
			}
		}).start();
		
	}
	
	public void resetFactory() {
		new Thread(new Runnable(){
			@Override
			public void run() {
				String command = CamCmdListHelper.ReSetCmd_Config;
				int res = UdtTools.sendCmdMsgById(device.getDeviceID(), command, command.length());
				if(res >0) {
					handler.sendEmptyMessage(Constants.RETSETCONFIGSUCCESS);
				} else {
					handler.sendEmptyMessage(Constants.RETSETCONFIGERROR);
				}
			}
		}).start();
		
	}
	
	private void searchWireless() {
		new Thread(new Runnable(){
			@Override
			public void run() {
				String command = CamCmdListHelper.SetCmd_SearchWireless;
				byte[] recv = new byte[Constants.COMMNICATEBUFFERSIZE];
				String id = device.getDeviceID();
				int res = UdtTools.sendCmdMsgById(id, command, command.length());
				Log.d(TAG, "search wireless send data length = " + res);
				if( res >0) {
					int re = UdtTools.recvCmdMsgById(id, recv, Constants.COMMNICATEBUFFERSIZE);
					Log.d(TAG, "search wireless  revc data length = " + re);
					if(re > 0) {
						String length = new String(recv, 0, 4).trim();
						int l = re - 4;
						if(length.equals(Integer.toString(l)) || !length.equals(Integer.toString(l))) {
							String wifiInfo = new String(recv,4, l);
							Log.d(TAG, "s = "  + length.trim()  + " " + wifiInfo.trim());
							wifiList = ParaUtil.encapsuWifiConfig(wifiInfo);
							handler.sendEmptyMessage(Constants.SENDSEARCHWIRELESSSUCCESSMSG);
						}else{
							String cont = new String(recv,4, l);
							Log.d(TAG, "recv length not legal!  header size = " + length + "  data content size = "  + cont.length() + " content = " + cont);
							handler.sendEmptyMessage(Constants.SENDSEARCHWIRELESSERRORMSG);
						}
					}else {
						handler.sendEmptyMessage(Constants.SENDSEARCHWIRELESSERRORMSG);
					}
				} else {
					handler.sendEmptyMessage(Constants.SENDSEARCHWIRELESSERRORMSG);
				}
				handler.sendEmptyMessage(Constants.SENDSETCONFIGERRORMSG);
			}
		}).start();
	}

	private void initWirelessList() {
		if(wifiList.size()<=0) {
			wirelessSpinner.setClickable(false);
		} else {
			wirelessSpinner.setAdapter(new WirelessAdapter(wifiList, DeviceParamSets.this));
			wirelessSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
				@Override
				public void onItemSelected(AdapterView<?> parent, View view,
						int position, long id) {
					WifiConfig config = wifiList.get(position);
					apnEditText.setText(config.getSsid());
				}

				@Override
				public void onNothingSelected(AdapterView<?> parent) {
					
				}
			});
		}
	}
	
	private String format(int x) {
		String s = "" + x;
		if(s.length() == 1) {
			s = "0" + s;
		}
		return s;
	}
	
	private String getCurrentVersion() {
		try {
			PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
			return packageInfo.versionName;
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}
		return "";
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		StatService.onResume(this);
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		StatService.onPause(this);
	}
}
