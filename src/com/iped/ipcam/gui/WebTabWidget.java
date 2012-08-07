package com.iped.ipcam.gui;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.TabActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.KeyEvent;
import android.widget.TabHost;
import android.widget.TabWidget;

import com.iped.ipcam.engine.CamMagFactory;
import com.iped.ipcam.engine.ICamManager;
import com.iped.ipcam.utils.Constants;

public class WebTabWidget extends TabActivity {

	public static TabHost tabHost = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.web_tab_widget);
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
UdtTools.startSearch();
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

		intent = new Intent(this, SystemSettings.class);
		tabSpec = tabHost
				.newTabSpec("SYSTEMSETTINGS")
				.setIndicator(resources.getString(R.string.system_settings_str))
				.setContent(intent);
		tabHost.addTab(tabSpec);
		// tabHost.setCurrentTabByTag("DEVICEMANAGER");
		tabHost.setCurrentTab(1);
		TabWidget tabWidget = tabHost.getTabWidget();
		int count = tabWidget.getChildCount();
		for (int i = 0; i < count; i++) {
			tabWidget.getChildAt(i).getLayoutParams().height = tabWidget
					.getChildAt(i).getLayoutParams().height * 2 / 3;

		}
	}

	@Override
	protected void onStop() {
		super.onStop();
	}

	@Override
	public boolean dispatchKeyEvent(KeyEvent event) {
		if (event.getKeyCode() == KeyEvent.KEYCODE_BACK
				&& event.getAction() == KeyEvent.ACTION_DOWN) {
			new AlertDialog.Builder(this)
					.setTitle(
							getResources().getString(R.string.user_exit_title))
					.setMessage(
							getResources()
									.getString(R.string.user_exit_message))
					.setPositiveButton(
							getResources().getString(R.string.user_exit_sure),
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {
									onDestroy();
								}
							})
					.setNegativeButton(
							getResources().getString(R.string.user_exit_cancle),
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {

								}
							}).create().show();
			return true;
		}
		return super.dispatchKeyEvent(event);
	}

	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		ICamManager camManager = CamMagFactory.getCamManagerInstance();
		camManager.clearCamList();
		int sdk_Version = android.os.Build.VERSION.SDK_INT;
		if (sdk_Version >= 8) {
			Intent startMain = new Intent(Intent.ACTION_MAIN);
			startMain.addCategory(Intent.CATEGORY_HOME);
			startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(startMain);
			System.exit(0);
		} else if (sdk_Version < 8) {
			ActivityManager activityMgr = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
			activityMgr.restartPackage(getPackageName());
		}
	}
}
