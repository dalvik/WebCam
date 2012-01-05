package com.iped.ipcam.gui;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TimePicker;

import com.iped.ipcam.engine.CamMagFactory;
import com.iped.ipcam.engine.IVideoManager;
import com.iped.ipcam.pojo.Video;
import com.iped.ipcam.utils.Constants;
import com.iped.ipcam.utils.VideoAdapter;

public class PlayBack extends ListActivity implements OnClickListener {

	private IVideoManager videoManager = null;
	
	private List<Video> videoList = null;

	private VideoAdapter videoAdapter = null;
	
	private final int DOWNLOAD = Menu.FIRST;
	
	private final int DELETE = Menu.FIRST + 1;
	
	private final int PLAYBACK = Menu.FIRST + 2;
	
	private Button videoSearch = null;
	
	private Button clearAll = null;
	
	private Button startSearchDate = null;
	
	private Button startSearchTime = null;
	
	private Button endSearchDate = null;
	
	private Button endSearchTime = null;
	 
	private View myDialogView = null;
	
	private AlertDialog dlg = null;
	
	private Calendar calendar = null;

	private ProgressDialog videoSearchProgressDialog = null;
	
	private String TAG = "PlayBack";
	
	
	private Handler handler = new Handler() {
		@Override
		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
			case Constants.VIDEOAUTOSEARCH:
				
				break;
			case Constants.UPDATEVIDEOLIST:
				videoAdapter.notifyDataSetChanged();
				break;
			case Constants.DISSMISVIDEOSEARCHDLG:
				hideProgress();
				break;
			default:
				break;
			}
		};
	};
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.play_back);
		videoManager = CamMagFactory.getVideoManagerInstance();
		videoList = videoManager.getVideoList();
		//Video video = new Video(1,12, "vodeoName", "2011-12-27 13:32:21","2012-01-27 13:32:21");
		//videoList.add(video);
		videoAdapter = new VideoAdapter(videoManager.getVideoList(), this);
		getListView().setAdapter(videoAdapter);
		registerForContextMenu(getListView());
		videoSearch = (Button) findViewById(R.id.play_back_video_search);
		clearAll = (Button) findViewById(R.id.play_back_clear_all);
		videoSearch.setOnClickListener(this);
		clearAll.setOnClickListener(this);
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
	
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.play_back_video_search:
			vodeoSearchDia();
			break;
		case R.id.play_back_clear_all:
			clearVideoList();
			break;
		case R.id.start_date_buttion:
			setDateStr(startSearchDate);
			break;
		case R.id.start_time_buttion:
			setTimeStr(startSearchTime);
			break;
		case R.id.end_date_buttion:
			setDateStr(endSearchDate);
			break;
		case R.id.end_time_buttion:
			setTimeStr(endSearchTime);
			break;
		default:
			break;
		}
	}
	
	private void vodeoSearchDia() {
        LayoutInflater factory = LayoutInflater.from(PlayBack.this);
        myDialogView = factory.inflate(R.layout.play_back_video_search_dlg, null);
        initSearchDlg();
        dlg = new AlertDialog.Builder(PlayBack.this).setTitle(getResources().getString(R.string.play_back_auto_search_video_str))
        .setView(myDialogView)//
        .setPositiveButton(getResources().getString(R.string.play_back_auto_search_button_str), //
        new DialogInterface.OnClickListener() {//
            public void onClick(DialogInterface dialog, int whichButton) {
            	showProgress();
            	videoManager.startSearchThread(handler);
            }
        }).setNegativeButton(getResources().getString(R.string.play_back_auto_cancle_button_str), null
       /* new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
            	 try {
             	    Field field  =  dlg.getClass().getSuperclass().getDeclaredField("mShowing");
             	    field.setAccessible( true );
             	    field.set(dialog, true);
             	    dialog.dismiss();
             	} catch  (Exception e) {
             		Log.v(TAG, e.getMessage());
             	}
            }
        }*/
        )
        .create();
        dlg.show();
	}
	
	private void showProgress() {
		hideProgress();
		videoSearchProgressDialog = new ProgressDialog(PlayBack.this);
		videoSearchProgressDialog.setTitle(getResources().getString(R.string.auto_search_tips_str));
		videoSearchProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		videoSearchProgressDialog.show();
	}
	
	private void hideProgress() {
		if(videoSearchProgressDialog != null) {
			videoSearchProgressDialog.dismiss();
			videoSearchProgressDialog = null;
		}
	}
	
	private void initSearchDlg() {
		calendar = Calendar.getInstance();
		EditText videoSearchName = (EditText) myDialogView.findViewById(R.id.play_back_video_search_name);
        EditText vodeoSearchAddr = (EditText) myDialogView.findViewById(R.id.play_back_video_search_addr);
        videoSearchName.setText("192.16.1.121");
        vodeoSearchAddr.setText("192.16.1.121");
        startSearchDate = (Button) myDialogView.findViewById(R.id.start_date_buttion);
        startSearchTime = (Button) myDialogView.findViewById(R.id.start_time_buttion);
        endSearchDate = (Button) myDialogView.findViewById(R.id.end_date_buttion);
        endSearchTime = (Button) myDialogView.findViewById(R.id.end_time_buttion);
        startSearchDate.setText(initDateStr());
        String date = initDateStr();
        String time = initTimeStr();
        System.out.println(date + "  " + time);
        startSearchDate.setText(date);
        startSearchTime.setText(time);
        
        endSearchDate.setText(date);
        endSearchTime.setText(time);
        startSearchDate.setOnClickListener(this);
        startSearchTime.setOnClickListener(this);
        endSearchDate.setOnClickListener(this);
        endSearchTime.setOnClickListener(this);
	}
	
	private String initDateStr() {
		return  format(calendar.get(Calendar.YEAR)) + "-" + format(calendar.get(Calendar.MONTH) + 1) + "-" + format(calendar.get(Calendar.DAY_OF_MONTH));
	}
	
	private String initTimeStr() {
		return format(calendar.get(Calendar.HOUR_OF_DAY)) + ":" + format(calendar.get(Calendar.MINUTE));
	}

	private void setDateStr(final Button button) {
		new DatePickerDialog(this, new DatePickerDialog.OnDateSetListener() {
			
			@Override
			public void onDateSet(DatePicker view, int year, int monthOfYear,
					int dayOfMonth) {
				String time = format(year) + "-" + format(monthOfYear + 1) + "-" + format(dayOfMonth);
				button.setText(time);
			}
		}, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
	}
	
	private void setTimeStr(final Button button) {
		new TimePickerDialog(this,new TimePickerDialog.OnTimeSetListener() {
			public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
				String time = format(hourOfDay) + ":" + format(minute);
				button.setText(time);
			}
		},calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE),true).show();
	}
	
	private String format(int x) {
		String s = "" + x;
		if(s.length() == 1) {
			s = "0" + s;
		}
		return s;
	}
	
	
	private void clearVideoList() {
		videoList.clear();
		videoAdapter.notifyDataSetChanged();
	}
}
