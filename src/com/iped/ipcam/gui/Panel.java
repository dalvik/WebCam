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

	/** Handle�Ŀ�ȣ���Panel�ȸ� */
	private final static int HANDLE_WIDTH = 30;
	/** ÿ���Զ�չ��/�����ķ�Χ */
	private final static int MOVE_WIDTH = 20;
	private Button btnHandle;
	private LinearLayout panelContainer;
	
	private int mRightMargin = 0;
	
	private Context mContext;
	
	private PanelClosedEvent panelClosedEvent = null;
	
	private PanelOpenedEvent panelOpenedEvent = null;

	/**
	 * otherView�Զ���������ӦPanelչ��/�����Ŀռ�仯
	 * 
	 * @author GV
	 * 
	 */
	public Panel(Context context, View otherView, int width, int height) {
		super(context);
		this.mContext = context;

		// �ı�Panel�������������
		LayoutParams otherLP = (LayoutParams) otherView.getLayoutParams();
		otherLP.weight = 1;// ֧��ѹ��
		otherView.setLayoutParams(otherLP);

		// ����Panel���������
		LayoutParams lp = new LayoutParams(width, height);
		lp.rightMargin = -lp.width + HANDLE_WIDTH;// Panel��Container����Ļ����������Handle�ڿ�������
		mRightMargin = Math.abs(lp.rightMargin);
		this.setLayoutParams(lp);
		this.setOrientation(LinearLayout.HORIZONTAL);

		// ����Handle������
		btnHandle = new Button(context);
		btnHandle.setLayoutParams(new LayoutParams(HANDLE_WIDTH, height));
		btnHandle.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View arg0) {
				LayoutParams lp = (LayoutParams) Panel.this.getLayoutParams();
				if (lp.rightMargin < 0)// CLOSE��״̬
					new AsynMove().execute(new Integer[] { MOVE_WIDTH });// ����չ��
				else if (lp.rightMargin >= 0)// OPEN��״̬
					new AsynMove().execute(new Integer[] { -MOVE_WIDTH });// ��������
			}

		});
		// btnHandle.setOnTouchListener(HandleTouchEvent);
		this.addView(btnHandle);

		// ����Container������
		panelContainer = new LinearLayout(context);
		panelContainer.setLayoutParams(new LayoutParams(
				LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
		this.addView(panelContainer);
	}

	/**
	 * ��������ʱ�Ļص�����
	 * 
	 * @param event
	 */
	public void setPanelClosedEvent(PanelClosedEvent event) {
		this.panelClosedEvent = event;
	}

	/**
	 * ����չ��ʱ�Ļص�����
	 * 
	 * @param event
	 */
	public void setPanelOpenedEvent(PanelOpenedEvent event) {
		this.panelOpenedEvent = event;
	}

	/**
	 * ��View����Panel��Container
	 * 
	 * @param v
	 */
	public void fillPanelContainer(View v) {
		panelContainer.addView(v);
	}

	/**
	 * �첽�ƶ�Panel
	 */
	class AsynMove extends AsyncTask<Integer, Integer, Void> {

		@Override
		protected Void doInBackground(Integer... params) {
			int times;
			if (mRightMargin % Math.abs(params[0]) == 0)// ����
				times = mRightMargin / Math.abs(params[0]);
			else
				// ������
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

			if (lp.rightMargin == 0 && panelOpenedEvent != null) {// չ��֮��
				panelOpenedEvent.onPanelOpened(Panel.this);// ����OPEN�ص�����
			} else if (lp.rightMargin == -(mRightMargin) && panelClosedEvent != null) {// ����֮��
				panelClosedEvent.onPanelClosed(Panel.this);// ����CLOSE�ص�����
			}
			Panel.this.setLayoutParams(lp);
		}
	}

}
