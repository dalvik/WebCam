package com.iped.ipcam.gui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff.Mode;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.widget.ProgressBar;

public class BCVControlProgressBar extends ProgressBar {

	private String text;
	
	private Paint paint;
	
	private String tips = "";
	
	private Rect rect = new Rect();
	
	public BCVControlProgressBar(Context context) {
		super(context);
		this.paint = new  Paint();
	}

	public BCVControlProgressBar(Context context, AttributeSet attrs) {
		super(context, attrs);
		this.paint = new  Paint();
	}
	
	public void init(String tips) {
		this.tips = tips;
	}
	
	
	@Override
	public synchronized void setProgress(int progress) {
		setText(progress);
		super.setProgress(progress);
	}
	
	private void setText(int progress){ 
        int i = (progress * 100)/this.getMax(); 
        this.text = tips + String.valueOf(i) + "%"; 
    } 

	@Override
	protected synchronized void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		this.paint.getTextBounds(this.text, 0, this.text.length(), rect);
		int x = getWidth() /2 - rect.centerX();
		int y = getHeight() /2 - rect.centerY();
		canvas.drawColor(getResources().getColor(R.color.web_cam_video_view_progress_bg), Mode.SRC_OVER);
		canvas.drawText(this.text, x, y, paint);
	}
	
}
