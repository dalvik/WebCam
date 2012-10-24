package com.iped.ipcam.utils;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.iped.ipcam.gui.R;

public class VideoPopupMenuAdaptor extends BaseAdapter {

	private int[] imageId;
	
	private LayoutInflater inflater;
	
	public VideoPopupMenuAdaptor(Context context, int[] imageId) {
		this.imageId = imageId;
		inflater = LayoutInflater.from(context);
	}
	
	@Override
	public int getCount() {
		return imageId.length;
	}

	@Override
	public Integer getItem(int position) {
		return imageId[position];
	}

	@Override
	public long getItemId(int position) {
		return imageId[position];
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder holder = null;
		int id = imageId[position];
		holder = new ViewHolder();
        if (convertView == null) {
        	convertView = inflater.inflate(R.layout.video_popwindow_item_layout, null);
			holder.imageView = (TextView) convertView.findViewById(R.id.video_popup_menu_item_name);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        //holder.imageView.setBackgroundResource(id);
        holder.imageView.setText(id);
        convertView.setTag(holder);
        return convertView;
	}

	static class ViewHolder {
		//ImageView imageView;
		TextView imageView;
	}
}
