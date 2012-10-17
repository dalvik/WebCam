package com.iped.ipcam.gui;

import com.iped.ipcam.utils.DateUtil;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.widget.SeekBar;

public class MySeekBar extends SeekBar {

	private String text;

	private Paint paint;

	private long tips = 0;

	private Rect rect = new Rect();

	public MySeekBar(Context context) {
		super(context);
		this.paint = new Paint();
	}

	public MySeekBar(Context context, AttributeSet attrs) {
		super(context, attrs);
		this.paint = new Paint();
	}

	public void init(long tips) {
		this.tips = tips;
		paint.setColor(Color.WHITE);
	}

	@Override
	public synchronized void setProgress(int progress) {
		setText(progress);
		super.setProgress(progress);
	}

	private void setText(int progress) {
		//int i = (progress * 100) / this.getMax();
		this.text = DateUtil.formatTimeToDate6(progress*1000+tips);//tips + String.valueOf(i) + "%";
	}

	@Override
	protected synchronized void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		this.paint.getTextBounds(this.text, 0, this.text.length(), rect);
		int x = getWidth() / 2 - rect.centerX();
		int y = getHeight() / 2 - rect.centerY();
		canvas.drawText(this.text, x, y, paint);
	}
}
