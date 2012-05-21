package com.iped.ipcam.gui;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.iped.ipcam.engine.CamMagFactory;
import com.iped.ipcam.engine.ICamManager;
import com.iped.ipcam.engine.ICamParasSet;
import com.iped.ipcam.pojo.CamConfig;
import com.iped.ipcam.pojo.Device;
import com.iped.ipcam.utils.Constants;
import com.iped.ipcam.utils.ProgressUtil;

public class DeviceParamSets extends Activity {

	private ICamManager camManager = CamMagFactory.getCamManagerInstance();
	
	private ICamParasSet camParasSet = CamMagFactory.getCamParasSetInstance();
	
	private Device device = null;
	
	private EditText versionEditText = null;
	
	private EditText deviceNameEditText = null;
	
	private EditText addrTypeEditText = null;
	
	private EditText addressEditText = null;
	
	private EditText inTotalSpaceEditText = null;
	
	private EditText outTotalSpaceEditText = null;
	
	private EditText enableRecordeTimeEditText = null;
	
	private EditText frameRateEditText = null;
	
	private Spinner frameSizeSpinner = null;
	
	private Spinner frameRateSpinner = null;
	
	private Spinner compressQuarSpinner = null;
	
	private Spinner compressTypeSpinner = null;
	
	private Spinner monitorFrameSizeSpinner = null;
	
	
	private Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case Constants.SHOWQUERYCONFIGDLG:
				camParasSet.getCamPara(device.getDeviceIp(), this);
				ProgressUtil.showProgress(R.string.device_params_request_config_str, DeviceParamSets.this);
				break;
			case Constants.HIDEQUERYCONFIGDLG:
				Bundle data = msg.getData();
				CamConfig camConfig = data.getParcelable("CAMPARAMCONFIG");
				System.out.println(camConfig);
				if(camConfig != null) {
					initializeEditText(camConfig);
				}
				ProgressUtil.hideProgress();
				break;
			case Constants.QUERYCONFIGERROR:
				ProgressUtil.hideProgress();
				Toast.makeText(DeviceParamSets.this, getResources().getString(R.string.device_params_request_error_str), Toast.LENGTH_SHORT).show();
				break;

			default:
				break;
			}
		}
	};
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}
	
	private void lookupEditText() {
		versionEditText = (EditText) findViewById(R.id.device_param_version);
		deviceNameEditText = (EditText) findViewById(R.id.device_param_name);
		addrTypeEditText = (EditText) findViewById(R.id.device_param_addr_type);
		addressEditText = (EditText) findViewById(R.id.device_param_address);
		inTotalSpaceEditText = (EditText) findViewById(R.id.device_param_in_tatal_space);
		outTotalSpaceEditText = (EditText) findViewById(R.id.device_param_out_tatal_space);
		enableRecordeTimeEditText = (EditText) findViewById(R.id.device_param_enable_recorde_time);
		frameRateEditText = (EditText) findViewById(R.id.device_params_recorde_real_speed_id);
	
		frameSizeSpinner = (Spinner) findViewById(R.id.device_params_recorde_frame_size_id);
		ArrayAdapter adpter = ArrayAdapter.createFromResource(this, R.array.frame_size_array, android.R.layout.simple_spinner_item);
		frameSizeSpinner.setAdapter(adpter);
		frameRateSpinner = (Spinner) findViewById(R.id.device_params_recorde_frame_speed_id);
		adpter = ArrayAdapter.createFromResource(this, R.array.frame_rate_array, android.R.layout.simple_spinner_item);
		frameRateSpinner.setAdapter(adpter);
		compressQuarSpinner = (Spinner) findViewById(R.id.device_params_recorde_comp_quar_id);
		adpter = ArrayAdapter.createFromResource(this, R.array.compress_quar_array, android.R.layout.simple_spinner_item);
		compressQuarSpinner.setAdapter(adpter);
		compressTypeSpinner = (Spinner) findViewById(R.id.device_params_recorde_comp_type_id);
		adpter = ArrayAdapter.createFromResource(this, R.array.compress_type_array, android.R.layout.simple_spinner_item);
		compressTypeSpinner.setAdapter(adpter);
		monitorFrameSizeSpinner = (Spinner) findViewById(R.id.device_params_monitor_frame_size_id);
		adpter = ArrayAdapter.createFromResource(this, R.array.monitor_frame_size_array, android.R.layout.simple_spinner_item);
		monitorFrameSizeSpinner.setAdapter(adpter);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		setContentView(R.layout.device_param_sets);
		lookupEditText();
		device = camManager.getSelectDevice();
		System.out.println(device);
		if(device == null) {
			Toast.makeText(this, getResources().getString(R.string.device_params_no_device_select_str), Toast.LENGTH_SHORT).show();
		} else {
			//vodeoSearchDia(device.getDeviceIp());
			handler.sendEmptyMessage(Constants.SHOWQUERYCONFIGDLG);
		}
	}
	
	private void initializeEditText(CamConfig camConfig) {
		versionEditText.setText(camConfig.getVersion());
		deviceNameEditText.setText(device.getDeviceName());
		String type = camConfig.getAddrType();
		if("1".equalsIgnoreCase(type)) {
			addrTypeEditText.setText(getResources().getString(R.string.device_params_static_ip_str));
		} else {
			addrTypeEditText.setText(getResources().getString(R.string.device_params_dynamic_ip_str));
		}
		addressEditText.setText(device.getDeviceIp());
		inTotalSpaceEditText.setText(camConfig.getInTotalSpace());
		outTotalSpaceEditText.setText(camConfig.getOutTotalSpace());
		enableRecordeTimeEditText.setText(camConfig.getValidRecordTime());
		frameRateEditText.setText(camConfig.getBitRate());
		String gop = camConfig.getGop();
		if(gop.equals("25")) {
			frameRateSpinner.setSelection(3);
		}
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		ProgressUtil.dismissProgress();
	}
	
 
	
}
