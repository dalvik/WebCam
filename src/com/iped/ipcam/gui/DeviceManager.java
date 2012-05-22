package com.iped.ipcam.gui;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

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
import com.iped.ipcam.pojo.Device;
import com.iped.ipcam.utils.ByteUtil;
import com.iped.ipcam.utils.CamCmdListHelper;
import com.iped.ipcam.utils.Common;
import com.iped.ipcam.utils.Constants;
import com.iped.ipcam.utils.DeviceAdapter;
import com.iped.ipcam.utils.ThroughNetUtil.SendUDTCommon;

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
					String name = bundle.getString("CAMERANAME");
					String ip = bundle.getString("CAMERAIP");
					String getway = bundle.getString("CAMERAGETWAY");
					showResult(name, ip, getway);
				}
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
			intent.putExtra("IPPLAY", device.getDeviceIp());
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
		newDeviceIPEditText.setText(device.getDeviceIp());
		final EditText newDeviceGatewayEditText = (EditText) addDeviceView
				.findViewById(R.id.device_manager_new_gateway_addr_id);
		newDeviceGatewayEditText.setText(device.getDeviceGateWay());
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
											newDiviceName, "IP Camera",
											newDiviceIP, Constants.TCPPORT,
											Constants.UDPPORT, newDiviceGateway);
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
									if (cameraId == null
											|| cameraId.length() <= 0) {
										unCloseDialog(
												dlg,
												R.string.device_manager_new_device_id_not_null,
												false);
										return;
									}
									if(!cameraId.matches("\\d")) {
										unCloseDialog(
												dlg,
												R.string.device_manager_new_device_id_not_number,
												false);
										return;
									}
									queryNewCameraDialog = ProgressDialog.show(DeviceManager.this, "设备查询", "请稍候");
									new Thread(new QueryCamera(newDiviceName, cameraId, true))
											.start();
								} else {
									String newDiviceIP = ((EditText) addDeviceView
											.findViewById(R.id.device_manager_new_addr_id))
											.getText().toString();
									if (newDiviceIP == null
											|| newDiviceIP.length() <= 0) {
										unCloseDialog(
												dlg,
												R.string.device_manager_add_not_null_str,
												false);
										return;
									}
									queryNewCameraDialog = ProgressDialog.show(DeviceManager.this, "设备查询", "请稍候");
									new Thread(new QueryCamera(newDiviceName, newDiviceIP,
											false)).start();
								}
								/*
								 * String newDiviceName =
								 * ((EditText)addDeviceView
								 * .findViewById(R.id.device_manager_add_name_id
								 * )).getText().toString(); String newDiviceIP =
								 * ((EditText)addDeviceView.findViewById(R.id.
								 * device_manager_new_addr_id
								 * )).getText().toString(); if(newDiviceIP==
								 * null || newDiviceIP.length() <=0) {
								 * unCloseDialog(dlg,
								 * R.string.device_manager_add_not_null_str,
								 * false); } else { String newDiviceGateway =
								 * ((EditText)addDeviceView.findViewById(R.id.
								 * device_manager_new_gateway_addr_id
								 * )).getText().toString(); //String
								 * newDiviceSubNet =
								 * ((EditText)addDeviceView.findViewById
								 * (R.id.device_manager_new_sub_net_addr_id
								 * )).getText().toString(); Device device = new
								 * Device(newDiviceName, "IP Camera",
								 * newDiviceIP, Constants.TCPPORT,
								 * Constants.UDPPORT, newDiviceGateway);
								 * if(camManager.addCam(device)) {
								 * handler.sendEmptyMessage
								 * (Constants.UPDATEDEVICELIST);
								 * unCloseDialog(dlg, -1, true); } else {
								 * unCloseDialog(dlg,
								 * R.string.device_manager_add_same_ip_str,
								 * false); } }
								 */
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
		addDeviceView.findViewById(R.id.device_manager_new_device_id_edittext)
				.setEnabled(isChecked);
		addDeviceView.findViewById(R.id.device_manager_new_addr_id).setEnabled(
				!isChecked);
		addDeviceView.findViewById(R.id.device_manager_new_gateway_addr_id)
				.setEnabled(!isChecked);
		addDeviceView.findViewById(R.id.device_manager_new_tcp_port_id)
				.setEnabled(!isChecked);
		addDeviceView.findViewById(R.id.device_manager_new_udp_port_id)
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
				.findViewById(R.id.device_manager_new_tcp_port_id)).setText("");// Constants.TCPPORT+
		((EditText) addDeviceView
				.findViewById(R.id.device_manager_new_udp_port_id)).setText(""); // Constants.UDPPORT+
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

		private boolean flag;

		public QueryCamera(String name, String ip, boolean flag) {
			this.name = name;
			this.ip = ip;
			this.flag = flag;
		}

		@Override
		public void run() {
			if (flag) {
				// 1、构造UDP数据包 请求连接到指定ID的camera
				System.out.println(Byte.parseByte(ip));
				byte[] connCameraId = new byte[] { Byte.parseByte(ip) };// common
																		// id
				// 2、send data content
				byte[] sendDataContent = ByteUtil.intToBytes(1);
				// 3、send data length
				byte[] sendDataLength = ByteUtil
						.shortToBytes((short) sendDataContent.length);
				int l1 = connCameraId.length;
				int l2 = sendDataContent.length;
				int l3 = sendDataLength.length;
				byte[] sendData = new byte[l1 + l2 + l3];
				// copy commid to send data
				System.arraycopy(connCameraId, 0, sendData, 0, l1);
				// copy data length to send data
				System.arraycopy(sendDataLength, 0, sendData, l1, l3);
				// copy data content to send data
				System.arraycopy(sendDataContent, 0, sendData, l1 + l3, l2);
				DatagramPacket datagramPacket;
				DatagramSocket socket = null;
				try {
					socket = new DatagramSocket();
					socket.setSoTimeout(10000);
					datagramPacket = new DatagramPacket(sendData, l1 + l2 + l3,
							InetAddress.getByName(Common.SERVER_IP),
							Common.CLIENT_WATCH_PORT);
					socket.send(datagramPacket);
					byte[] buf = new byte[64];
					DatagramPacket rp = new DatagramPacket(buf, 64);
					socket.receive(rp);
					byte[] rece = rp.getData();
					if (rece[0] == SendUDTCommon.NET_CAMERA_OK.ordinal()) {
						//Toast.makeText(DeviceManager.this, "query success",Toast.LENGTH_LONG).show();
						Bundle bundle = new Bundle();
						bundle.putString("CAMERANAME", name);
						bundle.putString("CAMERAIP", ip);
						bundle.putString("CAMERAGETWAY", "192.168.1.1");
						Message msg = handler.obtainMessage();
						msg.setData(bundle);
						msg.what = Constants.SHOWRESULTDIALOG;
						handler.sendMessage(msg);
					}else {
						//Toast.makeText(DeviceManager.this, "query error",Toast.LENGTH_LONG).show();
						System.out.println("--*******--");
						Message msg = handler.obtainMessage();
						msg.what = Constants.SHOWTOASTMSG;
						msg.arg1 = R.string.device_manager_find_device_id_error;
						handler.sendMessage(msg);
					}
				} catch (IOException e) {
					//Toast.makeText(DeviceManager.this, "query error",Toast.LENGTH_LONG).show();
					Message msg = handler.obtainMessage();
					msg.what = Constants.SHOWTOASTMSG;
					msg.arg1 = R.string.device_manager_find_device_id_error;
				    handler.sendMessage(msg);
					Log.d(TAG,
							"ThroughNetUtil request connect camera by id receive data error! "
									+ e.getLocalizedMessage());
				} finally {
					socket.close();
					handler.sendEmptyMessage(Constants.HIDETEAUTOSEARCH);
				}
			} else {
				DatagramSocket datagramSocket = null;
				byte [] tem = CamCmdListHelper.QueryCmd_Online.getBytes();
				byte [] buffTemp = new byte[Constants.COMMNICATEBUFFERSIZE];
				try {
					datagramSocket = new DatagramSocket();
					datagramSocket.setSoTimeout(10000);
					DatagramPacket datagramPacket = new DatagramPacket(tem, tem.length, InetAddress.getByName(ip), Constants.UDPPORT);
					datagramSocket.send(datagramPacket);
					DatagramPacket rece = new DatagramPacket(buffTemp, buffTemp.length);
					datagramSocket.receive(rece);
					String info = new String(buffTemp,0, rece.getLength());
					Log.d(TAG, "Receive inof //////////////" + info);
					Bundle bundle = new Bundle();
					bundle.putString("CAMERANAME", name);
					bundle.putString("CAMERAIP", ip);
					bundle.putString("CAMERAGETWAY", "192.168.1.1");
					Message msg = handler.obtainMessage();
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
	
	private void showResult(final String newDiviceName, final String newDiviceIP, final String newDiviceGateway) {
		ad = new AlertDialog.Builder(DeviceManager.this)
		.setTitle(getString(R.string.device_manager_new_device_title))
		.setMessage(getString(R.string.device_manager_new_device_message))
		.setPositiveButton(getString(R.string.system_settings_save_path_preview_sure_str), new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				Device device = new Device(newDiviceName, "IP Camera",newDiviceIP, Constants.TCPPORT, Constants.UDPPORT, newDiviceGateway);
				if(camManager.addCam(device)) {
					handler.sendEmptyMessage(Constants.UPDATEDEVICELIST);
				}
				unCloseDialog(dlg, -1, true); 
			}
		}).setNegativeButton(getString(R.string.system_settings_save_path_preview_cancle_str), new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				ad.dismiss();
			}
		}).create();
		ad.show();
	}
}
