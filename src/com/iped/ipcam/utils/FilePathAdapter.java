package com.iped.ipcam.utils;

import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.iped.ipcam.gui.R;

public class FilePathAdapter extends BaseAdapter {

	private LayoutInflater _inflater;
	
	private List<String> filePath;

	public FilePathAdapter(Context context, List<String> filePath) {
		this.filePath = filePath;
		_inflater = LayoutInflater.from(context);
	}

	@Override
	public int getCount() {
		return filePath.size();
	}

	@Override
	public String getItem(int position) {
		return filePath.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder holder = null;
		holder = new ViewHolder();
		if (convertView == null) { 
			convertView = _inflater.inflate(R.layout.dir_preview_list_item, null);
			holder.name = (TextView) convertView.findViewById(R.id.file_path);
			convertView.setTag(holder);
		} else {
			holder = (ViewHolder) convertView.getTag();
		}
		// 更新View信息
		holder.name.setText(filePath.get(position));
		return convertView;
	}

	private static class ViewHolder {
		TextView name;
	}
}
