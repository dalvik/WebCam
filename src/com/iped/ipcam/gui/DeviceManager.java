package com.iped.ipcam.gui;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.LinkedHashMap;
import java.util.Map;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
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
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.iped.ipcam.engine.CamMagFactory;
import com.iped.ipcam.engine.ICamManager;
import com.iped.ipcam.exception.CamManagerException;
import com.iped.ipcam.pojo.Device;
import com.iped.ipcam.utils.CamCmdListHelper;
import com.iped.ipcam.utils.Constants;
import com.iped.ipcam.utils.DeviceAdapter;
import com.iped.ipcam.utils.FileUtil;
import com.iped.ipcam.utils.PackageUtil;
import com.iped.ipcam.utils.ParaUtil;
import com.iped.ipcam.utils.ThroughNetUtil;

public class DeviceManager extends ListActivity implements OnClickListener {

	private ListView listView = null;

	private DeviceAdapter adapter = null;

	private final int MENU_EDIT = Menu.FIRST;

	private final int MENU_DEL = Menu.FIRST + 1;

	private final int MENU_PREVIEW = Menu.FIRST + 2;

	private Button autoSearchButton = null;

	private Button manulAddButton = null;

	private Button claerCamButton = null;

	private ICamManager camManager = null;

	private ProgressDialog progressDialog = null;

	private ProgressDialog queryNewCameraDialog = null;
	
	private AlertDialog dlg = null;

	private int lastSelected = 0;

	private Thread throughNetThread = null;
	
	private ThroughNetUtil netUtil = null;
	
	private static final String TAG = "DeviceManager";

	private Handler handler = new Handler() {
		@Override
		public void handleMessage(Message message) {
			switch (message.what) {
			case Constants.UPDATEAUTOSEARCH:
				int value = message.arg1;
				progressDialog.setProgress(message.arg1);
				if (value >= 100) {
					hideProgress();
				}
				break;
			case Constants.HIDETEAUTOSEARCH:
				hideProgress();
				break;
			case Constants.UPDATEDEVICELIST:
				//FileUtil.persistentDevice(DeviceManager.this,camManager.getCamList());
				adapter.notifyDataSetChanged();
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
					String ip = bd.getString("IPADDRESS");
					int port1 = bd.getInt("PORT1");
					int port2 = bd.getInt("PORT2");
					int port3 = bd.getInt("PORT3");
					hideProgress();
					getDeviceConfig(ip, port1, port2, port3);
				}
				break;
			case Constants.SENDGETUNFULLPACKAGEMSG:
				showToast(R.string.device_manager_find_device_id_error);
				handler.sendEmptyMessage(Constants.HIDETEAUTOSEARCH);
				break;
			case Constants.SENDGETTHREEPORTTIMOUTMSG:
				handler.sendEmptyMessage(Constants.HIDETEAUTOSEARCH);
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
		manulAddButton = (Button) findViewById(R.id.manul_add_button);
		claerCamButton = (Button) findViewById(R.id.clear_all_button);
		camManager = CamMagFactory.getCamManagerInstance();
		adapter = new DeviceAdapter(camManager.getCamList(), this);
		camManager.getCamList().addAll(FileUtil.fetchDeviceFromFile(this));
		//adapter = new DeviceAdapter(, this);
		listView = getListView();
		listView.setAdapter(adapter);
		listView.setOnItemClickListener(itemClickListener);
		registerForContextMenu(getListView());
		autoSearchButton.setOnClickListener(this);
		manulAddButton.setOnClickListener(this);
		claerCamButton.setOnClickListener(this);
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
		menu.add(0, MENU_EDIT, 1, getString(R.string.device_edit_str));
		menu.add(0, MENU_DEL, 2, getString(R.string.device_del_str));
		menu.add(0, MENU_PREVIEW, 3, getString(R.string.device_preview_str));
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		AdapterContextMenuInfo infor = (AdapterContextMenuInfo) item
				.getMenuInfo();
		Device device = camManager.getDevice(infor.position);
		switch (item.getItemId()) {
		case MENU_EDIT:
			editDevice(device);
			break;

		case MENU_DEL:
			camManager.delCam(infor.position);
			handler.sendEmptyMessage(Constants.UPDATEDEVICELIST);
			break;
		case MENU_PREVIEW:
			WebTabWidget.tabHost.setCurrentTabByTag(Constants.VIDEOPREVIEW);
			Intent intent = new Intent();
			Bundle bundle = new Bundle();
			bundle.putSerializable("IPPLAY", device);
			intent.putExtras(bundle);
			intent.setAction(Constants.ACTION_IPPLAY);
			sendBroadcast(intent);
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
		case R.id.clear_all_button:
			camManager.clearCamList();
			handler.sendEmptyMessage(Constants.UPDATEDEVICELIST);
		default:
			break;
		}
	}

	public void test() {
		byte[] tem = CamCmdListHelper.QueryCmd_Online.getBytes();
		DatagramSocket datagramSocket = null;
		try {
			datagramSocket = new DatagramSocket();
			datagramSocket.setSoTimeout(Constants.DEVICESEARCHTIMEOUT);
			DatagramPacket datagramPacket = new DatagramPacket(tem, tem.length,
					InetAddress.getByName("192.168.1." + 141),
					Constants.UDPPORT);
			datagramSocket.send(datagramPacket);
			DatagramPacket rece = new DatagramPacket(tem, tem.length);
			datagramSocket.receive(rece);
			Log.d(TAG, "receive inof   9090909090 ");
		} catch (SocketException e) {
			Log.d(TAG, "CamManagerImp isOnline : " + e.getLocalizedMessage());
		} catch (UnknownHostException e) {
			Log.d(TAG, "CamManagerImp isOnline : " + e.getLocalizedMessage());
		} catch (IOException e) {
			Log.d(TAG, "CamManagerImp isOnline : " + e.getLocalizedMessage());
		} finally {
			if (datagramSocket != null) {
				datagramSocket.close();
				datagramSocket = null;
			}
		}
	}

	private void showProgress() {
		progressDialog = new ProgressDialog(DeviceManager.this);
		progressDialog.setTitle(getResources().getString(
				R.string.auto_search_tips_str));
		progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		progressDialog.setCancelable(false);
		progressDialog.setProgress(1);
		progressDialog.show();
	}

	public void hideProgress() {
		if (progressDialog != null && progressDialog.isShowing()) {
			progressDialog.dismiss();
		}
		
		if(queryNewCameraDialog != null){
			queryNewCameraDialog.dismiss();
		}
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			hideProgress();
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

	private void editDevice(final Device device) {
		final View addDeviceView = initAddNewDeviceView();
		final EditText newDeviceNameEditText = (EditText) addDeviceView
				.findViewById(R.id.device_manager_add_name_id);
		newDeviceNameEditText.setText(device.getDeviceName());
		final EditText newDeviceIPEditText = (EditText) addDeviceView
				.findViewById(R.id.device_manager_new_addr_id);
		//newDeviceIPEditText.setText(device.getDeviceIp());
		final EditText newDeviceGatewayEditText = (EditText) addDeviceView
				.findViewById(R.id.device_manager_new_gateway_addr_id);
		//newDeviceGatewayEditText.setText(device.getDeviceGateWay());
		// final EditText newDeviceSubnetEditText =
		// (EditText)addDeviceView.findViewById(R.id.device_manager_add_name_id);
		// newDeviceSubnetEditText.setText(device.get());
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
								String newDiviceName = newDeviceIPEditText
										.getText().toString();
								String newDiviceIP = newDeviceIPEditText
										.getText().toString();
								if (newDiviceIP == null
										|| newDiviceIP.length() <= 0) {
									unCloseDialog(
											dlg,
											R.string.device_manager_add_not_null_str,
											false);
								} else {
									String newDiviceGateway = newDeviceGatewayEditText
											.getText().toString();
									Device deviceNew = new Device(
											newDiviceName, "IP Camera");
									if (camManager.editCam(device, deviceNew)) {
										handler.sendEmptyMessage(Constants.UPDATEDEVICELIST);
										unCloseDialog(dlg, -1, true);
									} else {
										unCloseDialog(
												dlg,
												R.string.device_manager_add_same_ip_str,
												false);
									}
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

	@Override
	protected void onDestroy() {
		FileUtil.persistentDevice(this,camManager.getCamList());
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

	private void updateComponentState(View addDeviceView, boolean isChecked) {
		addDeviceView.findViewById(R.id.device_manager_new_device_id_edittext).setEnabled(isChecked);
		addDeviceView.findViewById(R.id.device_manager_new_addr_id).setEnabled(
				!isChecked);
		addDeviceView.findViewById(R.id.device_manager_new_gateway_addr_id)
				.setEnabled(!isChecked);
		addDeviceView.findViewById(R.id.device_manager_new_dns1_id)
				.setEnabled(!isChecked);
		addDeviceView.findViewById(R.id.device_manager_new_dns2_id)
				.setEnabled(!isChecked);
		addDeviceView.findViewById(R.id.device_manager_new_sub_net_addr_id)
				.setEnabled(!isChecked);
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
				if(throughNetThread != null) {
					try {
						throughNetThread.join(100);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				netUtil = new ThroughNetUtil(handler,true,Integer.parseInt(ip,16));
				throughNetThread = new Thread(netUtil);
				throughNetThread.start();
			} else {//根据IP地址添加设备
				DatagramSocket datagramSocket = null;
				byte [] tem = CamCmdListHelper.GetCmd_Config.getBytes();
				byte [] buffTemp = new byte[Constants.COMMNICATEBUFFERSIZE];
				StringBuffer sb = new StringBuffer();
				String tmp = null;
				try {
					datagramSocket = new DatagramSocket();
					datagramSocket.setSoTimeout(Constants.VIDEOSEARCHTIMEOUT);
					DatagramPacket datagramPacket = new DatagramPacket(tem, tem.length, InetAddress.getByName(ip), Constants.UDPPORT);
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
					Device device = new Device(paraMap.get("name"), paraMap.get("cam_id"));
					
					device.setDeviceNetType(false);
					device.setDeviceWlanIp(ip);
					device.setDeviceWlanGateWay(gateWay);
					device.setDeviceWlanMask(mask);
					device.setDeviceWlanDNS1(dns1);
					device.setDeviceWlanDNS2(dns2);
					
					device.setDeviceRemoteCmdPort(Constants.UDPPORT);
					device.setDeviceRemoteVideoPort(Constants.TCPPORT);
					device.setDeviceRemoteAudioPort(Constants.AUDIOPORT);
					Bundle bundle = new Bundle();
					Message msg = handler.obtainMessage();
					bundle.putSerializable("NEW_DEVICE", device);
					msg.setData(bundle);
					msg.what = Constants.SHOWRESULTDIALOG;
					handler.sendMessage(msg);
				} catch (IOException e) {
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
				}
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
	
	public void getDeviceConfig(String ip, int port1, int port2, int port3) {
		DatagramSocket socket = netUtil.getPort1();
		if(socket == null) {
			return;
		}
		String cmd = null;
		try {
			cmd = PackageUtil.CMDPackage2(netUtil, CamCmdListHelper.GetCmd_Config, ip, port1);
		} catch (CamManagerException e) {
			Log.d(TAG, e.getLocalizedMessage());
			handler.sendEmptyMessage(Constants.HIDETEAUTOSEARCH);
			return;
		}
		Map<String,String> paraMap = new LinkedHashMap<String,String>();
		ParaUtil.putParaByString(cmd, paraMap);
		Device device = new Device(paraMap.get("name"), paraMap.get("cam_id"));
		
		device.setDeviceEthIp(paraMap.get("inet_eth_ip"));
		device.setDeviceEthGateWay(paraMap.get("inet_eth_gateway"));
		device.setDeviceEthMask(paraMap.get("inet_eth_mask"));
		device.setDeviceEthDNS1(paraMap.get("inet_eth_dns1"));
		device.setDeviceEthDNS2(paraMap.get("inet_eth_dns2"));
		
		device.setDeviceNetType(true);
		
		device.setDeviceWlanIp(paraMap.get("inet_wlan_ip"));
		device.setDeviceWlanGateWay(paraMap.get("inet_wlan_gateway"));
		device.setDeviceWlanMask(paraMap.get("inet_wlan_mask"));
		device.setDeviceWlanDNS1(paraMap.get("inet_wlan_dns1"));
		device.setDeviceWlanDNS2(paraMap.get("inet_wlan_dns2"));
		device.setUnDefine1(ip);
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
}
