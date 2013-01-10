package com.iped.ipcam.bitmapfun;

import java.util.List;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;

import com.iped.ipcam.gui.R;
import com.iped.ipcam.pojo.ImageInfo;

public class ImageGridAdapter extends BaseAdapter {

	private Context context;
	
	private int mNumColumns = 0;
	
	private int mItemHeight = 0;
	 
	private GridView.LayoutParams mImageViewLayoutParams;
	
	private ImageFetcher mImageFetcher;
	
	private List<ImageInfo> imageList;

	public ImageGridAdapter(Context context, ImageFetcher mImageFetcher, List<ImageInfo> imageList) {
		this.context = context;
		mImageViewLayoutParams = new GridView.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
		this.mImageFetcher = mImageFetcher;
		this.imageList = imageList;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		 final ImageInfo info = imageList.get(position);
        // Now handle the main ImageView thumbnails
        ImageView imageView;
        if (convertView == null) { // if it's not recycled, instantiate and initialize
            imageView = new ImageView(context);
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            imageView.setLayoutParams(mImageViewLayoutParams);
        } else { // Otherwise re-use the converted view
            imageView = (ImageView) convertView;
        }

        // Check the height matches our calculated column width
        if (imageView.getLayoutParams().height != mItemHeight) {
            imageView.setLayoutParams(mImageViewLayoutParams);
        }
        //imageView.setBackgroundResource(R.drawable.empty_photo);
        // Finally load the image asynchronously into the ImageView, this also takes care of
        // setting a placeholder image while the background thread runs
        mImageFetcher.loadImage(info.path, imageView);
        //mImageFetcher.processBitmap(info.path);
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
		return position < mNumColumns ? 0 : position - mNumColumns;
	}
	
	public void setNumColumns(int numColumns) {
            mNumColumns = numColumns;
    }

    public int getNumColumns() {
        return mNumColumns;
    }
    
    @Override
    public int getViewTypeCount() {
        // Two types of views, the normal ImageView and the top row of empty views
        return 1;
    }
    
    public void setItemHeight(int height) {
        if (height == mItemHeight) {
            return;
        }
        mItemHeight = height;
        mImageViewLayoutParams =
                new GridView.LayoutParams(LayoutParams.MATCH_PARENT, mItemHeight);
        mImageFetcher.setImageSize(height);
        notifyDataSetChanged();
    }
}
