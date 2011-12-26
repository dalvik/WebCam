package com.iped.ipcam.utils;

import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.iped.ipcam.gui.R;
import com.iped.ipcam.pojo.Device;

public class DeviceAdapter extends BaseAdapter {

	private List<Device> deviceList = null;
	
	private LayoutInflater inflater = null;
	
	public DeviceAdapter(List<Device> deviceList, Context context) {
		this.deviceList = deviceList;
		inflater = LayoutInflater.from(context);
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
			viewHolder.name = (TextView) convertView.findViewById(R.id.device_name);
			viewHolder.type = (TextView) convertView.findViewById(R.id.device_name);
			viewHolder.ip = (TextView) convertView.findViewById(R.id.device_ip_address);
			viewHolder.tcp = (TextView) convertView.findViewById(R.id.device_tcp_port);
			viewHolder.udp = (TextView) convertView.findViewById(R.id.device_udp_port);
			viewHolder.gateWay = (TextView) convertView.findViewById(R.id.device_gateway);
		} else {
			viewHolder = (ViewHolder) convertView.getTag();
		}
		viewHolder.name.setText(device.getDeviceName());
		viewHolder.type.setText(device.getDeviceType());
		viewHolder.ip.setText(device.getDeviceIp());
		viewHolder.tcp.setText(device.getDeviceTcpPort());
		viewHolder.udp.setText(device.getDeviceUdpPort());
		viewHolder.gateWay.setText(device.getDeviceGateWay());
		convertView.setTag(viewHolder);
		return convertView;
	}

	static class ViewHolder {
		
		public TextView name;

		public TextView type;
		
		public TextView ip;
		
		public TextView tcp;
		
		public TextView udp;
		
		public TextView gateWay;
			
	}
}
