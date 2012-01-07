package com.iped.ipcam.utils;

import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.iped.ipcam.gui.R;
import com.iped.ipcam.pojo.Video;

public class VideoAdapter extends BaseAdapter {

	private Context context;
	private List<Video> videoList = null;
	
	private LayoutInflater inflater = null;
	
	public VideoAdapter(List<Video> deviceList, Context context) {
		this.videoList = deviceList;
		this.context = context;
		inflater = LayoutInflater.from(context);
	}
	
	@Override
	public int getCount() {
		if(null == videoList) {
			return 0;
		}
		return videoList.size();
	}

	@Override
	public Video getItem(int position) {
		return videoList.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder viewHolder = new ViewHolder();
		Video video = getItem(position);
		if(convertView == null) {
			convertView = inflater.inflate(R.layout.play_back_video_list_item, null);
			viewHolder.name = (TextView) convertView.findViewById(R.id.video_preview_type);
			viewHolder.start = (TextView) convertView.findViewById(R.id.video_preview_start_time);
			viewHolder.end = (TextView) convertView.findViewById(R.id.video_preview_end_time);
			viewHolder.addr = (TextView) convertView.findViewById(R.id.play_back_video_address);
			viewHolder.size = (TextView) convertView.findViewById(R.id.play_back_video_size);
		} else {
			viewHolder = (ViewHolder) convertView.getTag();
		}
		viewHolder.name.setText(context.getResources().getText(R.string.video_preview_name_str) + video.getVideoName());
		viewHolder.start.setText(context.getResources().getText(R.string.video_preview_start_time_str) + video.getVideoStartTime());
		viewHolder.end.setText(context.getResources().getText(R.string.video_preview_end_time_str) + video.getVideoEndTime());
		viewHolder.addr.setText(context.getResources().getText(R.string.video_preview_addr_str) + video.getAddress());
		viewHolder.size.setText(context.getResources().getText(R.string.video_preview_length_str) + "" + video.getFileLength()/1000 + " kb");
		convertView.setTag(viewHolder);
		return convertView;
	}

	static class ViewHolder {
		
		public TextView name;

		public TextView start;
		
		public TextView end;
		
		public TextView addr;
		
		public TextView size;
		
			
	}
}
