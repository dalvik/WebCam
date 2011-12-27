package com.iped.ipcam.gui;

import android.content.Context;
import android.graphics.Color;
import android.os.AsyncTask;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;

public class ControlPanel extends LinearLayout implements OnClickListener{

	private LayoutParams lp = null;
	
	/** Handle�Ŀ�ȣ���Panel�ȸ� */
	private final static int HANDLE_WIDTH = 30;
	
	/** ÿ���Զ�չ��/�����ķ�Χ */
	private final static int MOVE_WIDTH = 20;
	
	private int rightMargin = 0;
	
	private Button buttonHandle = null;
	
	private PanelOpenedEvent panelOpenedEvent = null;
	
	private PanelCloseEvent panelCloseEvent = null;
	
	private LinearLayout panelContainer;
	
	public ControlPanel(Context context, View videoView, int width, int height) {
		super(context);
		// �ı�ControlPanel�������������
		LayoutParams layoutParams = (LayoutParams) videoView.getLayoutParams();
		layoutParams.weight = 1; // ֧�ּ�ѹ
		videoView.setLayoutParams(layoutParams);
		
		// ����ControlPanel���������
		lp = new LayoutParams(width, height);
		lp.rightMargin = -lp.width + HANDLE_WIDTH;
		rightMargin = Math.abs(lp.rightMargin);
		this.setLayoutParams(lp);
		this.setOrientation(LinearLayout.HORIZONTAL);
		
		buttonHandle = new Button(context);
		buttonHandle.setText("c");
		buttonHandle.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.FILL_PARENT));
		buttonHandle.setOnClickListener(this);
		addView(buttonHandle);
		panelContainer = new LinearLayout(context);
		panelContainer.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
		addView(panelContainer);
		
	}

	@Override
	public void onClick(View v) {
		LayoutParams lp = (LayoutParams)ControlPanel.this.getLayoutParams();
		System.out.println(lp.width + " " + lp.height);
		if(lp.rightMargin<0) {// close status
			new AsynMove().execute(new Integer[]{ MOVE_WIDTH });
		} else { //open status
			new AsynMove().execute(new Integer[]{ -MOVE_WIDTH });
		}
	}
	
	public void setPanelOpenedEvent(PanelOpenedEvent panelOpenedEvent) {
		this.panelOpenedEvent = panelOpenedEvent;
	}

	public void setPanelCloseEvent(PanelCloseEvent panelCloseEvent) {
		this.panelCloseEvent = panelCloseEvent;
	}
	
	public void fillPanelContainer(View v) {
		panelContainer.addView(v);
		System.out.println(v.getWidth() +  " -- " + v.getHeight());
	}

	public void updateControlView(int width) {
		buttonHandle.measure(0, 0);
		lp.width = width + buttonHandle.getMeasuredWidth();
		lp.rightMargin = -lp.width + HANDLE_WIDTH;
		System.out.println(lp.width + "  ~");
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
				lp.rightMargin = Math.min(lp.rightMargin + values[0], 0);
			}
			if(lp.rightMargin == 0 && panelOpenedEvent != null) {
				panelOpenedEvent.onPanelOpened(ControlPanel.this);
			} else if(lp.rightMargin == (-rightMargin) && panelCloseEvent != null) {
				panelCloseEvent.onPanelClosed(ControlPanel.this);
			}
			ControlPanel.this.setLayoutParams(lp);
		}
		
	}
}
