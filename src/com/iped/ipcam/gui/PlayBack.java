package com.iped.ipcam.gui;

import java.lang.reflect.Field;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.ListActivity;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TimePicker;
import android.widget.Toast;

import com.iped.ipcam.engine.CamMagFactory;
import com.iped.ipcam.engine.ICamManager;
import com.iped.ipcam.engine.IVideoManager;
import com.iped.ipcam.pojo.Device;
import com.iped.ipcam.pojo.Video;
import com.iped.ipcam.utils.Constants;
import com.iped.ipcam.utils.DateUtil;
import com.iped.ipcam.utils.ProgressUtil;
import com.iped.ipcam.utils.ToastUtils;
import com.iped.ipcam.utils.VideoAdapter;
import com.iped.ipcam.utils.WebCamActions;

public class PlayBack extends ListActivity implements OnClickListener {

	private IVideoManager videoManager = null;
	
	private ICamManager camManager = null;
	
	private List<Video> videoList = null;

	private VideoAdapter videoAdapter = null;
	
	private final int PLAYBACK = Menu.FIRST;
	
	private final int DELETE = Menu.FIRST + 1;
	
	private final int DOWNLOAD = Menu.FIRST + 2;
	
	private Button videoSearch = null;
	
	private Button clearAll = null;
	
	private Button startSearchDate = null;
	
	private Button startSearchTime = null;
	
	private Button endSearchDate = null;
	
	private Button endSearchTime = null;
	 
	private View myDialogView = null;
	
	private AlertDialog dlg = null;
	
	private Calendar startCalendar = null;

	private Calendar endCalendar = null;
	
	//private ProgressDialog videoSearchProgressDialog = null;
	
	private int selectIndex = 0;
	
	private int selectIndexDevcie = 0;
	
	private int searchDviceIndexCurrent = 0;
	
	private String TAG = "PlayBack";
	
	private Handler handler = new Handler() {
		@Override
		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
			case Constants.VIDEOAUTOSEARCH:
				//vodeoSearchDia();
				break;
			case Constants.UPDATEVIDEOLIST:
				videoAdapter.notifyDataSetChanged();
				break;
			case Constants.DISSMISVIDEOSEARCHDLG:
				int strId = msg.arg1;
				if(strId > 0) {
					ToastUtils.showToast(PlayBack.this, strId);
				}
				ProgressUtil.hideProgress();
				break;
			case Constants.DELETEFILES:
				ProgressUtil.showProgress(R.string.video_delete_ing, PlayBack.this);
				break;
			case Constants.DELETEFILESUCCESS:
				ProgressUtil.hideProgress();
				videoAdapter.notifyDataSetChanged();
				Toast.makeText(PlayBack.this, getResources().getString(R.string.video_delete_success), Toast.LENGTH_SHORT).show();
				break;
			case Constants.DELETEFILEERROR:
				Toast.makeText(PlayBack.this, getResources().getString(R.string.video_delete_error), Toast.LENGTH_SHORT).show();
				ProgressUtil.hideProgress();
				break;
			case Constants.CLEARFILES:
				//Toast.makeText(PlayBack.this, getResources().getString(R.string.video_delete_success), Toast.LENGTH_SHORT).show();
				//hideProgress();
				ProgressUtil.showProgress(R.string.video_clear_ing, PlayBack.this);
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
		camManager = CamMagFactory.getCamManagerInstance();
		videoList = videoManager.getVideoList();
		videoAdapter = new VideoAdapter(videoManager.getVideoList(), this);
		getListView().setAdapter(videoAdapter);
		getListView().setOnItemLongClickListener(listener);
		registerForContextMenu(getListView());
		videoSearch = (Button) findViewById(R.id.play_back_video_search);
		clearAll = (Button) findViewById(R.id.play_back_clear_all);
		videoSearch.setOnClickListener(this);
		clearAll.setOnClickListener(this);
	}
	
	private OnItemLongClickListener listener = new OnItemLongClickListener() {

		@Override
		public boolean onItemLongClick(AdapterView<?> arg0, View arg1,
				int index, long arg3) {
			selectIndex = index;
			return false;
		}
	};
	
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
		menu.add(0, PLAYBACK, 1, getResources().getString(R.string.video_playback));
		menu.add(0, DELETE, 2, getResources().getString(R.string.video_delete));
		menu.add(0, DOWNLOAD, 3, getResources().getString(R.string.video_download));
	}
	
	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		Video video = videoList.get(selectIndex);
		switch (item.getItemId()) {
		case DOWNLOAD:
			break;
		case DELETE:
			String index = video.getIndex();
			IVideoManager videoManager = CamMagFactory.getVideoManagerInstance();
			videoManager.deleteFiles(handler, index, index+" ", video.getAddress());
			handler.sendEmptyMessage(Constants.DELETEFILES);
			break;
		case PLAYBACK:
			WebTabWidget.tabHost.setCurrentTabByTag(Constants.VIDEOPREVIEW);
			String videoIndex = video.getIndex() + "00000000";
			Intent intent = new Intent();
			Bundle bundle = new Bundle();
			bundle.putString("PLVIDEOINDEX",videoIndex);
			String start = video.getVideoStartTime();
			String end = video.getVideoEndTime();
			long startTime = DateUtil.formatTimeToDate(start).getTime();
			if(end == null || end.trim().length()<=0) {
				bundle.putLong("TOTALTIME", 0);
			}else {
				bundle.putLong("TOTALTIME", DateUtil.formatTimeToDate(end).getTime() - startTime);
			}
			bundle.putLong("STARTTIME", startTime);
			//bundle.putSerializable("IPPLAY", camManager.getSelectDevice());
			bundle.putInt("PLAYBACKDEVICEINDEX", searchDviceIndexCurrent);
			intent.putExtras(bundle);
			intent.setAction(WebCamActions.ACTION_PLAY_BACK);
			sendBroadcast(intent);
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
			Device device = camManager.getDevice(selectIndexDevcie);//.getSelectDevice();
			if(device == null) {
				Toast.makeText(this, getResources().getString(R.string.play_back_select_device_first_str), Toast.LENGTH_SHORT).show();
			} else {
				Log.d(TAG, "### select device id = " + device.getDeviceID());
				vodeoSearchDia(device);
			}
			break;
		case R.id.play_back_clear_all:
			//clearVideoList();
			clearAlertDlg();
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
	
	private void vodeoSearchDia(final Device device) {
        LayoutInflater factory = LayoutInflater.from(PlayBack.this);
        myDialogView = factory.inflate(R.layout.play_back_video_search_dlg, null);
        initSearchDlg(device);
        dlg = new AlertDialog.Builder(PlayBack.this)
        .setTitle(getResources().getString(R.string.play_back_auto_search_video_str))
        .setView(myDialogView)//
        .setPositiveButton(getResources().getString(R.string.play_back_auto_search_button_str), //
        new DialogInterface.OnClickListener() {//
			public void onClick(DialogInterface dialog, int whichButton) {
            	Date startDate = DateUtil.formatTimeToDate2(getStartTime());
            	Date endDate = DateUtil.formatTimeToDate2(getEndTime());
            	//String s = getStartTime();
            	//String w = getEndTime();
            	 if(compareDate(startDate, endDate)) {
            		 searchDviceIndexCurrent = selectIndexDevcie;
            		 ProgressUtil.showProgress(R.string.auto_search_tips_str,PlayBack.this);
            		 videoList.clear();
            		 videoAdapter.notifyDataSetChanged();
            		 videoManager.videoSearchInit(camManager.getDevice(selectIndexDevcie), startDate, endDate);
            		 videoManager.startSearchThread(handler);
            		 try {
            			 Field field  =  dlg.getClass().getSuperclass().getDeclaredField("mShowing");
            			 field.setAccessible( true );
            			 field.set(dialog, true);
            			 dialog.dismiss();
            		 } catch  (Exception e) {
            			 Log.v(TAG, e.getMessage());
            		 }
            	 }else {
            		 Toast.makeText(PlayBack.this, getResources().getString(R.string.play_back_start_above_start_time_str), Toast.LENGTH_SHORT).show();
            		 try {
            			 Field field  =  dlg.getClass().getSuperclass().getDeclaredField("mShowing");
            			 field.setAccessible( true );
            			 field.set(dialog, false);
            			 dialog.dismiss();
            		 } catch  (Exception e) {
            			 Log.v(TAG, e.getMessage());
            		 }
            	 }
            }
        }).setNegativeButton(getResources().getString(R.string.play_back_auto_cancle_button_str), new DialogInterface.OnClickListener() {
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
        }
        )
        .create();
        dlg.show();
	}

	
	private void initSearchDlg(Device device) {
		startCalendar  = Calendar.getInstance();
		startCalendar.set(Calendar.YEAR, startCalendar.get(Calendar.YEAR)-1);
		endCalendar = Calendar.getInstance();
		EditText videoSearchName = (EditText) myDialogView.findViewById(R.id.play_back_video_search_name);
        //EditText vodeoSearchAddr = (EditText) myDialogView.findViewById(R.id.play_back_video_search_addr);
		Spinner searchSpinnerList = (Spinner) myDialogView.findViewById(R.id.search_device_id_list);
		final List<Device> list = camManager.getCamList();
		int l = list.size();
		CharSequence[] idArr = new CharSequence[l];
		for(int i=0;i<l;i++) {
			idArr[i] = list.get(i).getDeviceID();
		}
		final SharedPreferences settings = getSharedPreferences(WebCam.class.getName(), 0);
		selectIndexDevcie = settings.getInt("SEARCH_DEVICE_INDEX", 0);
		ArrayAdapter<CharSequence> adapter = new ArrayAdapter<CharSequence>(this, android.R.layout.simple_spinner_item, idArr);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		searchSpinnerList.setAdapter(adapter);
		if(selectIndexDevcie>l-1) {
			searchSpinnerList.setSelection(l-1);
		}else {
			searchSpinnerList.setSelection(selectIndexDevcie);
		}
        videoSearchName.setText(device.getDeviceName());
        videoSearchName.setEnabled(false);
        searchSpinnerList.setOnItemSelectedListener(new OnItemSelectedListener() {

			@Override
			public void onItemSelected(AdapterView<?> parent, View view,
					int position, long id) {
				selectIndexDevcie = position;
				settings.edit().putInt("SEARCH_DEVICE_INDEX", selectIndexDevcie).commit();
				Log.d(TAG, "### search select index = " + position + " device id = " + list.get(position).getDeviceID());
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
			}
        	
		});
        //vodeoSearchAddr.setText(device.getDeviceID());
        //vodeoSearchAddr.setEnabled(false);
        startSearchDate = (Button) myDialogView.findViewById(R.id.start_date_buttion);
        startSearchTime = (Button) myDialogView.findViewById(R.id.start_time_buttion);
        endSearchDate = (Button) myDialogView.findViewById(R.id.end_date_buttion);
        endSearchTime = (Button) myDialogView.findViewById(R.id.end_time_buttion);

        String time = initTimeStr();
        startSearchDate.setText(initStartDateStr());
        startSearchTime.setText(time);
        
        endSearchDate.setText(initEndDateStr());
        endSearchTime.setText(time);
        
        startSearchDate.setOnClickListener(this);
        startSearchTime.setOnClickListener(this);
        endSearchDate.setOnClickListener(this);
        endSearchTime.setOnClickListener(this);
	}
	

	private String initStartDateStr() {
		return  format(startCalendar.get(Calendar.YEAR)) + "-" + format(startCalendar.get(Calendar.MONTH) + 1) + "-" + format(startCalendar.get(Calendar.DAY_OF_MONTH));
	}
	
	private String initEndDateStr() {
		return  format(endCalendar.get(Calendar.YEAR)) + "-" + format(endCalendar.get(Calendar.MONTH) + 1) + "-" + format(endCalendar.get(Calendar.DAY_OF_MONTH));
	}
	
	private String initTimeStr() {
		return format(endCalendar.get(Calendar.HOUR_OF_DAY)) + ":" + format(endCalendar.get(Calendar.MINUTE));
	}

	private void setDateStr(final Button button) {
		new DatePickerDialog(this, new DatePickerDialog.OnDateSetListener() {
			
			@Override
			public void onDateSet(DatePicker view, int year, int monthOfYear,
					int dayOfMonth) {
				String time = format(year-1) + "-" + format(monthOfYear + 1) + "-" + format(dayOfMonth);
				button.setText(time);
			}
		}, endCalendar.get(Calendar.YEAR), endCalendar.get(Calendar.MONTH), endCalendar.get(Calendar.DAY_OF_MONTH)).show();
	}
	
	private void setTimeStr(final Button button) {
		new TimePickerDialog(this,new TimePickerDialog.OnTimeSetListener() {
			public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
				String time = format(hourOfDay) + ":" + format(minute);
				button.setText(time);
			}
		},endCalendar.get(Calendar.HOUR_OF_DAY), endCalendar.get(Calendar.MINUTE),true).show();
	}
	
	private String getStartTime() {
		return startSearchDate.getText() + " " +  startSearchTime.getText();
	}
	
	private String getEndTime() {
		return endSearchDate.getText() + " " +  endSearchTime.getText();
	}
	
	private boolean compareDate(Date start, Date end) {
		if(start.before(end)){
			return true;
		}
		return false;
	}
	
	private String format(int x) {
		String s = "" + x;
		if(s.length() == 1) {
			s = "0" + s;
		}
		return s;
	}
	
	

	private void clearAlertDlg() {
		final int count = videoAdapter.getCount();
		if(count<=0) {
			Toast.makeText(PlayBack.this, getResources().getString(R.string.play_back_auto_clear_no_files_str), Toast.LENGTH_SHORT).show();
			return;
		}
		new AlertDialog.Builder(PlayBack.this).setTitle(getResources().getString(R.string.play_back_clear_dlg_title_str))
		.setMessage(getResources().getString(R.string.play_back_clear_dlg_message_str))
		.setPositiveButton(getResources().getString(R.string.play_back_clear_dlg_sure_str), new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				IVideoManager videoManager = CamMagFactory.getVideoManagerInstance();
				Video videoStart = videoAdapter.getItem(0);
				Video videoEnd = videoAdapter.getItem(count-1);
				
				videoManager.deleteFiles(handler, videoStart.getIndex(), videoEnd.getIndex() + " ", videoStart.getAddress());
				Message msg = handler.obtainMessage();
				handler.sendMessage(msg);
				handler.sendEmptyMessage(Constants.CLEARFILES);
			}
		})
		.setNegativeButton(getResources().getString(R.string.play_back_auto_cancle_button_str), null).create().show();
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		ProgressUtil.dismissProgress();
	}
}
