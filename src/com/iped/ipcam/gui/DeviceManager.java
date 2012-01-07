package com.iped.ipcam.gui;

import android.app.ListActivity;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;

import com.iped.ipcam.engine.CamMagFactory;
import com.iped.ipcam.engine.ICamManager;
import com.iped.ipcam.pojo.Device;
import com.iped.ipcam.utils.Constants;
import com.iped.ipcam.utils.DeviceAdapter;

public class DeviceManager extends ListActivity implements OnClickListener {

	private ListView listView = null;
	
	private DeviceAdapter adapter = null;
	
	private final int MENU_EDIT = Menu.FIRST;
	
	private final int MENU_DEL = Menu.FIRST + 1;
	
	private final int MENU_PREVIEW = Menu.FIRST + 2;
	
	private Button autoSearchButton = null;
	
	private Button claerCamButton = null;
	
	private ICamManager camManager = null;
	
	private ProgressDialog progressDialog = null;
	
	private int lastSelected = 0;
	
	private static final String TAG = "DeviceManager";
	
	private Handler handler = new Handler() {
		@Override
		public void handleMessage(Message message) {
			switch (message.what) {
			case Constants.UPDATEAUTOSEARCH:
				int value = message.arg1;
				progressDialog.setProgress(message.arg1);
				if(value >= 99){
					progressDialog.dismiss();
				}
				break;
			case Constants.HIDETEAUTOSEARCH:
				adapter.notifyDataSetChanged();
				break;
			case Constants.UPDATEDEVICELIST:
				progressDialog.dismiss();
				break;
			case Constants.DEFAULTUSERSELECT:
				listView.requestFocusFromTouch();
				listView.setSelection(lastSelected);	
				break;
			default:
				break;
			}
		}
	};
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.device_manager);
		autoSearchButton = (Button) findViewById(R.id.auto_search_button);
		claerCamButton = (Button) findViewById(R.id.clear_all_button);
		camManager = CamMagFactory.getCamManagerInstance();
		adapter = new DeviceAdapter(camManager.getCamList(), this);
		listView = getListView();
		listView.setAdapter(adapter);
		listView.setOnItemClickListener(itemClickListener);
		registerForContextMenu(getListView());
		autoSearchButton.setOnClickListener(this);
		claerCamButton.setOnClickListener(this);
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
		Device device = camManager.getDevice(info.position);
		menu.setHeaderTitle(device.getDeviceName());
		menu.add(0, MENU_EDIT, 1, getString(R.string.device_edit_str));
		menu.add(0, MENU_DEL, 2, getString(R.string.device_del_str));
		menu.add(0, MENU_PREVIEW, 3, getString(R.string.device_preview_str));
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo infor = (AdapterContextMenuInfo) item.getMenuInfo();
		Device device = camManager.getDevice(infor.position);
		switch (item.getItemId()) {
		case MENU_EDIT:
			Toast.makeText(this,"edit", Toast.LENGTH_SHORT).show();
			break;

		case MENU_DEL:
			camManager.delCam(infor.position);
			handler.sendEmptyMessage(Constants.UPDATEDEVICELIST);
			break;
		case MENU_PREVIEW:
			WebTabWidget.tabHost.setCurrentTabByTag("VIDEOPREVIEW");
			break;
		default:
			break;
		}
		return super.onContextItemSelected(item);
		
	}
	
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.auto_search_button:
			showProgress();
			camManager.startThread(handler);
			break;
		case R.id.clear_all_button:
			camManager.clearCamList();
			handler.sendEmptyMessage(Constants.UPDATEDEVICELIST);
		default:
			break;
		}
	}
	
	private void showProgress() {
		progressDialog = new ProgressDialog(DeviceManager.this);
		progressDialog.setTitle(getResources().getString(R.string.auto_search_tips_str));
		progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		progressDialog.show();
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if(keyCode == KeyEvent.KEYCODE_BACK) {
			camManager.stopThread();
		}
		return super.onKeyDown(keyCode, event);
	}
	
	private OnItemClickListener itemClickListener = new OnItemClickListener() {

		@Override
		public void onItemClick(AdapterView<?> arg0, View arg1, int index,
				long arg3) {
			listView.requestFocusFromTouch();
			lastSelected = index;
			listView.setSelection(index);
			camManager.setSelectInde(index);
		}
	};
	
	protected void onResume() {
		super.onResume();
		handler.sendMessageDelayed(handler.obtainMessage(Constants.DEFAULTUSERSELECT), 50);
	};
}
