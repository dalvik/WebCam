package com.iped.ipcam.gui;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;

import com.baidu.mobstat.StatService;
import com.iped.ipcam.engine.CamMagFactory;
import com.iped.ipcam.engine.ICamManager;
import com.iped.ipcam.pojo.Device;
import com.iped.ipcam.utils.Constants;
import com.iped.ipcam.utils.FileUtil;

public class SystemSettings extends Activity implements android.view.View.OnClickListener{

	private List<String> files = new ArrayList<String>();
	
	private ArrayAdapter<String> array = null; 
		
	private EditText videoSavePath = null;
	
	private EditText deviceName = null;
	
	private EditText devicdIP = null;
	
	private SharedPreferences settings = null;
	
	private Button dirPreviewButton = null;
	
	private EditText binPathEditText = null;
	
	private Button binDirPreviewButton = null;
	
	private Button buttonUpdate = null;
	
	private String rootPath = FileUtil.getDefaultPath();
	
	private AlertDialog alg = null;
	
	private Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			Intent intent = new Intent(SystemSettings.this, DirPreview.class);
			switch (msg.what) {
			case Constants.SHOWPREVIEWDIRDLG:
				//showDirPreviewDlg(R.string.system_settings_save_path_preview_str,true);
				intent.putExtra("DIRPREVIEW", true);
				startActivityForResult(intent, 100);
				break;
			case Constants.SHOWBINPREVIEWDIRDLG:
				//showDirPreviewDlg(R.string.system_settings_save_path_preview_str,true);
				intent.putExtra("DIRPREVIEW", false);
				startActivityForResult(intent, 200);
				break;
			default:
				break;
			}
		}
	};
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.system_settings);
		videoSavePath = (EditText) findViewById(R.id.system_settings_save_path_id);
		deviceName = (EditText) findViewById(R.id.system_settings_device_name_id);
		devicdIP = (EditText) findViewById(R.id.system_settings_device_ip_id);
		dirPreviewButton = (Button) findViewById(R.id.system_settings_preview_id);
		binPathEditText = (EditText) findViewById(R.id.system_settings_device_bin_addr_id);
		binDirPreviewButton = (Button) findViewById(R.id.system_settings_device_bin_update_id);
		buttonUpdate = (Button) findViewById(R.id.system_settings_device_update_id);
		
		dirPreviewButton.setOnClickListener(this);
		binDirPreviewButton.setOnClickListener(this);
		buttonUpdate.setOnClickListener(this);
		settings = getPreferences(0);
		String defaultPath = settings.getString("VIDEOSAVEPATH", "D:\\Program Files\\IP CAM ¼à¿ØÈí¼þ\\");
		videoSavePath.setText(defaultPath);
		ICamManager camManager = CamMagFactory.getCamManagerInstance();
		Device device = camManager.getSelectDevice();
		if(device != null) {
			deviceName.setText(device.getDeviceName());
			if(device.getDeviceNetType()) {//ÍâÍø
				devicdIP.setText(device.getUnDefine1());
			}else {
				devicdIP.setText(device.getDeviceEthIp());
			}
		}
	}


	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.system_settings_preview_id:
			handler.sendEmptyMessage(Constants.SHOWPREVIEWDIRDLG);
			break;
		case R.id.system_settings_device_bin_update_id:
			handler.sendEmptyMessage(Constants.SHOWBINPREVIEWDIRDLG);
			break;
		case R.id.system_settings_device_update_id:
			
			break;
		default:
			break;
		}
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if(resultCode == 100) {
			String savePath = data.getStringExtra("DIRFILEPATH");
			System.out.println(requestCode + " " + resultCode + " " + data.getStringExtra("FILEPATH"));
			if(savePath != null) {
				videoSavePath.setText(savePath);
				settings.edit().putString("VIDEOSAVEPATH", savePath).commit();
			}
		} else if(resultCode == 200) {
			String binPath = data.getStringExtra("BINFILEPATH");
			if(binPath != null) {
				binPathEditText.setText(binPath);
			}
			System.out.println(requestCode + " " + resultCode + " " + data.getStringExtra("BINFILEPATH"));
		}
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
