package com.iped.ipcam.gui;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.LinkedHashMap;
import java.util.Map;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;

import com.iped.ipcam.engine.CamMagFactory;
import com.iped.ipcam.engine.CamParasSetImp;
import com.iped.ipcam.engine.ICamManager;
import com.iped.ipcam.exception.CamManagerException;
import com.iped.ipcam.pojo.Device;
import com.iped.ipcam.utils.CamCmdListHelper;
import com.iped.ipcam.utils.Constants;
import com.iped.ipcam.utils.PackageUtil;
import com.iped.ipcam.utils.ParaUtil;
import com.iped.ipcam.utils.ProgressUtil;
import com.iped.ipcam.utils.ThroughNetUtil;
import com.iped.ipcam.utils.ToastUtils;

public class DeviceParamSets extends Activity implements OnClickListener {

	private ICamManager camManager = CamMagFactory.getCamManagerInstance();

	private CamParasSetImp camParasSet = CamMagFactory.getCamParasSetInstance();

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

	private EditText addrTypeEditText = null;

	private EditText addressEditText = null;

	private Spinner frameRateSpinner = null;

	private Spinner frameSizeSpinner = null;

	private Spinner compressQuarSpinner = null;

	private Spinner compressTypeSpinner = null;

	private RadioGroup soundFlagSet = null;
	
	private RadioGroup mobileAlarmSet = null;
	
	private Spinner sensitvSet = null;
	
	private RadioButton recordSetOne = null;
	
	private Spinner recordRateOne = null;
	
	private Spinner recordFrameSizeOne = null;
	
	private RadioButton selfSetMonitor = null;
	
	private Spinner selfSetMonitorOneRate = null;
	
	private Spinner selfSetMonitorOneFrameSize = null;
	
	private EditText alarmEmailEditText = null;
	
	private EditText securityVisitPass = null;
	
	private Map<String, String> paraMap = null; 
	
	private DatagramSocket tmpDatagramSocket = null;
	
	private String ip;
	
	private int port1;
	
	private String TAG = "DeviceParamSets";
	
	private Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case Constants.SHOWQUERYCONFIGDLG:
				ProgressUtil.showProgress(R.string.device_params_request_config_str, DeviceParamSets.this);
				camParasSet.getCamPara(device, this);
				break;
			case Constants.HIDEQUERYCONFIGDLG:
				paraMap = camParasSet.getParaMap();
				initializeEditText(paraMap);
				/*
				 * Set<String> s = paraMap.keySet(); for(String ss:s){
				 * System.out.println(ss + " " + paraMap.get(ss)); }
				 */
				ProgressUtil.hideProgress();
				/*
				 * Bundle data = msg.getData(); CamConfig camConfig =
				 * data.getParcelable("CAMPARAMCONFIG");
				 * System.out.println(camConfig); if(camConfig != null) {
				 * //initializeEditText(camConfig); }
				 */
				break;
			case Constants.QUERYCONFIGERROR:
				ProgressUtil.hideProgress();
				Toast.makeText(
						DeviceParamSets.this,
						getResources().getString(
								R.string.device_params_request_error_str),
						Toast.LENGTH_SHORT).show();
				DeviceParamSets.this.finish();
				break;
			case Constants.SENDGETTHREEPORTMSG:
				ThroughNetUtil netUtil = camParasSet.getThroughNetUtil();
				tmpDatagramSocket = netUtil.getPort1();
				handler.sendEmptyMessage(Constants.SENDDATAWHENMODIFYCONFIG);
				if(netUtil != null) {
					Bundle bd = msg.getData();
					if(bd != null) {
						ip = bd.getString("IPADDRESS");
						port1 = bd.getInt("PORT1");
						//int port2 = bd.getInt("PORT2");
						//int port3 = bd.getInt("PORT3");
						String cmd = null;
						try {
							cmd = PackageUtil.CMDPackage2(netUtil, CamCmdListHelper.GetCmd_Config, ip, port1);
							paraMap = new LinkedHashMap<String,String>();
							ParaUtil.putParaByString(cmd, paraMap);
							initializeEditText(paraMap);
						} catch (CamManagerException e) {
							handler.sendEmptyMessage(Constants.QUERYCONFIGERROR);
							Log.d(TAG, e.getLocalizedMessage());
							return;
						}
					}
				}
				ProgressUtil.hideProgress();
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
				sendNullData();
				break;
			case Constants.SENDSETCONFIGSUCCESSMSG:
				DeviceParamSets.this.finish();
				break;
			case Constants.SENDSETCONFIGERRORMSG:
				ProgressUtil.hideProgress();
				break;
			case Constants.RETSETCONFIGSUCCESS:
				ToastUtils.showToast(DeviceParamSets.this, R.string.device_params_reset_config_success_str);
				handler.sendEmptyMessage(Constants.SENDSETCONFIGSUCCESSMSG);
				break;
			case Constants.RETSETCONFIGERROR:
				ToastUtils.showToast(DeviceParamSets.this, R.string.device_params_reset_config_error_str);
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
		System.out.println(device);
		if (device == null) {
			Toast.makeText(
					this,
					getResources().getString(
							R.string.device_params_no_device_select_str),
					Toast.LENGTH_SHORT).show();
		} else {
			// vodeoSearchDia(device.getDeviceIp());
			handler.sendEmptyMessage(Constants.SHOWQUERYCONFIGDLG);
		}
	}

	private void lookupEditText() {
		
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
		//frameRateSpinner = (Spinner) findViewById(R.id.device_params_set_recorde_frame_rate_one_id);
		recordSetOne = (RadioButton) findViewById(R.id.device_params_video_record_set_one_id);
		recordRateOne = (Spinner) findViewById(R.id.device_params_set_recorde_frame_rate_one_id);
		recordFrameSizeOne = (Spinner) findViewById(R.id.device_params_set_recorde_frame_size_id);
		
		selfSetMonitor = (RadioButton) findViewById(R.id.device_params_monitor_self_mode_set_id);
		selfSetMonitorOneRate = (Spinner) findViewById(R.id.device_params_monitor_one_frame_rate_id);
		selfSetMonitorOneFrameSize = (Spinner) findViewById(R.id.device_params_monitor_one_frame_size_id);
		
		soundFlagSet = (RadioGroup) findViewById(R.id.device_params_other_set_sound_open_id);
		mobileAlarmSet = (RadioGroup) findViewById(R.id.device_params_other_set_mobile_alarm_flag_id);
		sensitvSet = (Spinner) findViewById(R.id.device_params_other_set_mobile_alarm_offset_prompt_id);
		alarmEmailEditText = (EditText) findViewById(R.id.device_params_other_set_mobile_alarm_email_id);
		securityVisitPass = (EditText) findViewById(R.id.device_params_other_set_security_id);
		
		findViewById(R.id.device_params_set_factory_button_id).setOnClickListener(this);
		findViewById(R.id.device_params_set_commit_button_id).setOnClickListener(this);
		findViewById(R.id.device_params_set_concle_button_id).setOnClickListener(this);

	}

	@Override
	protected void onResume() {
		super.onResume();
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.device_params_set_factory_button_id:
			handler.sendEmptyMessage(Constants.SETCONFIGDLG);
			handler.removeMessages(Constants.SENDDATAWHENMODIFYCONFIG);
			resetFactory();
			break;
		case R.id.device_params_set_commit_button_id:
			handler.sendEmptyMessage(Constants.SETCONFIGDLG);
			handler.removeMessages(Constants.SENDDATAWHENMODIFYCONFIG);
			collectionData();
			send(paraMap);					
			break;
		case R.id.device_params_set_concle_button_id:
			this.finish();
			break;
		default:
			break;
		}
		
	}
	
	private void initializeEditText(Map<String, String> paraMap) {
		deviceNameEditText.setText(paraMap.containsKey("name")? paraMap.get("name") : "");
		deviceIdEditText.setText(paraMap.containsKey("cam_id")? paraMap.get("cam_id") : "");
		versionEditText.setText(paraMap.containsKey("")? paraMap.get("") : "V12.005.13");
		tfCardEditText.setText(paraMap.containsKey("tfcard_maxsize")? paraMap.get("tfcard_maxsize") : "");
		sdCardEditText.setText(paraMap.containsKey("")? paraMap.get("") : "");
		//changeStorageMode.setText("");
		//valueableRecordeTimeEd.setText("");
		if(paraMap.containsKey("inet_mode")){
			String s = paraMap.get("inet_mode");
			if("eth_only".equals(s)) {
				netWorkModeSet.check(R.id.device_param_set_network_mode_wireuse);
			}else if("wlan_only".equals(s)) {
				netWorkModeSet.check(R.id.device_param_set_network_mode_wireless);
			} else {
				netWorkModeSet.check(R.id.device_param_set_network_mode_intelligent);
			}
		}
		if(paraMap.containsKey("inet_udhcpc")) {
			String s = paraMap.get("inet_udhcpc");
			if("1".equals(s)) {
				wireuseNetworkSet.check(R.id.device_param_set_wireuse_network_mode_auto);
			} else {
				wireuseNetworkSet.check(R.id.device_param_set_wireuse_network_mode_static);
			}
		}
		wireuseIPAddress.setText(paraMap.containsKey("inet_eth_ip")? paraMap.get("inet_eth_ip") : "");
		wireuseGeteWayAddess.setText(paraMap.containsKey("inet_eth_gateway")? paraMap.get("inet_eth_gateway") : "");
		wireuseSubAddess.setText(paraMap.containsKey("inet_eth_mask")? paraMap.get("inet_eth_mask") : "");
		wireuseDNS1Address.setText(paraMap.containsKey("inet_eth_dns1")? paraMap.get("inet_eth_dns1") : "");
		wireuseDNS2Address.setText(paraMap.containsKey("inet_eth_dns2")? paraMap.get("inet_eth_dns2") : "");
		if(paraMap.containsKey("inet_udhcpc")) {
			String s = paraMap.get("inet_udhcpc");
			if("1".equals(s)) {
				wirelessNetworkSet.check(R.id.device_param_set_wireless_network_mode_audo);
			} else {
				wirelessNetworkSet.check(R.id.device_param_set_wireless_network_static);
			}
		}
		wirelessIPAddress.setText(paraMap.containsKey("inet_wlan_ip")? paraMap.get("inet_wlan_ip") : "");
		wirelessGeteWayAddess.setText(paraMap.containsKey("inet_wlan_gateway")? paraMap.get("inet_wlan_gateway") : "");
		wirelessSubAddess.setText(paraMap.containsKey("inet_wlan_mask")? paraMap.get("inet_wlan_mask") : "");
		wirelessDNS1Address.setText(paraMap.containsKey("inet_wlan_dns1")? paraMap.get("inet_wlan_dns1") : "");
		wirelessDNS2Address.setText(paraMap.containsKey("inet_wlan_dns2")? paraMap.get("inet_wlan_dns2") : "");
		
		if(paraMap.containsKey("record_mode")) {
			String s = paraMap.get("record_mode");
			if("inteligent".equals(s)) {
				recordSetOne.setChecked(true);
			} else {
				recordSetOne.setChecked(false);
			}
		}
		// 帧率
		if(paraMap.containsKey("record_normal_speed")) {
			String s = paraMap.get("record_normal_speed");
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
		if(paraMap.containsKey("resolution")) {
			String s = paraMap.get("resolution");
			if("720P".equalsIgnoreCase(s)) {
				recordFrameSizeOne.setSelection(0);
			} else if("qvga".equalsIgnoreCase(s)){
				recordFrameSizeOne.setSelection(2);
			}else {
				recordFrameSizeOne.setSelection(1);
			}
		}
		
		//监控模式
		if(paraMap.containsKey("monitor_mode")) {
			String s = paraMap.get("monitor_mode");
			if("normal".equals(s)) {
				selfSetMonitor.setChecked(true);
			} else {
				selfSetMonitor.setChecked(false);
			}
			
		}
		
		//监控模式的帧率
		if(paraMap.containsKey("framerate")) {
			String s = paraMap.get("framerate");
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
		if(paraMap.containsKey("resolution")) {
			String s = paraMap.get("resolution");
			if("720P".equalsIgnoreCase(s)) {
				selfSetMonitorOneFrameSize.setSelection(0);
			} else if("qvga".equalsIgnoreCase(s)){
				selfSetMonitorOneFrameSize.setSelection(2);
			}else {
				selfSetMonitorOneFrameSize.setSelection(1);
			}
		}
		
		if(paraMap.containsKey("sound_duplex")) {
			String s = paraMap.get("sound_duplex");
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
		
		if(paraMap.containsKey("record_sensitivity")) {
			String s = paraMap.get("record_sensitivity");
			if("2".equals(s)) {
				sensitvSet.setSelection(1);
			} else if("3".equals(s)) {
				sensitvSet.setSelection(2);
			} else  {
				sensitvSet.setSelection(0);
			}
		}
	
		alarmEmailEditText.setText(paraMap.containsKey("mailbox")? paraMap.get("mailbox") : "");
		securityVisitPass.setText(paraMap.containsKey("password")? paraMap.get("password") : "");
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		ProgressUtil.dismissProgress();
	}

	private void collectionData() {
		paraMap.put("name", deviceNameEditText.getText().toString().trim());
		switch(netWorkModeSet.getCheckedRadioButtonId()) {
		case R.id.device_param_set_network_mode_wireuse:
			paraMap.put("inet_mode", "eth_only");
			break;
		case R.id.device_param_set_network_mode_wireless:
			paraMap.put("inet_mode", "wlan_only");
			break;
			default:
				paraMap.put("inet_mode", "inteligent");
				break;
		}
		//有线网络设置
		switch(wireuseNetworkSet.getCheckedRadioButtonId()) {
		case R.id.device_param_set_wireuse_network_mode_auto:
			paraMap.put("inet_udhcpc", 1 + "");
			break;
			default:
				paraMap.put("inet_udhcpc", 0 + "");
				break;
		}
		
		paraMap.put("inet_eth_ip", wireuseIPAddress.getText().toString().trim());
		paraMap.put("inet_eth_gateway", wireuseGeteWayAddess.getText().toString().trim());
		paraMap.put("inet_eth_mask", wireuseSubAddess.getText().toString().trim());
		paraMap.put("inet_eth_dns1", wireuseDNS1Address.getText().toString().trim());
		paraMap.put("inet_eth_dns2", wireuseDNS2Address.getText().toString().trim());
		
		//无线网络设置 单选框与有线网络设置相同 此处不再设置
		paraMap.put("inet_wlan_ip", wirelessIPAddress.getText().toString().trim());
		paraMap.put("inet_wlan_gateway", wirelessGeteWayAddess.getText().toString().trim());
		paraMap.put("inet_wlan_mask", wirelessSubAddess.getText().toString().trim());
		paraMap.put("inet_wlan_dns1", wirelessDNS1Address.getText().toString().trim());
		paraMap.put("inet_wlan_dns2", wirelessDNS2Address.getText().toString().trim());
		
		// 录像设置
		if(recordSetOne.isChecked()) {
			paraMap.put("record_mode", "inteligent");
		}else {
			paraMap.put("record_mode", "normal");
		}
		
		// 帧率
		switch(recordRateOne.getSelectedItemPosition()) {
		case 0:
			paraMap.put("record_normal_speed", "1");
			break;
		case 1:
			paraMap.put("record_normal_speed", "2");
			break;
		case 2:
			paraMap.put("record_normal_speed", "4");
			break;
		case 3:
			paraMap.put("record_normal_speed", "8");
			break;
		case 4:
			paraMap.put("record_normal_speed", "12");
			break;
			default:
				paraMap.put("record_normal_speed", "24");
				break;
		}
		
		// 画面尺寸
		switch(recordFrameSizeOne.getSelectedItemPosition()) {
		case 0:
			paraMap.put("record_resolution", "720P");
			break;
		case 1:
			paraMap.put("record_resolution", "vga");
			break;
			default:
				paraMap.put("record_resolution", "qvga");
				break;
		}
		
		//监控模式
		if(selfSetMonitor.isChecked()) {
			paraMap.put("monitor_mode", "1");
		}else {
			paraMap.put("monitor_mode", "0");
		}
		
		//监控模式的帧率
		switch(selfSetMonitorOneRate.getSelectedItemPosition()) {
		case 0:
			paraMap.put("framerate", "1");
			break;
		case 1:
			paraMap.put("framerate", "2");
			break;
		case 2:
			paraMap.put("framerate", "4");
			break;
		case 3:
			paraMap.put("framerate", "8");
			break;
		case 4:
			paraMap.put("framerate", "12");
			break;
			default:
				paraMap.put("framerate", "24");
				break;
		}
		
		// 监控模式的画面尺寸 与录像模式相同 此处不在设置
		
		//其他设置
		switch(soundFlagSet.getCheckedRadioButtonId()) {
		case R.id.device_params_other_set_sound_open_flag_id:
			paraMap.put("sound_duplex", "1");
			break;
			default:
				paraMap.put("sound_duplex", "0");
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

		// 灵敏度
		switch(recordFrameSizeOne.getSelectedItemPosition()) {
		case 0:
			paraMap.put("record_sensitivity", "1");
			break;
		case 1:
			paraMap.put("record_sensitivity", "2");
			break;
			default:
				paraMap.put("record_sensitivity", "3");
				break;
		}
		paraMap.put("mailbox", alarmEmailEditText.getText().toString().trim());
		paraMap.put("password", securityVisitPass.getText().toString().trim());
	}
	
	public void send(Map<String, String> paraMap) {
		if(device.getDeviceNetType()) { // 利用临时保存的连接发送配置信息
			String ws = ParaUtil.enCapsuPara(paraMap);
			int l = ws.length();
			if(l>1000) {
				StringBuffer sb = new StringBuffer(CamCmdListHelper.SetCmd_Config);
				byte [] data = sb.toString().getBytes();
				DatagramPacket datagramPacket;
				try {
					datagramPacket = new DatagramPacket(data, data.length, InetAddress.getByName(ip), port1);
					tmpDatagramSocket.send(datagramPacket);
					sb = new StringBuffer(new String("0996") + ws.substring(0,996));
					data = sb.toString().getBytes();
					datagramPacket = new DatagramPacket(data, data.length, InetAddress.getByName(ip), port1);
					tmpDatagramSocket.send(datagramPacket);
					String a = ws.substring(996);
					int left = a.length();// l - 1000;
					sb = new StringBuffer(new String("0" + left) + a);
					data = sb.toString().getBytes();
					datagramPacket = new DatagramPacket(data, data.length, InetAddress.getByName(ip), port1);
					ToastUtils.showToast(DeviceParamSets.this, R.string.device_params_set_config_success_str);
				} catch (Exception e) {
					e.printStackTrace();
					ToastUtils.showToast(DeviceParamSets.this, R.string.device_params_set_config_error_str);
					handler.sendEmptyMessage(Constants.SENDSETCONFIGERRORMSG);
				} finally {
					handler.sendEmptyMessage(Constants.SENDSETCONFIGSUCCESSMSG);
				}
			}
		} else {
			String ethIp = device.getDeviceEthIp();
			if(ethIp != null) {
				String rece;
				try {
					rece = PackageUtil.sendPackageByIp(CamCmdListHelper.GetCmd_Config, ethIp, Constants.UDPPORT);
					System.out.println("ethIp = " + ethIp + "  recv===="+ rece);
					handler.sendEmptyMessage(Constants.HIDEQUERYCONFIGDLG);
				} catch (CamManagerException e) {
					getConfigByWlan(device.getDeviceWlanIp());
				}
			} else {
				getConfigByWlan(device.getDeviceWlanIp());
			}
		}	
	}
	
	private void getConfigByWlan(String wlan) {
		String rece;
		try {
			rece = PackageUtil.sendPackageByIp(CamCmdListHelper.GetCmd_Config, wlan, Constants.UDPPORT);
			System.out.println("wlan = " + wlan + "  recv===="+ rece);
		} catch (CamManagerException e) {
			handler.sendEmptyMessage(Constants.QUERYCONFIGERROR);
		} finally {
			handler.sendEmptyMessage(Constants.HIDEQUERYCONFIGDLG);
		}
	}
	
	public void sendNullData() {
		int l = 2;
		byte[] b = new byte[l];
		try {
			DatagramPacket datagramPacket = new DatagramPacket(b, l, InetAddress.getByName(ip), port1);
			tmpDatagramSocket.send(datagramPacket);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			Message msg = handler.obtainMessage();
			msg.what = Constants.SENDDATAWHENMODIFYCONFIG;
			handler.sendMessageDelayed(msg, 10000);
		}
	}
	
	public void resetFactory() {
		byte[] b = CamCmdListHelper.ReSetCmd_Config.getBytes();
		try {
			DatagramPacket datagramPacket = new DatagramPacket(b, b.length, InetAddress.getByName(ip), port1);
			tmpDatagramSocket.send(datagramPacket);
			handler.sendEmptyMessage(Constants.RETSETCONFIGSUCCESS);
		} catch (Exception e) {
			e.printStackTrace();
			handler.sendEmptyMessage(Constants.RETSETCONFIGERROR);
		} 
	}
}
