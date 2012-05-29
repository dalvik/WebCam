package com.iped.ipcam.gui;

import java.util.Map;
import java.util.Set;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;

import com.iped.ipcam.engine.CamMagFactory;
import com.iped.ipcam.engine.CamParasSetImp;
import com.iped.ipcam.engine.ICamManager;
import com.iped.ipcam.pojo.Device;
import com.iped.ipcam.utils.CamCmdListHelper;
import com.iped.ipcam.utils.Constants;
import com.iped.ipcam.utils.PackageUtil;
import com.iped.ipcam.utils.ParaUtil;
import com.iped.ipcam.utils.ProgressUtil;

public class DeviceParamSets extends Activity {

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

	private EditText frameRateEditText = null;

	private Spinner frameSizeSpinner = null;

	private Spinner frameRateSpinner = null;

	private Spinner compressQuarSpinner = null;

	private Spinner compressTypeSpinner = null;

	private Spinner monitorFrameSizeSpinner = null;

	private RadioGroup soundFlagSet = null;
	
	private RadioGroup mobileAlarmSet = null;
	
	private Spinner sensitvSet = null;
	
	private RadioButton recordSetOne = null;
	
	private Spinner recordRateOne = null;
	
	private RadioButton selfSetMonitor = null;
	
	private EditText alarmEmailEditText = null;
	
	private EditText securityVisitPass = null;
	
	private Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case Constants.SHOWQUERYCONFIGDLG:
				camParasSet.getCamPara(device.getDeviceIp(), this);
				// ProgressUtil.showProgress(R.string.device_params_request_config_str,
				// DeviceParamSets.this);
				break;
			case Constants.HIDEQUERYCONFIGDLG:
				Map<String, String> paraMap = camParasSet.getParaMap();
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
		wireuseSubAddess = (EditText) findViewById(R.id.device_param_set_wireuse_dns1_address);
		wireuseDNS1Address = (EditText) findViewById(R.id.device_param_set_wireuse_dns1_address);
		wireuseDNS2Address = (EditText) findViewById(R.id.device_param_set_wireuse_dns2_address);
		wirelessNetworkSet = (RadioGroup) findViewById(R.id.device_param_set_wireless_network_mode);
		wirelessIPAddress = (EditText) findViewById(R.id.device_param_set_wireless_ip_address);
		wirelessGeteWayAddess = (EditText) findViewById(R.id.device_param_set_wireless_gateway_address);
		wirelessSubAddess = (EditText) findViewById(R.id.device_param_set_wireless_subway_address);
		wirelessDNS1Address = (EditText) findViewById(R.id.device_param_set_wireless_dns1_address);
		wirelessDNS2Address = (EditText) findViewById(R.id.device_param_set_wireless_dns2_address);

		recordSetOne = (RadioButton) findViewById(R.id.device_params_video_record_set_one_id);
		recordRateOne = (Spinner) findViewById(R.id.device_params_set_recorde_frame_rate_one_id);
		selfSetMonitor = (RadioButton) findViewById(R.id.device_params_monitor_self_mode_set_id);
		soundFlagSet = (RadioGroup) findViewById(R.id.device_params_other_set_sound_open_id);
		mobileAlarmSet = (RadioGroup) findViewById(R.id.device_params_other_set_mobile_alarm_flag_id);
		sensitvSet = (Spinner) findViewById(R.id.device_params_other_set_mobile_alarm_offset_prompt_id);
		alarmEmailEditText = (EditText) findViewById(R.id.device_params_other_set_mobile_alarm_email_id);
		securityVisitPass = (EditText) findViewById(R.id.device_params_other_set_security_id);
		
		// addrTypeEditText = (EditText)
		// findViewById(R.id.device_param_addr_type);
		// addressEditText = (EditText) findViewById(R.id.device_param_address);
		// frameRateEditText = (EditText)
		// findViewById(R.id.device_params_recorde_real_speed_id);
		// frameSizeSpinner = (Spinner)
		// findViewById(R.id.device_params_recorde_frame_size_id);
		/*
		 * ArrayAdapter adpter = ArrayAdapter.createFromResource(this,
		 * R.array.frame_size_array, android.R.layout.simple_spinner_item);
		 * frameSizeSpinner.setAdapter(adpter); frameRateSpinner = (Spinner)
		 * findViewById(R.id.device_params_recorde_frame_speed_id); adpter =
		 * ArrayAdapter.createFromResource(this, R.array.frame_rate_array,
		 * android.R.layout.simple_spinner_item);
		 * frameRateSpinner.setAdapter(adpter); //compressQuarSpinner =
		 * (Spinner) findViewById(R.id.device_params_recorde_comp_quar_id);
		 * adpter = ArrayAdapter.createFromResource(this,
		 * R.array.compress_quar_array, android.R.layout.simple_spinner_item);
		 * compressQuarSpinner.setAdapter(adpter); //compressTypeSpinner =
		 * (Spinner) findViewById(R.id.device_params_recorde_comp_type_id);
		 * adpter = ArrayAdapter.createFromResource(this,
		 * R.array.compress_type_array, android.R.layout.simple_spinner_item);
		 * compressTypeSpinner.setAdapter(adpter); //monitorFrameSizeSpinner =
		 * (Spinner) findViewById(R.id.device_params_monitor_frame_size_id);
		 * adpter = ArrayAdapter.createFromResource(this,
		 * R.array.monitor_frame_size_array,
		 * android.R.layout.simple_spinner_item);
		 * monitorFrameSizeSpinner.setAdapter(adpter);
		 */
	}

	@Override
	protected void onResume() {
		super.onResume();
		//
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
		}/**/
	}

	private void initializeEditText(Map<String, String> paraMap) {
		send(paraMap);
		deviceNameEditText.setText(paraMap.containsKey("name")? paraMap.get("name") : "");
		deviceIdEditText.setText(paraMap.containsKey("cam_id")? paraMap.get("cam_id") : "");
		versionEditText.setText(paraMap.containsKey("")? paraMap.get("") : "");
		tfCardEditText.setText(paraMap.containsKey("")? paraMap.get("") : "");
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
		if(paraMap.containsKey("inet_wlan_mode")) {
			String s = paraMap.get("inet_wlan_mode");
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
		
		
		if(paraMap.containsKey("monitor_mode")) {
			String s = paraMap.get("monitor_mode");
			if("normal".equals(s)) {
				selfSetMonitor.setChecked(true);
			} else {
				selfSetMonitor.setChecked(false);
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

	public void send(Map<String, String> paraMap) {
		PackageUtil.sendPackageNoRecv(CamCmdListHelper.SetCmd_Config, "", 0);
		String ws = ParaUtil.enCapsuPara(paraMap);
		System.out.println("----" + ws);
		int l = ws.length();
		if(l>1000) {
			PackageUtil.sendPackageNoRecv(new String("0996") + ws.substring(0,996), "", 0);
			System.out.println("pagcage one");
		}
		String a = ws.substring(996);
		int left = a.length();// l - 1000;
		PackageUtil.sendPackageNoRecv(new String("0" + left) + a, "", 0);
		System.out.println("pagcage two");
	}
}
