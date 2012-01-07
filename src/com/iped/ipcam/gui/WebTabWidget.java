package com.iped.ipcam.gui;

import android.app.TabActivity;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.widget.TabHost;
import android.widget.TabWidget;

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
		//intent = new Intent(this, LeftVideoView.class);
		tabSpec = tabHost.newTabSpec("VIDEOPREVIEW")
		.setIndicator(resources.getString(R.string.default_activity_str)).setContent(intent);
		tabHost.addTab(tabSpec);
		
		intent = new Intent(this, DeviceManager.class);
		tabSpec = tabHost.newTabSpec("DEVICEMANAGER")
		.setIndicator(resources.getString(R.string.device_manager_str)).setContent(intent);
		tabHost.addTab(tabSpec);
		
		intent = new Intent(this, DeviceParamSets.class);
		tabSpec = tabHost.newTabSpec("DEVICEPARAMSETS")
		.setIndicator(resources.getString(R.string.device_params_sets_str)).setContent(intent);
		tabHost.addTab(tabSpec);
		
		intent = new Intent(this, PlayBack.class);
		tabSpec = tabHost.newTabSpec("PALYBACK")
		.setIndicator(resources.getString(R.string.play_back_str)).setContent(intent);
		tabHost.addTab(tabSpec);
		
		intent = new Intent(this, SystemSettings.class);
		tabSpec = tabHost.newTabSpec("SYSTEMSETTINGS")
		.setIndicator(resources.getString(R.string.system_settings_str)).setContent(intent);
		tabHost.addTab(tabSpec);
		tabHost.setCurrentTabByTag("DEVICEMANAGER");
		TabWidget tabWidget = tabHost.getTabWidget();
		int count = tabWidget.getChildCount();
		for(int i = 0; i < count; i++) {
			tabWidget.getChildAt(i).getLayoutParams().height  = tabWidget.getChildAt(i).getLayoutParams().height * 2/3;
		}
	}
	
	
	
}
