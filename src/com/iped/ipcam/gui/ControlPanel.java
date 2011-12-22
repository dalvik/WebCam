package com.iped.ipcam.gui;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

public class ControlPanel extends LinearLayout {

	private Context context;
	
	/** Handle�Ŀ�ȣ���Panel�ȸ� */
	private final static int HANDLE_WIDTH = 70;
	/** ÿ���Զ�չ��/�����ķ�Χ */
	private final static int MOVE_WIDTH =120;
	
	private int rightMargin = 0;
	
	private Button buttonHandle = null;
	
	public ControlPanel(Context context, View videoView, int width, int height) {
		super(context);
		this.context = context;
		// �ı�ControlPanel�������������
		LayoutParams layoutParams = (LayoutParams) videoView.getLayoutParams();
		layoutParams.weight = 1; // ֧�ּ�ѹ
		videoView.setLayoutParams(layoutParams);
		// ����ControlPanel���������
		LayoutParams lp = new LayoutParams(width, height);
		lp.rightMargin = -lp.width + HANDLE_WIDTH;
		rightMargin = Math.abs(lp.rightMargin);
		this.setLayoutParams(lp);
		setOrientation(LinearLayout.HORIZONTAL);
		
		buttonHandle = new Button(context);
		buttonHandle.setText("click");
		buttonHandle.setTextColor(Color.WHITE);
		buttonHandle.setLayoutParams(new LayoutParams(HANDLE_WIDTH,height));
		addView(buttonHandle);
	}

	


}
