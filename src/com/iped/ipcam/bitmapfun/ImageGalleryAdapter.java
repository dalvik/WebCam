package com.iped.ipcam.bitmapfun;

import java.util.List;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;

import com.iped.ipcam.pojo.ImageInfo;

public class ImageGalleryAdapter extends BaseAdapter {

	private Context context;
	
	private LinearLayout.LayoutParams mImageViewLayoutParams;
	
	private ImageFetcher mImageFetcher;
	
	private List<ImageInfo> imageList;

	public ImageGalleryAdapter(Context context, ImageFetcher mImageFetcher, List<ImageInfo> imageList) {
		this.context = context;
		mImageViewLayoutParams = new LinearLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);
		mImageViewLayoutParams.gravity= Gravity.CENTER;
		this.mImageFetcher = mImageFetcher;
		this.imageList = imageList;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		 final ImageInfo info = imageList.get(position);
		 System.out.println("postion = " + position +  " " + info.path);
        // Now handle the main ImageView thumbnails
        ImageView imageView;
        if (convertView == null) { // if it's not recycled, instantiate and initialize
            imageView = new ImageView(context);
            imageView.setScaleType(ScaleType.MATRIX);
            imageView.setLayoutParams(mImageViewLayoutParams);
        } else { // Otherwise re-use the converted view
            imageView = (ImageView) convertView;
        }
        mImageFetcher.loadImage(info.path, imageView);
        return imageView;
	}

	@Override
	public int getCount() {
		return imageList.size();
	}

	@Override
	public ImageInfo getItem(int position) {
		return imageList.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}
	
}
