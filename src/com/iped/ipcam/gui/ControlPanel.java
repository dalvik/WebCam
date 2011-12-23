package com.iped.ipcam.gui;

import android.content.Context;
import android.graphics.Color;
import android.os.AsyncTask;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;

public class ControlPanel extends LinearLayout implements OnClickListener{

	private Context context;
	
	/** Handle的宽度，与Panel等高 */
	private final static int HANDLE_WIDTH = 70;
	/** 每次自动展开/收缩的范围 */
	private final static int MOVE_WIDTH =120;
	
	private int rightMargin = 0;
	
	private Button buttonHandle = null;
	
	private PanelOpenedEvent panelOpenedEvent = null;
	
	private PanelCloseEvent panelCloseEvent = null;
	
	public ControlPanel(Context context, View videoView, int width, int height) {
		super(context);
		this.context = context;
		// 改变ControlPanel附近组件的属性
		LayoutParams layoutParams = (LayoutParams) videoView.getLayoutParams();
		layoutParams.weight = 1; // 支持挤压
		videoView.setLayoutParams(layoutParams);
		
		// 设置ControlPanel本身的属性
		LayoutParams lp = new LayoutParams(width, height);
		lp.rightMargin = -lp.width + HANDLE_WIDTH;
		rightMargin = Math.abs(lp.rightMargin);
		this.setLayoutParams(lp);
		setOrientation(LinearLayout.HORIZONTAL);
		
		buttonHandle = new Button(context);
		buttonHandle.setText("click");
		buttonHandle.setLayoutParams(new LayoutParams(HANDLE_WIDTH,height));
		buttonHandle.setOnClickListener(this);
		addView(buttonHandle);
	}

	@Override
	public void onClick(View v) {
		LayoutParams lp = (LayoutParams)ControlPanel.this.getLayoutParams();
		if(lp.rightMargin<0) {// close status
			new AsynMove().execute(new Integer[]{ MOVE_WIDTH });
			System.out.println(" + " +lp.rightMargin + " move =" + MOVE_WIDTH);
		} else { // open status
			new AsynMove().execute(new Integer[]{ -MOVE_WIDTH });
			System.out.println(lp.rightMargin + "---" + " move=" + (-MOVE_WIDTH));
		}
	}
	
	public void setPanelOpenedEvent(PanelOpenedEvent panelOpenedEvent) {
		this.panelOpenedEvent = panelOpenedEvent;
	}

	public void setPanelCloseEvent(PanelCloseEvent panelCloseEvent) {
		this.panelCloseEvent = panelCloseEvent;
	}

	public interface PanelCloseEvent {
		void onPanelClosed(View panel);
	}
	
	public interface PanelOpenedEvent {
		void onPanelOpened(View panel);
	}
	
	class AsynMove extends AsyncTask<Integer, Integer, Void> {

		@Override
		protected Void doInBackground(Integer... params) {
			int times;
			if(rightMargin % Math.abs(params[0]) == 0) {
				times = rightMargin / Math.abs(params[0]);
			} else {
				times = rightMargin / Math.abs(params[0]) + 1;
			}
			System.out.println("doInBackground" + params[0] + "-----=" + times);
			for(int i = 0; i < times; i++) {
				publishProgress(params);
				try {
					Thread.sleep(Math.abs(params[0]));
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			return null;
		}

		@Override
		protected void onProgressUpdate(Integer... values) {
			super.onProgressUpdate(values);
			LayoutParams lp = (LayoutParams) ControlPanel.this.getLayoutParams();
			if(values[0] < 0) {
				lp.rightMargin = Math.max(lp.rightMargin + values[0], (-rightMargin));
			} else {
				lp.rightMargin = Math.max(lp.rightMargin + values[0], 0);
			}
			if(lp.rightMargin == 0 && panelOpenedEvent != null) {
				panelOpenedEvent.onPanelOpened(ControlPanel.this);
			} else if(lp.rightMargin == (-rightMargin) && panelCloseEvent != null) {
				panelCloseEvent.onPanelClosed(ControlPanel.this);
			}
			ControlPanel.this.setLayoutParams(lp);
			System.out.println("onProgressUpdate=" + values);
		}
		
	}
}
