package com.iped.ipcam.gui;

import java.io.File;
import java.util.List;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

import com.baidu.mobstat.StatService;
import com.iped.ipcam.utils.FilePathAdapter;
import com.iped.ipcam.utils.FileUtil;

public class DirPreview extends ListActivity implements OnClickListener {

	private ListView listView = null;
	
	private List<String> filePathList = null;
	
	private FilePathAdapter adapter = null;
	
	private String rootPath = FileUtil.getDefaultPath();
	
	private String current = rootPath;
	
	private TextView titlePath = null;
	
	private Button backButton = null;
	
	private Button okButton = null;
	
	private Button cancleButton = null;
	
	private Button choceButton = null;
	
	private boolean flag = true;

	private OnItemClickListener itemClickListener = new OnItemClickListener() {

		@Override
		public void onItemClick(AdapterView<?> arg0, View arg1, int index,
				long arg3) {
			String temPath = adapter.getItem(index);
			viewFiles(temPath);
			listView.requestFocusFromTouch();
			listView.setSelection(index);
		}
	};
	
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.books_list);
		titlePath = (TextView) findViewById(R.id.path_title);
		backButton = (Button) findViewById(R.id.dir_preview_back_button);
		okButton = (Button) findViewById(R.id.system_settings_save_path_preview_sure_id);
		cancleButton = (Button) findViewById(R.id.system_settings_save_path_preview_cancle_id);
		choceButton = (Button) findViewById(android.R.id.empty);
		backButton.setOnClickListener(this);
		okButton.setOnClickListener(this);
		cancleButton.setOnClickListener(this);
		choceButton.setOnClickListener(this);
		titlePath.setText(rootPath);
		Intent intent = getIntent();
		if(intent != null) {
			flag = intent.getBooleanExtra("DIRPREVIEW", true);
		} else {
			flag = true;
		}
		filePathList = FileUtil.getFiles(DirPreview.this, rootPath, flag);
		adapter = new FilePathAdapter(this, filePathList);
		listView = getListView();
		listView.setAdapter(adapter);
		listView.setOnItemClickListener(itemClickListener);
	}
	
	public void viewFiles(String filePath) {
		List<String> temp = FileUtil.getFiles(DirPreview.this, filePath, flag);
		if(temp != null) {
			current = filePath;
			titlePath.setText(current);
			filePathList.clear();
			filePathList.addAll(temp);
			adapter.notifyDataSetChanged();
		}
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if(keyCode == KeyEvent.KEYCODE_BACK) {
			cancle();
		}
		return super.onKeyDown(keyCode, event);
	}
	
	@Override
	public void onClick(View view) {
		switch (view.getId()) {
		case R.id.dir_preview_back_button:
			File file = new File(current);
			String parentPath = file.getParent();
			if(parentPath != null) {
				viewFiles(parentPath);
			}
			break;
		case R.id.system_settings_save_path_preview_sure_id:
			ok();
			break;
		case R.id.system_settings_save_path_preview_cancle_id:
			cancle();
			break;
		case android.R.id.empty:
			ok();
			break;
		default:
			break;
		}
	}

	private void ok() {
		Intent intent = new Intent();
		if(flag) {
			intent.putExtra("DIRFILEPATH", titlePath.getText().toString());
			setResult(100, intent);
		} else {
			String path = titlePath.getText().toString();
			File file = new File(path);
			if(!file.isFile()) {
				return;
			}
			intent.putExtra("BINFILEPATH", titlePath.getText().toString());
			setResult(200, intent);
		}
		DirPreview.this.finish();
	}
	
	private void cancle() {
		setResult(RESULT_CANCELED, new Intent());
		DirPreview.this.finish();
	}

	@Override
	protected void onResume() {
		super.onResume();
		StatService.onResume(this);
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		StatService.onPause(this);
	}
}
