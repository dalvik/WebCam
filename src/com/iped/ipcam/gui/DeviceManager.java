package com.iped.ipcam.gui;

import java.util.ArrayList;
import java.util.List;

import android.app.ListActivity;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;

import com.iped.ipcam.pojo.Device;
import com.iped.ipcam.utils.DeviceAdapter;

public class DeviceManager extends ListActivity {

	private DeviceAdapter adapter = null;
	
	private List<Device> deviceList = null;
	
	private final int MENU_EDIT = Menu.FIRST;
	
	private final int MENU_DEL = Menu.FIRST + 1;
	
	private final int MENU_PREVIEW = Menu.FIRST + 2;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.device_manager);
		deviceList = new ArrayList<Device>();
		Device d = new Device("test", "type", "192.168.1.122", "18032", "18033", "255.255.255.0");
		deviceList.add(d);
		adapter = new DeviceAdapter(deviceList, this);
		getListView().setAdapter(adapter);
		registerForContextMenu(getListView());
	}
	
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		AdapterView.AdapterContextMenuInfo info = null;
		try{
			info = (AdapterView.AdapterContextMenuInfo) menuInfo;
		} catch (Exception e) {
			return;
		}
		Device device = deviceList.get(info.position);
		menu.setHeaderTitle(device.getDeviceName());
		menu.add(0, MENU_EDIT, 1, getString(R.string.device_edit_str));
		menu.add(0, MENU_DEL, 2, getString(R.string.device_del_str));
		menu.add(0, MENU_PREVIEW, 3, getString(R.string.device_preview_str));
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo infor = (AdapterContextMenuInfo) item.getMenuInfo();
		Device device = deviceList.get(infor.position);
		switch (item.getItemId()) {
		case MENU_EDIT:
			Toast.makeText(this,"edit", Toast.LENGTH_SHORT).show();
			break;

		case MENU_DEL:
			
			Toast.makeText(this,"del", Toast.LENGTH_SHORT).show();
			break;
		default:
			break;
		}
		return super.onContextItemSelected(item);
		
	}
}
