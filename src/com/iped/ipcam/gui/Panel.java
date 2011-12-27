package com.iped.ipcam.gui;

import android.content.Context;
import android.os.AsyncTask;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

public class Panel extends LinearLayout {

	public interface PanelClosedEvent {
		void onPanelClosed(View panel);
	}

	public interface PanelOpenedEvent {
		void onPanelOpened(View panel);
	}

	/** Handle的宽度，与Panel等高 */
	private final static int HANDLE_WIDTH = 30;
	/** 每次自动展开/收缩的范围 */
	private final static int MOVE_WIDTH = 20;
	private Button btnHandle;
	private LinearLayout panelContainer;
	
	private int mRightMargin = 0;
	
	private Context mContext;
	
	private PanelClosedEvent panelClosedEvent = null;
	
	private PanelOpenedEvent panelOpenedEvent = null;

	/**
	 * otherView自动布局以适应Panel展开/收缩的空间变化
	 * 
	 * @author GV
	 * 
	 */
	public Panel(Context context, View otherView, int width, int height) {
		super(context);
		this.mContext = context;

		// 改变Panel附近组件的属性
		LayoutParams otherLP = (LayoutParams) otherView.getLayoutParams();
		otherLP.weight = 1;// 支持压挤
		otherView.setLayoutParams(otherLP);

		// 设置Panel本身的属性
		LayoutParams lp = new LayoutParams(width, height);
		lp.rightMargin = -lp.width + HANDLE_WIDTH;// Panel的Container在屏幕不可视区域，Handle在可视区域
		mRightMargin = Math.abs(lp.rightMargin);
		this.setLayoutParams(lp);
		this.setOrientation(LinearLayout.HORIZONTAL);

		// 设置Handle的属性
		btnHandle = new Button(context);
		btnHandle.setLayoutParams(new LayoutParams(HANDLE_WIDTH, height));
		btnHandle.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				LayoutParams lp = (LayoutParams) Panel.this.getLayoutParams();
				if (lp.rightMargin < 0)// CLOSE的状态
					new AsynMove().execute(new Integer[] { MOVE_WIDTH });// 正数展开
				else if (lp.rightMargin >= 0)// OPEN的状态
					new AsynMove().execute(new Integer[] { -MOVE_WIDTH });// 负数收缩
			}

		});
		// btnHandle.setOnTouchListener(HandleTouchEvent);
		this.addView(btnHandle);

		// 设置Container的属性
		panelContainer = new LinearLayout(context);
		panelContainer.setLayoutParams(new LayoutParams(
				LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
		this.addView(panelContainer);
	}

	/**
	 * 定义收缩时的回调函数
	 * 
	 * @param event
	 */
	public void setPanelClosedEvent(PanelClosedEvent event) {
		this.panelClosedEvent = event;
	}

	/**
	 * 定义展开时的回调函数
	 * 
	 * @param event
	 */
	public void setPanelOpenedEvent(PanelOpenedEvent event) {
		this.panelOpenedEvent = event;
	}

	/**
	 * 把View放在Panel的Container
	 * 
	 * @param v
	 */
	public void fillPanelContainer(View v) {
		panelContainer.addView(v);
	}

	/**
	 * 异步移动Panel
	 */
	class AsynMove extends AsyncTask<Integer, Integer, Void> {

		@Override
		protected Void doInBackground(Integer... params) {
			int times;
			if (mRightMargin % Math.abs(params[0]) == 0)// 整除
				times = mRightMargin / Math.abs(params[0]);
			else
				// 有余数
				times = mRightMargin / Math.abs(params[0]) + 1;
			for (int i = 0; i < times; i++) {
				publishProgress(params);
				try {
					Thread.sleep(Math.abs(params[0]));
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			return null;
		}

		@Override
		protected void onProgressUpdate(Integer... params) {
			LayoutParams lp = (LayoutParams) Panel.this.getLayoutParams();
			if (params[0] < 0)
				lp.rightMargin = Math.max(lp.rightMargin + params[0],
						(-mRightMargin));
			else
				lp.rightMargin = Math.min(lp.rightMargin + params[0], 0);

			if (lp.rightMargin == 0 && panelOpenedEvent != null) {// 展开之后
				panelOpenedEvent.onPanelOpened(Panel.this);// 调用OPEN回调函数
			} else if (lp.rightMargin == -(mRightMargin) && panelClosedEvent != null) {// 收缩之后
				panelClosedEvent.onPanelClosed(Panel.this);// 调用CLOSE回调函数
			}
			Panel.this.setLayoutParams(lp);
		}
	}

}
