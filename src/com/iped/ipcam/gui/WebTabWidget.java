package com.iped.ipcam.gui;

import android.app.ActivityManager;
import android.app.TabActivity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TabHost;
import android.widget.TabWidget;

import com.iped.ipcam.engine.CamMagFactory;
import com.iped.ipcam.engine.ICamManager;
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
			final CustomAlertDialog alertDialog = new CustomAlertDialog(this, R.style.thems_customer_alert_dailog);
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
			alertDialog.show();
			
			/*PopupActivity popupActivity = new PopupActivity(this, R.style.thems_tips_popup_dailog);
			popupActivity.setContentView(R.layout.layout_web_cam_exit);
			popupActivity.show();
			WindowManager.LayoutParams params = popupActivity.getWindow().getAttributes();
			params.width = 800*20/36;
			params.height = 600/2;
			popupActivity.getWindow().setAttributes(params);*/
			
			/*new AlertDialog.Builder(this)
					.setTitle(getResources().getString(R.string.user_exit_title))
					.setMessage(getResources().getString(R.string.user_exit_message))
					.setPositiveButton(getResources().getString(R.string.user_exit_sure),
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {
									UdtTools.close();
									onDestroy();
								}
							})
					.setNegativeButton(
							getResources().getString(R.string.user_exit_cancle),
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {

								}
							}).create().show();*/
			return true;
		}
		return super.dispatchKeyEvent(event);
	}

	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		ICamManager camManager = CamMagFactory.getCamManagerInstance();
		camManager.clearCamList();
		//UdtTools.exit();
		UdtTools.freeConnection();
		UdtTools.close();
		UdtTools.cleanUp();
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
