package com.iped.ipcam.gui;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;

public class PopupActivity extends Dialog {

	
	private int count = 1;
	
	private Handler handler = new Handler() {
		public void handleMessage(android.os.Message msg) {
			PopupActivity.this.dismiss();
		};
	};
	
	public PopupActivity(Context context, int theme) {
		super(context, theme);
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.tips_pop_up_layout);
		new Thread(new Runnable() {
			@Override
			public void run() {
				while(true) {
					if(count++/16 == 0) {
						try {
							Thread.sleep(1500);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
						handler.sendEmptyMessage(1);
						break;
					}
				}
			}
		}).start();
	}
}
