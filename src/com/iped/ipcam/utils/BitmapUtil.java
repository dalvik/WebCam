package com.iped.ipcam.utils;

import java.io.File;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Shader.TileMode;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;


public class BitmapUtil {
	
	// 重构图片大小
	public static Bitmap resizeBitmapSize(Bitmap src, int width, int height) {
		return Bitmap.createScaledBitmap(src, width, height, true);
	}
	
	
	public static Bitmap resizeApplicationIcon(Drawable src, int width, int height) {
		Bitmap desc = Bitmap.createBitmap(width, height, src.getOpacity() != PixelFormat.OPAQUE ? Bitmap.Config.ARGB_8888 : Bitmap.Config.RGB_565);
		Canvas canvas = new Canvas(desc);   
		src.setBounds(0,0,width,height);   
		src.draw(canvas);
		return desc;
	}
	
	public static BitmapDrawable createTriangleImage(String txt, int txtSize) {
		Bitmap mbmpTest = Bitmap.createBitmap(txt.length() * txtSize + 4,
		txtSize + 4, Config.ALPHA_8);
		Canvas canvasTemp = new Canvas(mbmpTest);
		Paint p = new Paint();
		p.setAntiAlias(true);
		p.setColor(Color.BLACK);
		p.setTextSize(txtSize);
		canvasTemp.drawText(txt, 2, txtSize - 2, p);
        BitmapDrawable bd = new BitmapDrawable(mbmpTest);
		return bd;
	}
	
	public static Bitmap createTxtImage(String txt, int txtSize) {
		Bitmap mbmpTest = Bitmap.createBitmap(txt.length() * txtSize + 8, txtSize*27/10, Config.ALPHA_8);
		Canvas canvasTemp = new Canvas(mbmpTest);
		Paint p = new Paint();
		p.setAntiAlias(true);
		p.setColor(Color.BLACK);
		p.setTextSize(txtSize*4/3);
		canvasTemp.drawText(txt, 0, txtSize +4, p);
		return mbmpTest;
	}
	
	public static Bitmap createBookImageWithName(String name, int txtSize) {
		int w = 150;//name.length() * txtSize + 8;
		int h = 60;//txtSize * 4;
		Bitmap mbmpTest = Bitmap.createBitmap(w, h, Config.ARGB_8888);
		Canvas canvasTemp = new Canvas(mbmpTest);
		Paint p = new Paint();
		p.setColor(Color.BLACK);
		p.setTextSize(txtSize);
		canvasTemp.drawText(name, 0, h/3, p);
		//canvasTemp.translate(0, 0);
		//canvasTemp.drawText(name, 0, txtSize +4, p);
		/*TextPaint textPaint = new TextPaint();
		textPaint.setTextSize(20);
		Layout layout = new StaticLayout(name, textPaint, w, Layout.Alignment.ALIGN_NORMAL, 1.0f, 1.0f, false);
		layout.draw(canvasTemp);*/
		return mbmpTest;
	}
	

	public static Bitmap createBottomLineImage() {
		Bitmap mbmpTest = Bitmap.createBitmap(30, 30, Config.ARGB_8888);
		Canvas canvasTemp = new Canvas(mbmpTest);
		Paint p = new Paint();
		p.setAntiAlias(true);
		p.setColor(Color.BLACK);
		//canvasTemp.drawCircle(18, 18, 5, p);
		p.setStrokeWidth(1);
		canvasTemp.drawLine(8, 25, 22, 25, p);
		return mbmpTest;
	}
	
	public static Bitmap createReflectedImage(Bitmap originalImage) {
		// The gap we want between the reflection and the original image
		final int reflectionGap = 0;
		int width = originalImage.getWidth();
		int height = originalImage.getHeight();
		// This will not scale but will flip on the Y axis
		Matrix matrix = new Matrix();
		
		matrix.preScale(1, -1);
		// Create a Bitmap with the flip matrix applied to it.
		// We only want the bottom half of the image
		Bitmap reflectionImage = Bitmap.createBitmap(originalImage, 0,

		height / 2, width, height / 2, matrix, false);
		// Create a new bitmap with same width but taller to fit reflection
		Bitmap bitmapWithReflection = Bitmap.createBitmap(width,
		(height + height / 2), Config.ARGB_8888);
		// Create a new Canvas with the bitmap that's big enough for
		// the image plus gap plus reflection
		Canvas canvas = new Canvas(bitmapWithReflection);
		// Draw in the original image
		canvas.drawBitmap(originalImage, 0, 0, null);
		// Draw in the gap
		Paint defaultPaint = new Paint();
		canvas.drawRect(0, height, width, height + reflectionGap, defaultPaint);
		// Draw in the reflection
		canvas.drawBitmap(reflectionImage, 0, height + reflectionGap, null);
		// Create a shader that is a linear gradient that covers the reflection
		Paint paint = new Paint();
		LinearGradient shader = new LinearGradient(0,
				originalImage.getHeight(), 0, bitmapWithReflection.getHeight()
						+ reflectionGap, 0x70ffffff, 0x00ffffff, TileMode.CLAMP);
		// Set the paint to use this shader (linear gradient)
		paint.setShader(shader);
		// Set the Transfer mode to be porter duff and destination in
		paint.setXfermode(new PorterDuffXfermode(Mode.DST_IN));
		// Draw a rectangle using the paint with our linear gradient
		canvas.drawRect(0, height, width, bitmapWithReflection.getHeight()
				+ reflectionGap, paint);
		return bitmapWithReflection;
	}

	public static Bitmap drawBookName(Bitmap src, String textPath) {
		int bitmapWidth =  src.getWidth();
		int x = 20;
		int y = 35;
		Canvas canvas = new Canvas(src);
		TextPaint textPaint = new TextPaint();
		textPaint.setTextSize(20);
		canvas.translate(x, y);
		Layout layout = new StaticLayout(textPath, textPaint, bitmapWidth-30, Layout.Alignment.ALIGN_CENTER, 1.3f, 1.0f, false);
		layout.draw(canvas);
		return src;
	}
	
	public static String getBooKName(String textPath) {
		if(textPath == null || "".equalsIgnoreCase(textPath) || textPath.startsWith(".")) {
			return "";
		}
		return textPath.substring(textPath.lastIndexOf(File.separator)+1);
	}
	
	


}
