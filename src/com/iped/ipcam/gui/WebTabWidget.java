package com.iped.ipcam.gui;

import android.app.TabActivity;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.KeyEvent;
import android.widget.TabHost;
import android.widget.TabWidget;

import com.baidu.mobstat.StatService;
import com.iped.ipcam.engine.UpdateManager;
import com.iped.ipcam.mail.ExceptionHandler;
import com.iped.ipcam.utils.Constants;

public class WebTabWidget extends TabActivity {

	public static TabHost tabHost = null;

	public static int tabHeight = 0;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.web_tab_widget);
		ExceptionHandler crashHandler = ExceptionHandler.getInstance();    
		crashHandler.init(this);
		Resources resources = getResources();
		tabHost = getTabHost();
		TabHost.TabSpec tabSpec;
		Intent intent;
		intent = new Intent(this, CamVideoH264.class);
		// intent = new Intent(this, LeftVideoView.class);
		tabSpec = tabHost
				.newTabSpec(Constants.VIDEOPREVIEW)
				.setIndicator(
						resources.getString(R.string.default_activity_str))
				.setContent(intent);
		tabHost.addTab(tabSpec);

		intent = new Intent(this, DeviceManager.class);
		tabSpec = tabHost.newTabSpec("DEVICEMANAGER")
				.setIndicator(resources.getString(R.string.device_manager_str))
				.setContent(intent);
		tabHost.addTab(tabSpec);
		/*
		 * intent = new Intent(this, DeviceParamSets.class); tabSpec =
		 * tabHost.newTabSpec("DEVICEPARAMSETS")
		 * .setIndicator(resources.getString
		 * (R.string.device_params_sets_str)).setContent(intent);
		 * tabHost.addTab(tabSpec);
		 */
		
		intent = new Intent(this, PlayBack.class);
		tabSpec = tabHost.newTabSpec("PALYBACK")
				.setIndicator(resources.getString(R.string.play_back_str))
				.setContent(intent);
		tabHost.addTab(tabSpec);
		/*
		intent = new Intent(this, SystemSettings.class);
		tabSpec = tabHost
				.newTabSpec("SYSTEMSETTINGS")
				.setIndicator(resources.getString(R.string.system_settings_str))
				.setContent(intent);
		tabHost.addTab(tabSpec);
		*/
		// tabHost.setCurrentTabByTag("DEVICEMANAGER");
		tabHost.setCurrentTab(1);
		TabWidget tabWidget = tabHost.getTabWidget();
		int count = tabWidget.getChildCount();
		for (int i = 0; i < count; i++) {
			tabHeight = tabWidget.getChildAt(i).getLayoutParams().height * 2 / 3;
			tabWidget.getChildAt(i).getLayoutParams().height = tabHeight; 
		}
		UpdateManager.getUpdateManager().checkAppUpdate(this, false);
	}

	@Override
	protected void onStop() {
		super.onStop();
	}

	@Override
	public boolean dispatchKeyEvent(KeyEvent event) {
		if (event.getKeyCode() == KeyEvent.KEYCODE_BACK	&& event.getAction() == KeyEvent.ACTION_DOWN) {
			Intent intent = new Intent(this, LogoutDialog.class);
			startActivity(intent);
			/*final CustomAlertDialog alertDialog = new CustomAlertDialog(this, R.style.thems_customer_alert_dailog);
			alertDialog.setContentView(R.layout.layout_web_cam_exit);
			alertDialog.setTitle(getResources().getString(R.string.user_exit_title));
			alertDialog.findViewById(R.id.web_cam_user_continue).setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					alertDialog.dismiss();
				}
			});
			alertDialog.findViewById(R.id.web_cam_user_exit).setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					alertDialog.dismiss();
					UdtTools.close();
					onDestroy();
				}
			});
			alertDialog.show();*/
			return true;
		}
		return super.dispatchKeyEvent(event);
	}

	
	@Override
	protected void onDestroy() {
		super.onDestroy();
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
