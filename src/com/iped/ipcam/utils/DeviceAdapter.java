package com.iped.ipcam.utils;

import java.util.List;

import android.content.Context;
import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.RadioButton;
import android.widget.TextView;

import com.iped.ipcam.gui.R;
import com.iped.ipcam.pojo.Device;

public class DeviceAdapter extends BaseAdapter {

	private Context context;
	
	private Resources res;
	
	private List<Device> deviceList = null;
	
	private LayoutInflater inflater = null;
	
	private int checkedIndex;
	
	public DeviceAdapter(List<Device> deviceList, Context context) {
		this.deviceList = deviceList;
		this.context = context;
		inflater = LayoutInflater.from(context);
		res = context.getResources();
	}
	
	@Override
	public int getCount() {
		if(null == deviceList) {
			return 0;
		}
		return deviceList.size();
	}

	@Override
	public Device getItem(int position) {
		return deviceList.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder viewHolder = new ViewHolder();
		Device device = getItem(position);
		if(convertView == null) {
			convertView = inflater.inflate(R.layout.device_list_item, null);
			viewHolder.selectDevice = (RadioButton) convertView.findViewById(R.id.device_select_id);
			viewHolder.name = (TextView) convertView.findViewById(R.id.device_name);
			viewHolder.type = (TextView) convertView.findViewById(R.id.device_type);
			viewHolder.id = (TextView) convertView.findViewById(R.id.device_id);
			viewHolder.ip = (TextView) convertView.findViewById(R.id.device_ip_address);
			viewHolder.tcp = (TextView) convertView.findViewById(R.id.device_tcp_port);
			viewHolder.udp = (TextView) convertView.findViewById(R.id.device_udp_port);
			viewHolder.gateWay = (TextView) convertView.findViewById(R.id.device_gateway);
			viewHolder.cmd = (TextView) convertView.findViewById(R.id.device_audio_port);
		} else {
			viewHolder = (ViewHolder) convertView.getTag();
		}
		viewHolder.name.setText(device.getDeviceName());
		if(position == checkedIndex) {
			viewHolder.selectDevice.setChecked(true);
		}else {
			viewHolder.selectDevice.setChecked(false);
		}
		if(device.getDeviceNetType()) {
			viewHolder.type.setText(res.getText(R.string.device_manager_add_net_type_wlan_str));
			viewHolder.ip.setText(res.getText(R.string.device_ip_str) + device.getUnDefine1());
			viewHolder.gateWay.setText(res.getText(R.string.device_gateway_str) + device.getDeviceEthGateWay());
			viewHolder.id.setText("ID:" + device.getDeviceID());
			viewHolder.tcp.setText(res.getText(R.string.device_cmd_port_str) + "" + device.getDeviceRemoteCmdPort());
			viewHolder.udp.setText(res.getText(R.string.device_diveo_port_str) + "" + device.getDeviceRemoteVideoPort());
			viewHolder.cmd.setText(res.getText(R.string.device_audio_port_str) + "" + device.getDeviceRemoteAudioPort());
		} else {
			viewHolder.type.setText(res.getText(R.string.device_manager_add_net_type_eth_str));
			viewHolder.ip.setText(res.getText(R.string.device_ip_str) + device.getDeviceEthIp());
			viewHolder.gateWay.setText(res.getText(R.string.device_gateway_str) + device.getDeviceEthGateWay());
			viewHolder.id.setText("ID:" + device.getDeviceID());
			viewHolder.tcp.setText(res.getText(R.string.device_cmd_port_str) + "" + device.getDeviceLocalCmdPort());
			viewHolder.udp.setText(res.getText(R.string.device_diveo_port_str) + "" + device.getDeviceLocalVideoPort());
			viewHolder.cmd.setText(res.getText(R.string.device_audio_port_str) + "" + device.getDeviceLocalAudioPort());
		}
		convertView.setTag(viewHolder);
		return convertView;
	}
	
	public void setChecked(int checkedIndex) {
		this.checkedIndex = checkedIndex;
	}
	
	static class ViewHolder {
		
		public RadioButton selectDevice;
		
		public TextView name;

		public TextView type;
		
		public TextView ip;

		public TextView gateWay;
		
		public TextView id;
		
		public TextView tcp;
		
		public TextView udp;
		
		public TextView cmd;	
	}
}
