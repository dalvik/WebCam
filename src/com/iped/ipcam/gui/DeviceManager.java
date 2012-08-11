package com.iped.ipcam.gui;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Toast;

import com.iped.ipcam.engine.CamMagFactory;
import com.iped.ipcam.engine.ICamManager;
import com.iped.ipcam.pojo.Device;
import com.iped.ipcam.utils.CamCmdListHelper;
import com.iped.ipcam.utils.Constants;
import com.iped.ipcam.utils.DeviceAdapter;
import com.iped.ipcam.utils.DialogUtils;
import com.iped.ipcam.utils.ErrorCode;
import com.iped.ipcam.utils.FileUtil;
import com.iped.ipcam.utils.PackageUtil;
import com.iped.ipcam.utils.ParaUtil;
import com.iped.ipcam.utils.RandomUtil;
import com.iped.ipcam.utils.ToastUtils;
import com.iped.ipcam.utils.WebCamActions;

public class DeviceManager extends ListActivity implements OnClickListener, OnItemLongClickListener {

	private ListView listView = null;

	private DeviceAdapter adapter = null;

	private final int MENU_EDIT = Menu.FIRST;

	private final int MENU_DEL = Menu.FIRST + 1;

	private final int MENU_PREVIEW = Menu.FIRST + 2;

	private Button autoSearchButton = null;

	private Button manulAddButton = null;

	private Button deviceParaSetButton = null;
	
	private Button clearCamButton = null;

	private ICamManager camManager = null;

	private ProgressDialog progressDialog = null;

	private ProgressDialog queryNewCameraDialog = null;
	
	private AlertDialog dlg = null;

	private int lastSelected = 0;

	private String ip;
	
	private int port1;
	
	private int port2;
	
	private int port3;
	
	private String deviceName = "";
	
	private String pwd;
	
	private Device deviceTmp;
	
	private Device device;
	
	private ProgressDialog m_Dialog = null;
	
	private String password = "";
	
	private static final String TAG = "DeviceManager";

	private Handler handler = new Handler() {
		@Override
		public void handleMessage(Message message) {
			switch (message.what) {
			/*case Constants.UPDATEAUTOSEARCH:
				int value = message.arg1;
				progressDialog.setProgress(message.arg1);
				if (value >= 100) {
					dismissProgress();
				}
				break;*/
			case Constants.HIDETEAUTOSEARCH:
				dismissProgress();
				//FileUtil.persistentDevice(DeviceManager.this,camManager.getCamList());
				break;
			case Constants.UPDATEDEVICELIST:
				adapter.notifyDataSetChanged();
				FileUtil.persistentDevice(DeviceManager.this,camManager.getCamList());
				handler.sendEmptyMessageDelayed(Constants.SEND_UPDATE_DEVICE_LIST_MSG, 200);
				break;
			case Constants.DEFAULTUSERSELECT:
				listView.requestFocusFromTouch();
				listView.setSelection(lastSelected);
				break;
			case Constants.SHOWTOASTMSG:
				int id = message.arg1;
				showToast(id);
				break;
			case Constants.SHOWRESULTDIALOG:
				Bundle bundle = message.getData();
				if(bundle != null) {
					Object obj = bundle.get("NEW_DEVICE");
					if(obj instanceof Device) {
						showResult((Device)obj);
					}
				}
				break;
			case Constants.SENDGETTHREEPORTMSG:
				Bundle bd = message.getData();
				if(bd != null) {
					ip = bd.getString("IPADDRESS");
					port1 = bd.getInt("PORT1");
					port2 = bd.getInt("PORT2");
					port3 = bd.getInt("PORT3");
					dismissProgress();
					getDeviceConfig(ip, port1, port2, port3);
				}
				break;
			case Constants.SENDGETUNFULLPACKAGEMSG:
				showToast(R.string.device_manager_find_device_id_error);
				handler.sendEmptyMessage(Constants.HIDETEAUTOSEARCH);
				break;
			case Constants.SENDGETTHREEPORTTIMOUTMSG:
				showToast(R.string.device_manager_find_device_id_error);
				handler.sendEmptyMessage(Constants.HIDETEAUTOSEARCH);
				break;
			case Constants.SEND_SHOW_ONE_PWD_FIELD_CONFIG_MSG:
			case Constants.SEND_SHOW_TWO_PWD_FIELD_CONFIG_MSG:
				pwd = (String) message.obj;
				Intent intent = new Intent(WebCamActions.QUERY_CONFIG_ACTION);
            	intent.setDataAndType(Uri.parse(pwd), WebCamActions.QUERY_CONFIG_MINITYPE);
            	Log.d(TAG, "======>" + pwd);
            	startActivity(intent);
				break;
			case Constants.SEND_SHOW_ONE_PWD_FIELD_PREVIEW_MSG:
				
				break;
			/*case Constants.SEND_SHOW_TWO_PWD_FIELD_PREVIEW_MSG:
				Device device = camManager.getSelectDevice();
				pwd = (String) message.obj;
				device.setUnDefine2(pwd);
				int checkPwd = PackageUtil.checkPwd(device.getUnDefine2());
				if(checkPwd == 1) {
					camManager.updateCam(device);
					FileUtil.persistentDevice(DeviceManager.this,camManager.getCamList());
					WebTabWidget.tabHost.setCurrentTabByTag(Constants.VIDEOPREVIEW);
					Intent intent2 = new Intent();
					Bundle bundle2 = new Bundle();
					bundle2.putString("PLVIDEOINDEX",""); 
					bundle2.putSerializable("IPPLAY", device);
					intent2.putExtras(bundle2);
					intent2.setAction(Constants.ACTION_IPPLAY);
					sendBroadcast(intent2);
				} else {
					ToastUtils.showToast(DeviceManager.this, R.string.device_manager_pwd_set_err);
				}
				break;*/
			case Constants.SEND_GET_CONFIG_MSG:
				String pwd2 = (String) message.obj;
				queryDeviceConfig(pwd2);
				break;
			case Constants.SEND_ADD_NEW_DEVICE_BY_IP_MSG:
				String pass = (String) message.obj;
				deviceTmp.setUnDefine2(pass);
				FileUtil.persistentDevice(DeviceManager.this,camManager.getCamList());
				queryDeiceConfigByIp();
				break;
			case Constants.SEND_UPDATE_DEVICE_LIST_MSG:
				sendBroadcast(new Intent(WebCamActions.SEND_DEVICE_LIST_UPDATE_ACTION));
				break;
			case Constants.WEB_CAM_CONNECT_INIT_MSG:
				String random = RandomUtil.generalRandom();
				Log.d(TAG, "random = " + random);
				int initRes = UdtTools.initialSocket(random);
				if(initRes<0) {
					Log.d(TAG, "initialSocket init error!");
					ToastUtils.showToast(DeviceManager.this, R.string.webcam_connect_init_error);
				}else {
					handler.sendEmptyMessage(Constants.WEB_CAM_CHECK_PWD_STATE_MSG);
				}
				break;
			case Constants.WEB_CAM_CHECK_PWD_STATE_MSG:
				int resu = PackageUtil.checkPwdState();
				Log.d(TAG, "device manager checkPwdState result = " + resu);
				handler.removeMessages(Constants.WEB_CAM_SHOW_CHECK_PWD_DLG_MSG);
				handler.sendEmptyMessage(Constants.WEB_CAM_HIDE_CHECK_PWD_DLG_MSG);
				if(resu == 0) { // unset
					handler.sendEmptyMessage(Constants.SEND_SHOW_INPUT_TWO_PASS_DIALOG_SMG);
					//DialogUtils.inputTwoPasswordDialog(DeviceManager.this, device, handler, Constants.SEND_SHOW_TWO_PWD_FIELD_PREVIEW_MSG);
				} else if(resu == 1) {// pwd seted
					if(device.getUnDefine2() != null && device.getUnDefine2().length()>0) {
						Message msg = handler.obtainMessage();
						msg.obj  = device.getUnDefine2();
						msg.what = Constants.WEB_CAM_CHECK_PWD_MSG;
						handler.sendMessage(msg);
					}else {
						handler.sendEmptyMessage(Constants.SEND_SHOW_INPUT_ONE_PASS_DIALOG_SMG);
					}
				} else if(resu == 2){
					ToastUtils.showToast(DeviceManager.this, R.string.device_manager_pwd_set_err);
					//DialogUtils.inputOnePasswordDialog(DeviceManager.this, handler, Constants.SEND_SHOW_TWO_PWD_FIELD_PREVIEW_MSG);
				} else {
					ToastUtils.showToast(DeviceManager.this, R.string.device_manager_time_out_or_device_off_line);
				}
				break;
			case Constants.WEB_CAM_SHOW_CHECK_PWD_DLG_MSG:
				showProgressDlg(R.string.webcam_check_pwd_dialog_str);
				break;
			case Constants.WEB_CAM_HIDE_CHECK_PWD_DLG_MSG:
				hideProgressDlg();
				break;
			case Constants.SEND_SHOW_INPUT_TWO_PASS_DIALOG_SMG:
				DialogUtils.inputTwoPasswordDialog(DeviceManager.this, camManager.getSelectDevice(), handler, Constants.WEB_CAM_CHECK_PWD_MSG);
				break;
			case Constants.SEND_SHOW_INPUT_ONE_PASS_DIALOG_SMG:
				DialogUtils.inputOnePasswordDialog(DeviceManager.this, handler, Constants.WEB_CAM_CHECK_PWD_MSG);
				break;
			case Constants.WEB_CAM_CHECK_PWD_MSG:
				handler.sendEmptyMessage(Constants.WEB_CAM_SHOW_CHECK_PWD_DLG_MSG);
				password = (String) message.obj;
				handler.removeCallbacks(checkPwdRunnable);
				handler.post(checkPwdRunnable);
				break;
			
			default:
				break;
			}
		}
	};

	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		UdtTools.startUp();
		setContentView(R.layout.device_manager);
		autoSearchButton = (Button) findViewById(R.id.auto_search_button);
		manulAddButton = (Button) findViewById(R.id.manul_add_button);
		deviceParaSetButton = (Button) findViewById(R.id.device_manager_button);
		clearCamButton = (Button) findViewById(R.id.clear_all_button);
		camManager = CamMagFactory.getCamManagerInstance();
		adapter = new DeviceAdapter(camManager.getCamList(), this);
		camManager.getCamList().addAll(FileUtil.fetchDeviceFromFile(this));
		//adapter = new DeviceAdapter(, this);
		listView = getListView();
		listView.setAdapter(adapter);
		listView.setOnItemClickListener(itemClickListener);
		listView.setOnItemLongClickListener(this);
		registerForContextMenu(getListView());
		autoSearchButton.setOnClickListener(this);
		manulAddButton.setOnClickListener(this);
		deviceParaSetButton.setOnClickListener(this);
		clearCamButton.setOnClickListener(this);
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		AdapterView.AdapterContextMenuInfo info = null;
		try {
			info = (AdapterView.AdapterContextMenuInfo) menuInfo;
		} catch (Exception e) {
			return;
		}
		Device device = camManager.getDevice(info.position);
		menu.setHeaderTitle(device.getDeviceName());
		menu.add(0, MENU_PREVIEW, 1, getString(R.string.device_preview_str));
		menu.add(0, MENU_DEL, 2, getString(R.string.device_del_str));
		menu.add(0, MENU_EDIT, 3, getString(R.string.device_edit_str));
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo infor = (AdapterContextMenuInfo) item
				.getMenuInfo();
		switch (item.getItemId()) {
		case MENU_EDIT:
			//editDevice(device);
			break;

		case MENU_DEL:
			camManager.delCam(infor.position);
			handler.sendEmptyMessage(Constants.UPDATEDEVICELIST);
			break;
		case MENU_PREVIEW:
			WebTabWidget.tabHost.setCurrentTabByTag(Constants.VIDEOPREVIEW);
			/*Intent intent2 = new Intent();
			Bundle bundle2 = new Bundle();
			bundle2.putSerializable("IPPLAY", device);
			intent2.putExtras(bundle2);
			intent2.setAction(WebCamActions.ACTION_IPPLAY);*/
			sendBroadcast(new Intent(WebCamActions.ACTION_IPPLAY));
			/*Device d = camManager.getSelectDevice();
			if(d == null) {
				ToastUtils.showToast(DeviceManager.this, R.string.device_params_info_no_device_str);
				return super.onContextItemSelected(item);
			}
			if(!d.equals(device)) {
				this.device = d;
			}
			sendBroadcast(new Intent(WebCamActions.WEB_CAM_CLOSE_CONN_ACTION));
			handler.sendEmptyMessageDelayed(Constants.WEB_CAM_SHOW_CHECK_PWD_DLG_MSG, 1000);
			handler.removeCallbacks(monitorSocketTask);
			handler.postDelayed(monitorSocketTask, 100);*/
			//handler.sendEmptyMessage(Constants.WEB_CAM_THROUGH_NET_MSG);
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
		case R.id.manul_add_button:
			addNewDevice();
			break;
		case R.id.device_manager_button:
			Device device = camManager.getSelectDevice();
			if(device == null) {
				Log.d(TAG, "device = " + device);
				ToastUtils.showToast(DeviceManager.this, R.string.device_params_info_no_device_str);
				return;
			}
			handler.sendEmptyMessage(Constants.WEB_CAM_SHOW_CHECK_PWD_DLG_MSG);
			handler.removeCallbacks(monitorSocketTask);
			handler.postDelayed(monitorSocketTask, 100);
			
			break;
		case R.id.clear_all_button:
			if(camManager.getCamList().size()>0) {
				new AlertDialog.Builder(this).setTitle(getResources().getString(R.string.clear_all_device_title_str))
				.setMessage(getResources().getString(R.string.clear_all_device_message_str))
				.setPositiveButton(getResources().getString(R.string.clear_all_device_ok_str),
						new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						camManager.clearCamList();
						handler.sendEmptyMessage(Constants.UPDATEDEVICELIST);
					}
				}).setNegativeButton(getResources().getString(R.string.clear_all_device_cancle_str), 
						new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						
					}
				})
				.create().show();
			}
	        
		default:
			break;
		}
	}

	
	private void showProgress() {
		if(progressDialog != null) {
			dismissProgress();
		}
		progressDialog = new ProgressDialog(DeviceManager.this);
		progressDialog.setTitle(getResources().getString(R.string.auto_search_tips_str));
		progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		//progressDialog.setCancelable(false);
		progressDialog.show();
	}

	public void dismissProgress() {
		if (progressDialog != null && progressDialog.isShowing()) {
			progressDialog.dismiss();
		}
		
		if(queryNewCameraDialog != null){
			queryNewCameraDialog.dismiss();
		}
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK && progressDialog != null && progressDialog.isShowing()) {
			dismissProgress();
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
			adapter.setChecked(index);
			adapter.notifyDataSetChanged();	
		}
	};

	@Override
	public boolean onItemLongClick(AdapterView<?> parent, View view,
			int position, long id) {
		listView.requestFocusFromTouch();
		lastSelected = position;
		listView.setSelection(position);
		camManager.setSelectInde(position);
		adapter.setChecked(position);
		adapter.notifyDataSetChanged();	
		return false;
	}
	

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}
	
	private void addNewDevice() {
		final View addDeviceView = initAddNewDeviceView();
		final CheckBox serchConfig = (CheckBox) addDeviceView
				.findViewById(R.id.device_manager_search_device_checkbox);
		updateComponentState(addDeviceView, serchConfig.isChecked());
		serchConfig
				.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
					@Override
					public void onCheckedChanged(CompoundButton buttonView,
							boolean isChecked) {
						updateComponentState(addDeviceView, isChecked);
					}
				});
		dlg = new AlertDialog.Builder(DeviceManager.this)
				.setTitle(
						getResources().getString(
								R.string.device_manager_add_title_str))
				.setView(addDeviceView)
				.setPositiveButton(
						getResources().getString(
								R.string.device_manager_add_sure_str),
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								EditText ed = (EditText)addDeviceView.findViewById(R.id.device_manager_add_name_id);
								deviceName = ed.getText().toString();
								unCloseDialog(dlg, -1, false);
								String newDiviceName = ((EditText) addDeviceView
										.findViewById(R.id.device_manager_add_name_id))
										.getText().toString();
								if (serchConfig.isChecked()) {
									String cameraId = ((EditText) addDeviceView
											.findViewById(R.id.device_manager_new_device_id_edittext))
											.getText().toString().trim();
									if (cameraId == null || cameraId.length() <= 0) {
										unCloseDialog(dlg,R.string.device_manager_new_device_id_not_null,
												false);
										return;
									}
									if(!cameraId.matches("\\d+")) {
										unCloseDialog(
												dlg,
												R.string.device_manager_new_device_id_not_number,
												false);
										return;
									}
									queryNewCameraDialog = ProgressDialog.show(DeviceManager.this, getString(R.string.device_manager_add_query_title_str), getString(R.string.device_manager_add_query_message_str));
									new Thread(new QueryCamera(newDiviceName, cameraId,"","","","", true))
											.start();
								} else {
									String newDiviceIP = ((EditText) addDeviceView
											.findViewById(R.id.device_manager_new_addr_id))
											.getText().toString();
									String newDiviceGateWay = ((EditText) addDeviceView
											.findViewById(R.id.device_manager_new_gateway_addr_id))
											.getText().toString();
									String newDiviceDNS1 = ((EditText) addDeviceView
											.findViewById(R.id.device_manager_new_dns1_id))
											.getText().toString();
									String newDiviceDNS2 = ((EditText) addDeviceView
											.findViewById(R.id.device_manager_new_dns2_id))
											.getText().toString();
									String newDiviceMask = ((EditText) addDeviceView
											.findViewById(R.id.device_manager_new_sub_net_addr_id))
											.getText().toString();
									if (newDiviceIP == null
											|| newDiviceIP.length() <= 0) {
										unCloseDialog(
												dlg,
												R.string.device_manager_add_not_null_str,
												false);
										return;
									}
									queryNewCameraDialog = ProgressDialog.show(DeviceManager.this, getString(R.string.device_manager_add_query_title_str), getString(R.string.device_manager_add_query_message_str));
									new Thread(new QueryCamera(newDiviceName, newDiviceIP,newDiviceGateWay,newDiviceMask,newDiviceDNS1,newDiviceDNS2,
											false)).start();
								}
							}
						})
				.setNegativeButton(
						getResources().getString(
								R.string.device_manager_add_cancle_str),
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								unCloseDialog(dlg, -1, true);
							}
						}).create();
		dlg.show();
	}

	private void updateComponentState(View addDeviceView, final boolean isChecked) {
		LinearLayout paramSubnetLayout = (LinearLayout) addDeviceView.findViewById(R.id.subnet_param_layout);
		LinearLayout deviceIdLayout = (LinearLayout) addDeviceView.findViewById(R.id.device_id_param_layout);
		if(isChecked) {
			paramSubnetLayout.setVisibility(View.GONE);
			deviceIdLayout.setVisibility(View.VISIBLE);
		} else {
			paramSubnetLayout.setVisibility(View.VISIBLE);
			deviceIdLayout.setVisibility(View.GONE);
		}
	}

	private void unCloseDialog(AlertDialog dialog, int id, boolean flag) {
		if (id > 0) {
			Toast.makeText(DeviceManager.this, getResources().getString(id),
					Toast.LENGTH_LONG).show();
		}
		try {
			Field field = dlg.getClass().getSuperclass()
					.getDeclaredField("mShowing");
			field.setAccessible(true);
			field.set(dialog, flag);
			dialog.dismiss();
		} catch (Exception e) {
			Log.v(TAG, e.getMessage());
		}
	}

	private View initAddNewDeviceView() {
		LayoutInflater inflater = LayoutInflater.from(DeviceManager.this);
		View addDeviceView = inflater
				.inflate(R.layout.device_manager_add, null);
		((EditText) addDeviceView
				.findViewById(R.id.device_manager_new_dns1_id)).setText("");// Constants.TCPPORT+
		((EditText) addDeviceView
				.findViewById(R.id.device_manager_new_dns2_id)).setText(""); // Constants.UDPPORT+
		return addDeviceView;
	}

	protected void onResume() {
		super.onResume();
		handler.sendEmptyMessage(Constants.UPDATEDEVICELIST);
		handler.sendMessageDelayed(
				handler.obtainMessage(Constants.DEFAULTUSERSELECT), 50);
	};

	private void showToast(int id) {
		Toast.makeText(this, getText(id), Toast.LENGTH_SHORT).show();
	}
	
	private class QueryCamera implements Runnable {

		private String name;
		
		private String ip;

		private String gateWay;
		
		private String mask;
		
		private String dns1;
		
		private String dns2;
		
		private boolean flag;

		public QueryCamera(String name, String ip, String gateWay, String mask, String dns1,String dns2, boolean flag) {
			this.name = name;
			this.ip = ip;
			this.gateWay = gateWay;
			this.mask = mask;
			this.dns1 = dns1;
			this.dns2 = dns2;
			this.flag = flag;
		}

		@Override
		public void run() {
			if (flag) {// 根据用户ID添加设备
			} else {//根据IP地址添加设备
				//检查密码设置状态
				//TODO
				deviceTmp = new Device();
				deviceTmp.setDeviceName(name);
				deviceTmp.setDeviceNetType(false);
				deviceTmp.setDeviceEthIp(ip);
				deviceTmp.setDeviceEthGateWay(gateWay);
				deviceTmp.setDeviceEthMask(mask);
				deviceTmp.setDeviceEthDNS1(dns1);
				deviceTmp.setDeviceEthDNS2(dns2);
				int resu = PackageUtil.checkPwdState();
				handler.sendEmptyMessage(Constants.HIDETEAUTOSEARCH);
				if(resu == 1) { // 密码已设置
					handler.sendEmptyMessage(Constants.SEND_SHOW_INPUT_ONE_PASS_DIALOG_SMG);
				} else if(resu == 0) { // 密码未设置
					handler.sendEmptyMessage(Constants.SEND_SHOW_INPUT_TWO_PASS_DIALOG_SMG);
				} else { // 其他
					Message msg = handler.obtainMessage();
					msg.arg1 = R.string.device_manager_find_device_id_not_online;
					msg.what = Constants.SHOWTOASTMSG;
					handler.sendMessage(msg);
				}
				/*DatagramSocket datagramSocket = null;
				byte [] tem = CamCmdListHelper.GetCmd_Config.getBytes();
				byte [] buffTemp = new byte[Constants.COMMNICATEBUFFERSIZE];
				StringBuffer sb = new StringBuffer();
				String tmp = null;*/
				//try {
					/*datagramSocket = new DatagramSocket();
					datagramSocket.setSoTimeout(Constants.VIDEOSEARCHTIMEOUT);
					DatagramPacket datagramPacket = new DatagramPacket(tem, tem.length, InetAddress.getByName(ip), Constants.LOCALCMDPORT);
					datagramSocket.send(datagramPacket);
					DatagramPacket rece = new DatagramPacket(buffTemp, buffTemp.length);
					int recvLength = 0;
					while(true) {
						datagramSocket.receive(rece);
						int l = rece.getLength();
						byte[] ipByte = new byte[4];
						System.arraycopy(buffTemp, 0, ipByte, 0, 4);
						recvLength += Integer.parseInt(new String(ipByte).trim());
						tmp = new String(buffTemp,4,l-4).trim();
						sb.append(tmp);
						Log.d(TAG, "Receive inof //////////////"  + l + " " + tmp);
						if(recvLength>1000) {
							break;
						}
					}
					
					Map<String,String> paraMap = new LinkedHashMap<String,String>();
					ParaUtil.putParaByString(sb.toString(), paraMap);
					int resu = PackageUtil.checkPwdState(device);
					boolean flag = PackageUtil.pingTest(CamCmdListHelper.GetCmd_Config, ip, Constants.LOCALCMDPORT);
					*/
				/*} catch (IOException e) {
					Message msg = handler.obtainMessage();
					msg.arg1 = R.string.device_manager_find_device_id_not_online;
					msg.what = Constants.SHOWTOASTMSG;
					handler.sendMessage(msg);
					Log.d(TAG, "CamManagerImp isOnline : " + e.getLocalizedMessage());
				} finally {
					if(datagramSocket != null) {
						datagramSocket.close();
						datagramSocket = null;
					}
					handler.sendEmptyMessage(Constants.HIDETEAUTOSEARCH);
				}*/
			}
		}
	}

	private AlertDialog ad  = null;
	
	private void showResult(final Device device) {
		ad = new AlertDialog.Builder(DeviceManager.this)
		.setTitle(getString(R.string.device_manager_new_device_title))
		.setMessage(getString(R.string.device_manager_new_device_message))
		.setPositiveButton(getString(R.string.system_settings_save_path_preview_sure_str), new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				if(camManager.addCam(device)) {
					handler.sendEmptyMessage(Constants.UPDATEDEVICELIST);
					unCloseDialog(dlg, -1, true); 
				} else {
					showToast(R.string.device_manager_add_device_is__exist);
				}
			}
			
		}).setNegativeButton(getString(R.string.system_settings_save_path_preview_cancle_str), new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				ad.dismiss();
			}
		}).create();
		ad.show();
	}
	
	//TODO
	public void getDeviceConfig(String ip, int port1, int port2, int port3) {
		Device tempDevice = new Device();
		tempDevice.setDeviceNetType(true);
		tempDevice.setDeviceRemoteCmdPort(port1);
		tempDevice.setUnDefine1(ip);
		int result = PackageUtil.checkPwdState();
		Log.d(TAG, "DeviceManager getDeviceConfig checkPwdState result = " + result);
		if(result == 0) { // unset
			DialogUtils.inputTwoPasswordDialog(DeviceManager.this, tempDevice, handler, Constants.SEND_GET_CONFIG_MSG);
		} else if(result == 1) {// pwd seted
			/*int checkPwd = PackageUtil.checkPwd(tempDevice);
			if(checkPwd == 1) {
			} else {
				ToastUtils.showToast(DeviceManager.this, R.string.device_manager_pwd_set_err);
			}*/
			DialogUtils.inputOnePasswordDialog(DeviceManager.this, handler, Constants.SEND_GET_CONFIG_MSG);
		} else if(result == 2){
			ToastUtils.showToast(DeviceManager.this, R.string.device_manager_pwd_set_err);
			DialogUtils.inputOnePasswordDialog(DeviceManager.this, handler, Constants.SEND_GET_CONFIG_MSG);
		}else {
			ToastUtils.showToast(DeviceManager.this, R.string.device_manager_time_out_or_device_off_line);
		}
	}

	public void queryDeviceConfig(String pwd) {
		//TODO
		String cmd = null;
		/*try {
			cmd = PackageUtil.CMDPackage2( CamCmdListHelper.GetCmd_Config + pwd + "\0", ip, port1);
			if(cmd == null)  {
				return;
			}
			if("PSWD_FAIL".equals(cmd)) {
				ToastUtils.showToast(DeviceManager.this, R.string.device_manager_pwd_set_err);
				return;
			}
			System.out.println("cmd=" + cmd);
		} catch (CamManagerException e) {
			Log.d(TAG, e.getLocalizedMessage());
			handler.sendEmptyMessage(Constants.HIDETEAUTOSEARCH);
			return;
		}*/
		Map<String,String> paraMap = new LinkedHashMap<String,String>();
		ParaUtil.putParaByString(cmd, paraMap);
		Device device = new Device();
		device.setDeviceName(deviceName);
		device.setDeviceID(paraMap.get("cam_id"));
		String eht_ip = paraMap.get("e_ip");
		device.setDeviceEthIp(eht_ip);
		device.setDeviceEthGateWay(paraMap.get("e_gw"));
		device.setDeviceEthMask(paraMap.get("e_mask"));
		device.setDeviceEthDNS1(paraMap.get("e_dns1"));
		device.setDeviceEthDNS2(paraMap.get("e_dns2"));
		
		device.setUnDefine1(ip);
		device.setUnDefine2(pwd);
		device.setDeviceNetType(true);
		device.setDeviceRemoteCmdPort(port1);
		device.setDeviceRemoteVideoPort(port2);
		device.setDeviceRemoteAudioPort(port3);
		Message msg = handler.obtainMessage();
		Bundle bundle = new Bundle();
		bundle.putSerializable("NEW_DEVICE", device);
		msg.setData(bundle);
		msg.what = Constants.SHOWRESULTDIALOG;
		handler.sendMessage(msg);
	}

	public void queryDeiceConfigByIp() {
		String cmd = null;
		try {
			cmd = PackageUtil.CMDPackage(CamCmdListHelper.GetCmd_Config + deviceTmp.getUnDefine2() + "\0", deviceTmp.getDeviceEthIp(), deviceTmp.getDeviceLocalCmdPort());
			if(cmd == null) {
				return;
			}
			//System.out.println("cmd=" + cmd);
		} catch (Exception e) {
			Log.d(TAG, e.getLocalizedMessage());
			handler.sendEmptyMessage(Constants.HIDETEAUTOSEARCH);
			return;
		}
		System.out.println("queryDeiceConfigByIp config =" + cmd);
		if("PSWD_FAIL".equals(cmd)) {
			Message msg = handler.obtainMessage();
			msg.arg1 = R.string.device_manager_pwd_set_err;
			msg.what = Constants.SHOWTOASTMSG;
			handler.sendMessage(msg);
		} else {
			Map<String,String> paraMap = new LinkedHashMap<String,String>();
			ParaUtil.putParaByString(cmd, paraMap);
			deviceTmp.setDeviceName(deviceName);
			deviceTmp.setDeviceID(paraMap.get("cam_id"));
			deviceTmp.setDeviceEthDNS1(paraMap.get("e_dns1"));
			deviceTmp.setDeviceEthDNS2(paraMap.get("e_dns2"));
			Bundle bundle2 = new Bundle();
			Message msg = handler.obtainMessage();
			bundle2.putSerializable("NEW_DEVICE", deviceTmp);
			msg.setData(bundle2);
			msg.what = Constants.SHOWRESULTDIALOG;
			handler.sendMessage(msg);
		}
	}
	

	private Runnable monitorSocketTask = new Runnable() {
		@Override
		public void run() {
			//UdtTools.close();
			device = camManager.getSelectDevice();
			int result = UdtTools.monitorSocket(device.getDeviceID());
			System.out.println("result = " + result);
			Log.d(TAG, "monitor result = " + result);
			if(result<0) {
				handler.removeMessages(Constants.WEB_CAM_SHOW_CHECK_PWD_DLG_MSG);
				handler.sendEmptyMessage(Constants.WEB_CAM_HIDE_CHECK_PWD_DLG_MSG);
			}
			analyseResult(result, device);
		}
	};
	
	private void analyseResult(int result, Device device) {
		switch (result) {
		case ErrorCode.STUN_ERR_INTERNAL:
			ToastUtils.showToast(this, R.string.webcam_error_code_internel);
			return;
		case ErrorCode.STUN_ERR_SERVER:
			ToastUtils.showToast(this, R.string.webcam_error_code_server_not_reached);
			return;
		case ErrorCode.STUN_ERR_TIMEOUT:
			ToastUtils.showToast(this, R.string.webcam_error_code_timeout);
			return;
		case ErrorCode.STUN_ERR_INVALIDID:
			ToastUtils.showToast(this, R.string.webcam_error_code_unlegal);
			return;
		case ErrorCode.STUN_ERR_CONNECT:
			ToastUtils.showToast(this, R.string.webcam_error_code_connect_error);
			return;
		case ErrorCode.STUN_ERR_BIND:
			ToastUtils.showToast(this, R.string.webcam_error_code_bind_error);
			return;
		default:
			break;
		}
		//mHandler.sendEmptyMessage(Constants.WEB_CAM_CONNECT_INIT_MSG);
		String random = RandomUtil.generalRandom();
		Log.d(TAG, "random = " + random);
		int initRes = UdtTools.initialSocket(random);
		if(initRes<0) {
			Log.d(TAG, "initialSocket init error!");
			ToastUtils.showToast(DeviceManager.this, R.string.webcam_connect_init_error);
		}else {
			// exec checkpwd runnable and show wait dialog
			handler.sendEmptyMessage(Constants.WEB_CAM_SHOW_CHECK_PWD_DLG_MSG);
			handler.removeCallbacks(checkPwdStateRunnable);
			handler.post(checkPwdStateRunnable);
		}
	}
	
	private Runnable checkPwdStateRunnable = new Runnable() {

		@Override
		public void run() {
			int resu = PackageUtil.checkPwdState();
			Log.d(TAG, "device manager checkPwdState result = " + resu);
			handler.removeMessages(Constants.WEB_CAM_SHOW_CHECK_PWD_DLG_MSG);
			handler.sendEmptyMessage(Constants.WEB_CAM_HIDE_CHECK_PWD_DLG_MSG);
			if(resu == 0) { // unset
				handler.sendEmptyMessage(Constants.SEND_SHOW_INPUT_TWO_PASS_DIALOG_SMG);
				//DialogUtils.inputTwoPasswordDialog(DeviceManager.this, device, handler, Constants.SEND_SHOW_TWO_PWD_FIELD_PREVIEW_MSG);
			} else if(resu == 1) {// pwd seted
				device = camManager.getSelectDevice();
				if(device.getUnDefine2() != null && device.getUnDefine2().length()>0) {
					Message mesg = handler.obtainMessage();
					mesg.obj  = device.getUnDefine2();
					mesg.what = Constants.WEB_CAM_CHECK_PWD_MSG;
					handler.sendMessage(mesg);
				}else {
					handler.sendEmptyMessage(Constants.SEND_SHOW_INPUT_ONE_PASS_DIALOG_SMG);
				}
			} else if(resu == 2){
				ToastUtils.showToast(DeviceManager.this, R.string.device_manager_pwd_set_err);
				//DialogUtils.inputOnePasswordDialog(DeviceManager.this, handler, Constants.SEND_SHOW_TWO_PWD_FIELD_PREVIEW_MSG);
			} else {
				ToastUtils.showToast(DeviceManager.this, R.string.device_manager_time_out_or_device_off_line);
			}
		}
	};
	
	private Runnable checkPwdRunnable = new Runnable() {

		@Override
		public void run() {
			int checkPwd = PackageUtil.checkPwd(password);
			Log.d(TAG, "checkPwd result = " + checkPwd);
			handler.removeMessages(Constants.WEB_CAM_SHOW_CHECK_PWD_DLG_MSG);
			handler.sendEmptyMessage(Constants.WEB_CAM_HIDE_CHECK_PWD_DLG_MSG);
			if(checkPwd == 1) {
				device.setUnDefine2(password);
				camManager.updateCam(device);
				FileUtil.persistentDevice(DeviceManager.this,camManager.getCamList());
				Intent intent = new Intent(WebCamActions.QUERY_CONFIG_ACTION);
				intent.setType(WebCamActions.QUERY_CONFIG_MINITYPE);
				startActivity(intent);
			} else if(checkPwd == -1) {
				ToastUtils.showToast(DeviceManager.this, R.string.device_manager_pwd_set_err);
				//DialogUtils.inputOnePasswordDialog(DeviceManager.this, handler, Constants.SEND_SHOW_TWO_PWD_FIELD_PREVIEW_MSG);
				handler.sendEmptyMessage(Constants.SEND_SHOW_INPUT_ONE_PASS_DIALOG_SMG);
			} else {
				ToastUtils.showToast(DeviceManager.this, R.string.device_manager_time_out_or_device_off_line);
			}
		}
	};
	
	private void showProgressDlg(int textId) {
		if(m_Dialog == null) {
			m_Dialog = new ProgressDialog(DeviceManager.this);
			m_Dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
			m_Dialog.setCancelable(false);
			m_Dialog.setMessage(getResources().getText(textId));
		}
		m_Dialog.show();
	}
	
	private void hideProgressDlg() {
		if(m_Dialog != null && m_Dialog.isShowing()) {
			m_Dialog.hide();
		}
	}

}
