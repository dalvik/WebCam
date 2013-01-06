package com.iped.ipcam.view;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;

public class TouchViewPager extends ViewPager {

	public TouchViewPager(Context context) {
		super(context);
	}
	
	public TouchViewPager(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	

	@Override
	public boolean onTouchEvent(MotionEvent arg0) {
		// TODO Auto-generated method stub
		return super.onTouchEvent(arg0);
	}
	
}
