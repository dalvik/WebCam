package com.iped.ipcam.gui;

import java.util.ArrayList;
import java.util.List;

import android.app.ListActivity;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;

import com.iped.ipcam.pojo.Video;
import com.iped.ipcam.utils.VideoAdapter;

public class PlayBack extends ListActivity{

	private List<Video> videoList = null;

	private VideoAdapter videoAdapter = null;
	
	private final int DOWNLOAD = Menu.FIRST;
	
	private final int DELETE = Menu.FIRST + 1;
	
	private final int PLAYBACK = Menu.FIRST + 2;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.play_back);
		videoList = new ArrayList<Video>();
		Video video = new Video("vodeoName", "2011-12-27 13:32:21","2012-01-27 13:32:21");
		videoList.add(video);
		videoAdapter = new VideoAdapter(videoList, this);
		getListView().setAdapter(videoAdapter);
		registerForContextMenu(getListView());
	}
	
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		AdapterView.AdapterContextMenuInfo info = null;
		try {
			info = (AdapterView.AdapterContextMenuInfo) menuInfo;
		} catch (Exception e) {
			return ;
		}
		Video video = videoList.get(info.position);
		menu.setHeaderTitle(video.getVideoName());
		menu.add(0, DOWNLOAD, 1, getResources().getString(R.string.video_download));
		menu.add(0, DELETE, 2, getResources().getString(R.string.video_delete));
		menu.add(0, PLAYBACK, 3, getResources().getString(R.string.video_playback));
	}
	
	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		switch (item.getItemId()) {
		case DOWNLOAD:
			
			break;
		case DELETE:
			
			break;
		case PLAYBACK:
			
			break;
		default:
			break;
		}
		return super.onMenuItemSelected(featureId, item);
	}
}
