package com.iped.ipcam.gui;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.widget.ProgressBar;

public class BCVControlProgressBar extends ProgressBar {

	private String text;
	
	private Paint paint;
	
	public BCVControlProgressBar(Context context) {
		super(context);
		init();
	}

	public BCVControlProgressBar(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}
	
	private void init() {
		this.paint = new  Paint(Color.WHITE);
	}
	
	
	@Override
	public synchronized void setProgress(int progress) {
		setText(progress);
		super.setProgress(progress);
	}
	
	private void setText(int progress){ 
        int i = (progress * 100)/this.getMax(); 
        this.text = String.valueOf(i) + "%"; 
    } 

}
