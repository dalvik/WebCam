/*
 * Copyright (C) 2011 TC Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and limitations under the
 * License. This code is base on the Android TextView and was Created by titanchen2000@yahoo.com.cn
 * 
 * @author TC
 */
package com.iped.ipcam.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuffXfermode;
import android.graphics.PorterDuff.Mode;
import android.graphics.Shader.TileMode;
import android.util.AttributeSet;
import android.widget.TextView;

public class ReflectTextView extends TextView {

	public ReflectTextView(Context context) {
		super(context);

	}

	public ReflectTextView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);

	}

	public ReflectTextView(Context context, AttributeSet attrs) {
		super(context, attrs);

	}

	@Override
	protected void onDraw(Canvas canvas) {
		//draw the text from layout()
		super.onDraw(canvas);

		int height = getHeight();
		int width = getWidth();
		
		//make the shadow reverse of Y 
		Matrix matrix = new Matrix();
		matrix.preScale(1, -1);
		
		//make sure you can use the cache
		setDrawingCacheEnabled(true);
		//create bitmap from cache,this is the most important of this 
		Bitmap originalImage = Bitmap.createBitmap(getDrawingCache());
		//create the shadow
		Bitmap reflectionImage = Bitmap.createBitmap(originalImage, 0,
				height / 3, width, height / 3, matrix, false);
		//draw the shadow
		canvas.drawBitmap(reflectionImage, 0, 8 * height / 12, null);
		//process shadow bitmap to make it shadow like
		Paint paint = new Paint();
		LinearGradient shader = new LinearGradient(0, 8 * height / 12, 0,
				height, 0x70ffffff, 0x00ffffff, TileMode.CLAMP);		
		paint.setShader(shader);		
		paint.setXfermode(new PorterDuffXfermode(Mode.DST_IN));		
		canvas.drawRect(0, 8 * height / 12, width, height, paint);
	}

}
