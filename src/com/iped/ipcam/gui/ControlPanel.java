package com.iped.ipcam.gui;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

public class ControlPanel extends LinearLayout {

	private Context context;
	
	/** Handle的宽度，与Panel等高 */
	private final static int HANDLE_WIDTH = 70;
	/** 每次自动展开/收缩的范围 */
	private final static int MOVE_WIDTH =120;
	
	private int rightMargin = 0;
	
	private Button buttonHandle = null;
	
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
		buttonHandle.setTextColor(Color.WHITE);
		buttonHandle.setLayoutParams(new LayoutParams(HANDLE_WIDTH,height));
		addView(buttonHandle);
	}

	


}
