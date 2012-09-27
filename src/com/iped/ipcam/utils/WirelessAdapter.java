package com.iped.ipcam.utils;

import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.iped.ipcam.gui.R;
import com.iped.ipcam.pojo.WifiConfig;

public class WirelessAdapter extends BaseAdapter {

	private List<WifiConfig> wirelessList;
	
	private LayoutInflater flater = null;
	
	public WirelessAdapter(List<WifiConfig> wirelessList, Context context) {
		this.wirelessList = wirelessList;
		flater = LayoutInflater.from(context);
	}
	
	@Override
	public int getCount() {
		return wirelessList.size();
	}

	@Override
	public WifiConfig getItem(int position) {
		return wirelessList.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder view = new ViewHolder();
		WifiConfig config = wirelessList.get(position);
		if(convertView == null) {
			convertView = flater.inflate(R.layout.wireless_list_item, null);
			view.ssid = (TextView) convertView.findViewById(R.id.wireless_ssid);
			view.signal_level = (TextView) convertView.findViewById(R.id.wireless_signal_level);
			view.proto = (TextView) convertView.findViewById(R.id.wireless_proto);
		} else {
			view = (ViewHolder)convertView.getTag();
		}
		view.ssid.setText(config.getSsid());
		view.signal_level.setText(config.getSignal_level());
		view.proto.setText(config.getProto());
		convertView.setTag(view);
		return convertView;
	}
	
	
	static class ViewHolder {
		
		public TextView ssid;

		public TextView signal_level;
		
		public TextView proto;
		
	}

}
