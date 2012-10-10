package com.iped.ipcam.view;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.util.Log;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;

import com.iped.ipcam.gui.ImageViewer;
import com.iped.ipcam.pojo.ImageInfo;

public class ImageLoaderTask extends AsyncTask<ImageInfo, Void, Bitmap> {

	private ImageInfo imageInfo;
	
	private final WeakReference<ImageView> imageViewReference; // 防止内存溢出

	public ImageLoaderTask(ImageView imageView) {
		imageViewReference = new WeakReference<ImageView>(imageView);
	}

	@Override
	protected Bitmap doInBackground(ImageInfo... params) {
		imageInfo = params[0];
		return loadImageFile(imageInfo.path);
	}

	private Bitmap loadImageFile(final String filename) {
		InputStream is = null;
		try {
			Bitmap bmp = BitmapCache.getInstance().getBitmap(filename);
			return bmp;
		} catch (Exception e) {
			Log.e(this.getClass().getSimpleName(), "fetchDrawable failed", e);
		} finally {
			try {
				if (is != null)
					is.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return null;
	}

	@Override
	protected void onPostExecute(Bitmap bitmap) {
		if (isCancelled()) {
			bitmap = null;
		}
		if (imageViewReference != null) {
			ImageView imageView = imageViewReference.get();
			if (imageView != null) {
				if (bitmap != null) {
					int width = bitmap.getWidth(); //获取真实宽高
					int height = bitmap.getHeight();
					LayoutParams lp = imageView.getLayoutParams();
					lp.height = (height * ImageViewer.itemWidth) / width;//调整高度
					imageView.setLayoutParams(lp);
					imageView.setImageBitmap(bitmap);
				}
			}
		}
	}
}