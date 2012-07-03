package com.iped.ipcam.gui;

import android.content.Context;
import android.os.AsyncTask;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;

public class ControlPanel extends LinearLayout implements OnClickListener{

	private LayoutParams lp = null;
	
	/** Handle�Ŀ�ȣ���Panel�ȸ� */
	public final static int HANDLE_WIDTH = 30;
	
	/** ÿ���Զ�չ��/�����ķ�Χ */
	private final static int MOVE_WIDTH = 10;
	
	private int nRightMargin = 0;
	
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
		nRightMargin = Math.abs(lp.rightMargin);
		this.setLayoutParams(lp);
		this.setOrientation(LinearLayout.HORIZONTAL);
		
		buttonHandle = new Button(context);
		buttonHandle.setText("<");
		LayoutParams textParams = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.FILL_PARENT);
		buttonHandle.setLayoutParams(textParams);
		buttonHandle.setGravity(Gravity.CENTER_VERTICAL|Gravity.CENTER);
		buttonHandle.setOnClickListener(this);
		this.addView(buttonHandle);
		panelContainer = new LinearLayout(context);
		panelContainer.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
		this.addView(panelContainer);
		
	}

	@Override
	public void onClick(View v) {
		LayoutParams lp = (LayoutParams)ControlPanel.this.getLayoutParams();
		if(lp.rightMargin<0) {// close status
			new AsynMove().execute(new Integer[]{ MOVE_WIDTH });
			buttonHandle.setText(">");
		} else { //open status
			new AsynMove().execute(new Integer[]{ -MOVE_WIDTH });
			buttonHandle.setText("<");
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
	}

	public void updateControlView(int width) {
		buttonHandle.measure(0, 0);
		lp.width = width + buttonHandle.getMeasuredWidth();
		lp.rightMargin = -lp.width + HANDLE_WIDTH;
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
			if(nRightMargin % Math.abs(params[0]) == 0) {
				times = nRightMargin / Math.abs(params[0]);
			} else {
				times = nRightMargin / Math.abs(params[0]) + 1;
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
				lp.rightMargin = Math.max(lp.rightMargin + values[0], (-nRightMargin));
			} else {
				lp.rightMargin = Math.min(lp.rightMargin + values[0], 0);
			}
			if(lp.rightMargin == 0 && panelOpenedEvent != null) {
				panelOpenedEvent.onPanelOpened(ControlPanel.this);
			} else if(lp.rightMargin == (-nRightMargin) && panelCloseEvent != null) {
				panelCloseEvent.onPanelClosed(ControlPanel.this);
			}
			ControlPanel.this.setLayoutParams(lp);
		}
		
	}
}
