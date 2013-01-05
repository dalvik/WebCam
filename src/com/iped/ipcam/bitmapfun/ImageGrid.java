/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.iped.ipcam.bitmapfun;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.ViewTreeObserver;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import com.iped.ipcam.bitmapfun.ImageCache.ImageCacheParams;
import com.iped.ipcam.gui.BuildConfig;
import com.iped.ipcam.gui.R;
import com.iped.ipcam.pojo.ImageInfo;
import com.iped.ipcam.utils.FileUtil;

/**
 * Simple FragmentActivity to hold the main {@link ImageGridFragment} and not much else.
 */
public class ImageGrid extends FragmentActivity implements OnItemClickListener {

	private static final String IMAGE_CACHE_DIR = "thumbs";
	 
	private static final String TAG = "ImageGridActivity";

	private ImageAdapter imageAdapter;
	
	private GridView gridView = null;
	
    private int mImageThumbSize;

    private int mImageThumbSpacing;
    
	private static List<ImageInfo> imageList = new ArrayList<ImageInfo>();
	
	private ImageFetcher mImageFetcher;
	
    @Override
	public void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
    	mImageThumbSize = getResources().getDimensionPixelSize(R.dimen.image_thumbnail_size);
    	mImageThumbSpacing = getResources().getDimensionPixelSize(R.dimen.image_thumbnail_spacing);
    	setContentView(R.layout.layout_all_image);
		gridView = (GridView) findViewById(R.id.all_image_gridview);
		ImageCacheParams cacheParams = new ImageCacheParams(this, IMAGE_CACHE_DIR);

	    // Set memory cache to 25% of mem class
		mImageFetcher = new ImageFetcher(this, mImageThumbSize);
		cacheParams.setMemCacheSizePercent(this, 0.25f);
	    mImageFetcher.setLoadingImage(R.drawable.empty_photo);
	    mImageFetcher.addImageCache(getSupportFragmentManager(), cacheParams);
		imageAdapter = new ImageAdapter(ImageGrid.this);
		gridView.setAdapter(imageAdapter);
		loadImageFiles();
		gridView.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        if (imageAdapter.getNumColumns() == 0) {
                            final int numColumns = (int) Math.floor(
                            		gridView.getWidth() / (mImageThumbSize + mImageThumbSpacing));
                            if (numColumns > 0) {
                                final int columnWidth =
                                        (gridView.getWidth() / numColumns) - mImageThumbSpacing;
                                imageAdapter.setNumColumns(numColumns);
                                imageAdapter.setItemHeight(columnWidth);
                                if (BuildConfig.DEBUG) {
                                    Log.d(TAG, "onCreateView - numColumns set to " + numColumns);
                                }
                            }
                        }
                    }
                });
		gridView.setOnScrollListener(new AbsListView.OnScrollListener() {
	            @Override
	            public void onScrollStateChanged(AbsListView absListView, int scrollState) {
	                // Pause fetcher to ensure smoother scrolling when flinging
	                if (scrollState == AbsListView.OnScrollListener.SCROLL_STATE_FLING) {
	                    mImageFetcher.setPauseWork(true);
	                } else {
	                    mImageFetcher.setPauseWork(false);
	                }
	            }

	            @Override
	            public void onScroll(AbsListView absListView, int firstVisibleItem,
	                    int visibleItemCount, int totalItemCount) {
	            }
	        });
		gridView.setOnItemClickListener(this);
    }
	
	@Override
    public void onItemClick(AdapterView<?> arg0, View v, int index, long id) {
    	final Intent i = new Intent(this, ImageDetailActivity.class);
        i.putExtra(ImageDetailActivity.EXTRA_IMAGE, index);
        i.putExtra(ImageDetailActivity.LIST_SIZE, imageList.size());
        if (Utils.hasJellyBean()) {
            // makeThumbnailScaleUpAnimation() looks kind of ugly here as the loading spinner may
            // show plus the thumbnail image in GridView is cropped. so using
            // makeScaleUpAnimation() instead.
            ActivityOptions options =
                    ActivityOptions.makeScaleUpAnimation(v, 0, 0, v.getWidth(), v.getHeight());
            startActivity(i, options.toBundle());
        } else {
            startActivity(i);
        }
    }
    
    @Override
    public void onResume() {
        super.onResume();
        mImageFetcher.setExitTasksEarly(false);
        imageAdapter.notifyDataSetChanged();
    }

    @Override
    public void onPause() {
        super.onPause();
        mImageFetcher.setExitTasksEarly(true);
        mImageFetcher.flushCache();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mImageFetcher.closeCache();
        imageList.clear();
    }
    
	private class ImageAdapter extends BaseAdapter {

		private Context context;
		
		private int mNumColumns = 0;
		
		private int mItemHeight = 0;
		 
		private int mActionBarHeight = 0;
		
		private GridView.LayoutParams mImageViewLayoutParams;

		public ImageAdapter(Context context) {
			this.context = context;
			mImageViewLayoutParams = new GridView.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
			TypedValue tv = new TypedValue();
            if (context.getTheme().resolveAttribute(
                    android.R.attr.actionBarSize, tv, true)) {
                mActionBarHeight = TypedValue.complexToDimensionPixelSize(
                        tv.data, context.getResources().getDisplayMetrics());
            }
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			 final ImageInfo info = imageList.get(position);
			/* if (position < mNumColumns) {
                if (convertView == null) {
                    convertView = new View(context);
                }
                // Set empty view with height of ActionBar
                convertView.setLayoutParams(new AbsListView.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, mActionBarHeight));
                return convertView;
            }*/

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
            imageView.setBackgroundResource(R.drawable.empty_photo);

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
	
	public static String getFilePath(int position) {
		return imageList.get(position).path;
	}
	
	private void loadImageFiles() {
		if (Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED)) {
			String sdDir = Environment.getExternalStorageDirectory().getAbsolutePath();// »ñÈ¡¸úÄ¿Â¼
			File file = new File(sdDir + FileUtil.parentPath + FileUtil.picForder);
			if(!file.exists()) {
				file.mkdirs();
				File noMedia = new File(sdDir + FileUtil.parentPath + FileUtil.picForder + ".nomedia");
				try {
					noMedia.createNewFile();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			File[] files = file.listFiles(new FileFilter() {
				
				@Override
				public boolean accept(File pathname) {
					if(pathname.isDirectory()) {
						return true;
					}
					String name = pathname.getName().toLowerCase();
					return name.endsWith(".jpeg") || name.endsWith(".jpg") || name.endsWith(".png") || name.endsWith(".gif");
				}
			});
            for(int i = 0; i < files.length; i++){
            	//paths[i] = files[i].getPath();
            	ImageInfo imageInfo = new ImageInfo();
            	imageInfo.path = files[i].getPath();
            	imageInfo.title = files[i].getName();
            	Uri uri = Uri.parse("file://"+files[i].getPath());
            	imageInfo.setActivity(uri, Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            	imageList.add(imageInfo);
            }
            imageAdapter.notifyDataSetChanged();
		}
	}
	static class ViewHolder {

		public ImageView icon;

		public TextView name;
	}
}
